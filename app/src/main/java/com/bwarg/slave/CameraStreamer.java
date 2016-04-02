/* Copyright 2013 Foxdog Studios Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bwarg.slave;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

/* package */ final class CameraStreamer extends Object
{
    private static final String TAG = CameraStreamer.class.getSimpleName();
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int MESSAGE_TRY_START_STREAMING = 0;
    private static final int MESSAGE_SEND_PREVIEW_FRAME = 1;

    private static final long OPEN_CAMERA_POLL_INTERVAL_MS = 1000L;

    private final Object mLock = new Object();
    private final MovingAverage mAverageSpf = new MovingAverage(50 /* numValues */);

    /*private final int mCameraIndex;
    private final boolean mUseFlashLight;
    private final int mPort;
    private final int mPreviewSizeIndex;
    private final int mJpegQuality;*/
    private SlaveStreamPreferences streamPrefs = new SlaveStreamPreferences();
    private final SurfaceHolder mPreviewDisplay;

    private boolean mRunning = false;
    private Looper mLooper = null;
    private Handler mWorkHandler = null;
    private Camera mCamera = null;
    private int mPreviewFormat = Integer.MIN_VALUE;
    private int mPreviewWidth = Integer.MIN_VALUE;
    private int mPreviewHeight = Integer.MIN_VALUE;
    private Rect mPreviewRect = null;
    private int mPreviewBufferSize = Integer.MIN_VALUE;
    private MemoryOutputStream mJpegOutputStream = null;
    private MJpegHttpStreamer mMJpegHttpStreamer = null;

    private long mNumFrames = 0L;
    private long mLastTimestamp = Long.MIN_VALUE;

    /* package */ CameraStreamer(final SlaveStreamPreferences streamPrefs, final SurfaceHolder previewDisplay)
    {
        super();

        if (previewDisplay == null)
        {
            throw new IllegalArgumentException("previewDisplay must not be null");
        } // if

        /*mCameraIndex = cameraIndex;
        mUseFlashLight = useFlashLight;
        mPort = port;
        mPreviewSizeIndex = previewSizeIndex;
        mJpegQuality = jpegQuality;*/
        this.streamPrefs = streamPrefs;
        mPreviewDisplay = previewDisplay;
    } // constructor(SurfaceHolder)

    private final class WorkHandler extends Handler
    {
        private WorkHandler(final Looper looper)
        {
            super(looper);
        } // constructor(Looper)

        @Override
        public void handleMessage(final Message message)
        {
            switch (message.what)
            {
                case MESSAGE_TRY_START_STREAMING:
                    tryStartStreaming();
                    break;
                case MESSAGE_SEND_PREVIEW_FRAME:
                    final Object[] args = (Object[]) message.obj;
                    sendPreviewFrame((byte[]) args[0], (Camera) args[1], (Long) args[2]);
                    break;
                default:
                    throw new IllegalArgumentException("cannot handle message");
            } // switch
        } // handleMessage(Message)
    } // class WorkHandler

    /* package */ void start()
    {
        synchronized (mLock)
        {
            if (mRunning)
            {
                throw new IllegalStateException("CameraStreamer is already running");
            } // if
            mRunning = true;
        } // synchronized

        final HandlerThread worker = new HandlerThread(TAG, Process.THREAD_PRIORITY_MORE_FAVORABLE);
        worker.setDaemon(true);
        worker.start();
        mLooper = worker.getLooper();
        mWorkHandler = new WorkHandler(mLooper);
        mWorkHandler.obtainMessage(MESSAGE_TRY_START_STREAMING).sendToTarget();
    } // start()

    /**
     *  Stop the image streamer. The camera will be released during the
     *  execution of stop() or shortly after it returns. stop() should
     *  be called on the main thread.
     */
    /* package */ void stop()
    {
        synchronized (mLock)
        {
            if (!mRunning)
            {
                throw new IllegalStateException("CameraStreamer is already stopped");
            } // if

            mRunning = false;
            if (mMJpegHttpStreamer != null)
            {
                mMJpegHttpStreamer.stop();
            } // if
            if (mCamera != null)
            {
                mCamera.release();
                mCamera = null;
            } // if
        } // synchronized
        mLooper.quit();
    } // stop()

    private void tryStartStreaming()
    {
        try
        {
            while (true)
            {
                try
                {
                    startStreamingIfRunning();
                } //try
                catch (final RuntimeException openCameraFailed)
                {
                    Log.d(TAG, "Open camera failed, retying in " + OPEN_CAMERA_POLL_INTERVAL_MS
                            + "ms", openCameraFailed);
                    Thread.sleep(OPEN_CAMERA_POLL_INTERVAL_MS);
                    continue;
                } // catch
               break;
            } // while
        } // try
        catch (final Exception startPreviewFailed)
        {
            // Captures the IOException from startStreamingIfRunning and
            // the InterruptException from Thread.sleep.
            Log.w(TAG, "Failed to start camera preview", startPreviewFailed);
        } // catch
    } // tryStartStreaming()

    private void startStreamingIfRunning() throws IOException
    {
        // Throws RuntimeException if the camera is currently opened
        // by another application.
        final Camera camera = Camera.open(streamPrefs.getCamIndex());
        final Camera.Parameters params = camera.getParameters();

        applyImageSettings(params, camera);
        // Set up preview callback
        mPreviewFormat = params.getPreviewFormat();
        final Camera.Size previewSize = params.getPreviewSize();
        mPreviewWidth = previewSize.width;
        mPreviewHeight = previewSize.height;
        final int BITS_PER_BYTE = 8;
        final int bytesPerPixel = ImageFormat.getBitsPerPixel(mPreviewFormat) / BITS_PER_BYTE;
        // XXX: According to the documentation the buffer size can be
        // calculated by width * height * bytesPerPixel. However, this
        // returned an error saying it was too small. It always needed
        // to be exactly 1.5 times larger.
        mPreviewBufferSize = mPreviewWidth * mPreviewHeight * bytesPerPixel * 3 / 2 + 1;
        camera.addCallbackBuffer(new byte[mPreviewBufferSize]);
        mPreviewRect = new Rect(0, 0, mPreviewWidth, mPreviewHeight);
        camera.setPreviewCallbackWithBuffer(mPreviewCallback);

        // We assumed that the compressed image will be no bigger than
        // the uncompressed image.
        mJpegOutputStream = new MemoryOutputStream(mPreviewBufferSize);

        final MJpegHttpStreamer streamer = new MJpegHttpStreamer(streamPrefs.getIpPort(), mPreviewBufferSize);
        streamer.start();

        synchronized (mLock)
        {
            if (!mRunning)
            {
                streamer.stop();
                camera.release();
                return;
            } // if

            try
            {
                camera.setPreviewDisplay(mPreviewDisplay);
            } // try
            catch (final IOException e)
            {
                streamer.stop();
                camera.release();
                throw e;
            } // catch

            mMJpegHttpStreamer = streamer;

            camera.startPreview();
            mCamera = camera;
        } // synchronized
    } // startStreamingIfRunning()
    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }
    /*private Rect calculateTapArea(float x, float y, float coefficient) {
        int orientation = 0;
        Camera.CameraInfo info = new Camera.CameraInfo();
        //getCameraInfo(0, info);

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            orientation = (info.orientation + degrees) % 360;
            orientation = (360 - orientation) % 360;  // compensate the mirror
        } else {  // back-facing
            orientation = (info.orientation - degrees + 360) % 360;
        }
        Matrix matrix = new Matrix();
        Matrix matrix2 = new Matrix();
        matrix2.postRotate(orientation);
        matrix2.postScale(WIDTH / 2000f, HEIGHT / 2000f);
        matrix2.postTranslate(WIDTH / 2f, HEIGHT / 2f);
        matrix2.invert(matrix);

        double focusAreaSize = getResources().getDimensionPixelSize(R.dimen.camera_focus_area_size);
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

        int left = clamp((int) x - areaSize / 2, 0, WIDTH - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, HEIGHT.getHeight() - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        matrix.mapRect(rectF);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }*/
private void applyImageSettings(Camera.Parameters params, Camera camera){
    final List<Camera.Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
    final Camera.Size selectedPreviewSize = supportedPreviewSizes.get(streamPrefs.getSizeIndex());
    params.setPreviewSize(selectedPreviewSize.width, selectedPreviewSize.height);
    ArrayList<String> stringSupportedPreviewSizeList = new ArrayList<>();
    for(Camera.Size s : supportedPreviewSizes) {
        stringSupportedPreviewSizeList.add(s.width + "x" + s.height);
    }
    streamPrefs.setResolutionsSupported(stringSupportedPreviewSizeList);

    List<Camera.Area> camera_areas = new ArrayList<Camera.Area>();
   // Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);
   // Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f);

    camera_areas.add(new Camera.Area(new Rect(0, 0, 0, 0), 0));
    params.setFocusAreas(camera_areas);

    if (streamPrefs.useFlashLight())
    {
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
    } // if
    params.setWhiteBalance(streamPrefs.getWhiteBalance());
    streamPrefs.setAutoWhiteBalanceLockSupported(params.isAutoWhiteBalanceLockSupported());
    if(params.isAutoWhiteBalanceLockSupported()){
        params.setAutoWhiteBalanceLock(streamPrefs.getAutoWhiteBalanceLock());
    }
    String temp = params.get("iso");
    if(temp!=null)
        params.set("iso", streamPrefs.getIso());

    temp = params.get("iso.speed");
    if(temp!=null)
        params.set("iso-speed", streamPrefs.getIso());

    temp = params.get("fast-fps-mode");
    streamPrefs.setFastFpsModeSupported(temp!=null);
    if(temp!=null) {
        params.set("fast-fps-mode", streamPrefs.getFastFpsMode());
    }
    params.setFocusMode(streamPrefs.getFocusMode());

    streamPrefs.setImageStabilizationSupported(params.isVideoStabilizationSupported());
    if(params.isVideoStabilizationSupported()){
        params.setVideoStabilization(streamPrefs.getImageStabilization());
    }

    streamPrefs.setAutoExposureLockSupported(params.isAutoExposureLockSupported() && !(Build.MANUFACTURER+Build.MODEL).equals("samsungGT-I9300"));
    if(params.isAutoExposureLockSupported()){
        if(streamPrefs.getAutoExposureLock()) {
            camera.cancelAutoFocus();
        }
        params.setAutoExposureLock(streamPrefs.getAutoExposureLock());
    }else{
        Log.d(TAG, "Camera parameters : auto-exposure-lock not supported.");
    }
    params.set("min-exposure-compensation",0);
    params.set("max-exposure-compensation",0);

    //params.setColorEffect(Camera.Parameters.Effe);

    // Set Preview FPS range. The range with the greatest maximum
    // is returned first.
    final List<int[]> supportedPreviewFpsRanges = params.getSupportedPreviewFpsRange();
    // XXX: However sometimes it returns null. This is a known bug
    // https://code.google.com/p/android/issues/detail?id=6271
    // In which case, we just don't set it.
    if (supportedPreviewFpsRanges != null)
    {
        /*final int[] range = supportedPreviewFpsRanges.get(0);
        params.setPreviewFpsRange(range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);*/
        params.set("preview-frame-rate", 30);
        camera.setParameters(params);
        //Log.i(TAG, "Camera parameters applied: " + params.flatten().replaceAll(";", ";\n"));
        Log.i(TAG, "Camera parameters applied: " + camera.getParameters().flatten().replaceAll(";", ";\n"));

    } // if

}
    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback()
    {
        @Override
        public void onPreviewFrame(final byte[] data, final Camera camera)
        {
            final Long timestamp = SystemClock.elapsedRealtime();
            final Message message = mWorkHandler.obtainMessage();
            message.what = MESSAGE_SEND_PREVIEW_FRAME;
            ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 262144);
            buffer.put(data);
            buffer.position(0);

            message.obj = new Object[]{ data, camera, timestamp };
            message.sendToTarget();
            /*Camera.Parameters parameters = camera.getParameters();
            parameters.setAutoExposureLock(true);
            camera.setParameters(parameters);*/
        } // onPreviewFrame(byte[], Camera)
    }; // mPreviewCallback

   private void sendPreviewFrame(final byte[] data, final Camera camera, final long timestamp)
   {
        // Calcalute the timestamp
        final long MILLI_PER_SECOND = 1000L;
        final long timestampSeconds = timestamp / MILLI_PER_SECOND;

        // Update and log the frame rate
        final long LOGS_PER_FRAME = 10L;
        mNumFrames++;
        if (mLastTimestamp != Long.MIN_VALUE)
        {
            mAverageSpf.update(timestampSeconds - mLastTimestamp);
            if (mNumFrames % LOGS_PER_FRAME == LOGS_PER_FRAME - 1)
            {
                Log.d(TAG, "FPS: " + 1.0 / mAverageSpf.getAverage());
            } // if
        } // else

        mLastTimestamp = timestampSeconds;

        // Create JPEG
        final YuvImage image = new YuvImage(data, mPreviewFormat, mPreviewWidth, mPreviewHeight,
                null /* strides */);
        image.compressToJpeg(mPreviewRect, streamPrefs.getQuality(), mJpegOutputStream);

        mMJpegHttpStreamer.streamJpeg(mJpegOutputStream.getBuffer(), mJpegOutputStream.getLength(),
                timestamp);

        // Clean up
        mJpegOutputStream.seek(0);
        // XXX: I believe that this is thread-safe because we're not
        // calling methods in other threads. I might be wrong, the
        // documentation is not clear.
        camera.addCallbackBuffer(data);
   } // sendPreviewFrame(byte[], camera, long)

    public Camera getCamera(){
        return mCamera;
    }
} // class CameraStreamer

