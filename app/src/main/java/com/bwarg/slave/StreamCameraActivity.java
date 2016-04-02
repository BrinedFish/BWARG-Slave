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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.apache.http.conn.util.InetAddressUtils;

import eu.hgross.blaubot.android.BlaubotAndroid;
import eu.hgross.blaubot.android.BlaubotAndroidFactory;
import eu.hgross.blaubot.core.IBlaubotDevice;
import eu.hgross.blaubot.core.ILifecycleListener;
import eu.hgross.blaubot.messaging.BlaubotMessage;
import eu.hgross.blaubot.messaging.IBlaubotChannel;
import eu.hgross.blaubot.messaging.IBlaubotMessageListener;

public final class StreamCameraActivity extends Activity
        implements SurfaceHolder.Callback
{
    private static final String TAG = StreamCameraActivity.class.getSimpleName();
    private static final String TAG_BLAUBOT = TAG+"-BLAUBOT";
    private static final int REQUEST_SETTINGS = 1;
    private static final String WAKE_LOCK_TAG = "bwarg-slave";

    private static final int DISCOVER_PORT_DEF = 8888;

    private boolean mRunning = false;
    private boolean mPreviewDisplayCreated = false;
    private SurfaceHolder mPreviewDisplay = null;
    private CameraStreamer mCameraStreamer = null;
    private ImageButton exposure_lock_button;

    private String mIpAddress = "";

    private SlaveStreamPreferences streamPrefs = new SlaveStreamPreferences();

    private TextView mIpAddressView = null;
    private WakeLock mWakeLock = null;

    //Networking attributes
    private static final String APP_UUID_STRING = "52260110-f8f0-11e5-a837-0800200c9a66";
    private static final int APP_PORT = 5606;
    private BlaubotAndroid blaubot;

    //Lock physical keys attributes
    public static boolean LOCK_PHYS_KEYS = false;
    HomeKeyLocker homeKeyLocker = new HomeKeyLocker();

    //private NetworkServerTask netSTask;

    public StreamCameraActivity()
    {
        super();
    } // constructor()

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPreviewDisplay = ((SurfaceView) findViewById(R.id.camera)).getHolder();
        mPreviewDisplay.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreviewDisplay.addCallback(this);

        mIpAddress = tryGetIpV4Address();
        mIpAddressView = (TextView) findViewById(R.id.ip_address);

        SharedPreferences sharedPrefs = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        streamPrefs = loadPreferences(sharedPrefs);
        updatePrefCacheAndUi(streamPrefs);

        exposure_lock_button = (ImageButton) findViewById(R.id.auto_exposure_lock_button);

        final PowerManager powerManager =
                (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                WAKE_LOCK_TAG);
        final UUID APP_UUID = UUID.fromString(APP_UUID_STRING);
        blaubot = BlaubotAndroidFactory.createEthernetBlaubotWithBluetoothBeacon(APP_UUID, APP_PORT, tryGetIpV4InetAddress());
        blaubot.startBlaubot();
        blaubot.registerReceivers(this);
        blaubot.setContext(this);
        //blaubot.onResume(this);
        final IBlaubotChannel channel = blaubot.createChannel((short)1);
        channel.subscribe(new IBlaubotMessageListener() {
            @Override
            public void onMessage(BlaubotMessage message) {
                // we got a message - our payload is a byte array
                // deserialize
                String msg = new String(message.getPayload());
                //Structure of messages : HEADER_[fromID]XXX_[toID]XXX_DATA
                String header = msg.substring(0,2);
                int fromIDIndex = msg.indexOf("_[fromID]")+9;
                int toIDIndex = msg.indexOf("_[toID]")+7;
                int dataIndex = msg.indexOf("_[data]")+7;

                String fromID = msg.substring(fromIDIndex, toIDIndex-7);
                String toID = msg.substring(toIDIndex, dataIndex-7);
                String data = msg.substring(dataIndex);

                switch(header){
                    case "SSP" : //manage complete settings rewrite here
                        if(toID.equals(blaubot.getOwnDevice().getUniqueDeviceID())) {
                            SlaveStreamPreferences prefs = SlaveStreamPreferences.fromGson(data);
                            updatePrefCacheAndUi(prefs);
                            tryStartCameraStreamer();
                        }
                        break;
                    case "AEL" :
                        if(toID.equals(blaubot.getOwnDevice().getUniqueDeviceID())) {
                            boolean lock = Boolean.parseBoolean(msg.substring(2));
                            Camera.Parameters params = mCameraStreamer.getCamera().getParameters();
                            params.setAutoExposureLock(lock);
                            mCameraStreamer.getCamera().setParameters(params);
                        }
                        break;
                    default : break;
                }
                Log.i(TAG_BLAUBOT, "Received from channel : " + msg);
            }
        });
        blaubot.addLifecycleListener(new ILifecycleListener() {
            @Override
            public void onDisconnected() {
                // THIS device disconnected from the network
                Log.i(TAG_BLAUBOT, "Disconnected.");
            }

            @Override
            public void onDeviceLeft(IBlaubotDevice blaubotDevice) {
                // ANOTHER device disconnected from the network
            }

            @Override
            public void onDeviceJoined(IBlaubotDevice blaubotDevice) {
                // ANOTHER device connected to the network THIS device is on
                channel.publish(("SSP" + streamPrefs.toGson()).getBytes());
            }

            @Override
            public void onConnected() {
                // THIS device connected to a network
                // you can now subscribe to channels and use them:
                channel.subscribe();
                channel.publish(("SSP"+streamPrefs.toGson()).getBytes());
                // onDeviceJoined(...) calls will follow for each OTHER device that was already connected
            }

            @Override
            public void onPrinceDeviceChanged(IBlaubotDevice oldPrince, IBlaubotDevice newPrince) {
                // if the network's king goes down, the prince will rule over the remaining peasants
            }

            @Override
            public void onKingDeviceChanged(IBlaubotDevice oldKing, IBlaubotDevice newKing) {

            }
        });
    } // onCreate(Bundle)

    @Override
    protected void onResume()
    {
        super.onResume();
        mRunning = true;

        updatePrefCacheAndUi(streamPrefs);
        tryStartCameraStreamer();
        mWakeLock.acquire();

        blaubot.startBlaubot();
        blaubot.registerReceivers(this);
        blaubot.setContext(this);
        //blaubot.onResume(this);
    } // onResume()

    @Override
    protected void onPause()
    {
        mWakeLock.release();
        super.onPause();
        mRunning = false;

        ensureCameraStreamerStopped();

        super.onPause();
        unlockPhysKeys();

        blaubot.unregisterReceivers(this);
        blaubot.onPause(this);
    } // onPause()

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format,
                               final int width, final int height)
    {
        // Ingored
    } // surfaceChanged(SurfaceHolder, int, int, int)

    @Override
    public void surfaceCreated(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = true;
        tryStartCameraStreamer();
    } // surfaceCreated(SurfaceHolder)

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = false;
        ensureCameraStreamerStopped();
    } // surfaceDestroyed(SurfaceHolder)

    private void tryStartCameraStreamer()
    {
        if (mRunning && mPreviewDisplayCreated/* && mPrefs != null*/)
        {
            mCameraStreamer = new CameraStreamer(streamPrefs, mPreviewDisplay);
            mCameraStreamer.start();
        } // if
    } // tryStartCameraStreamer()

    private void ensureCameraStreamerStopped() {
        if (mCameraStreamer != null) {
            mCameraStreamer.stop();
            mCameraStreamer = null;
        } // if
    } // stopCameraStreamer()

    public void openSettings(View v){
        Intent intent = new Intent(this, SlaveSettingsActivity.class);

        Log.d(TAG, "GSON string given to settings : " + streamPrefs.toGson());
        intent.putExtra("stream_prefs", streamPrefs.toGson());

        startActivityForResult(intent, REQUEST_SETTINGS);
    }

    public void toggleExposureLock(View v) {
        Camera cam = mCameraStreamer.getCamera();
        Camera.Parameters params = cam.getParameters();
        boolean exposureLocked = params.getAutoExposureLock();

        exposure_lock_button.setImageResource(exposureLocked ? R.drawable.exposure_unlocked : R.drawable.exposure_locked);

        params.setAutoExposureLock(!exposureLocked);
        cam.setParameters(params);
    }

    private final void updatePrefCacheAndUi(SlaveStreamPreferences streamPrefs)
    {
        this.streamPrefs = streamPrefs;
        streamPrefs.setIpAdress(tryGetIpV4Address());
        mIpAddressView.setText("http://" + mIpAddress + ":" + streamPrefs.getIpPort() + "/");

        if(LOCK_PHYS_KEYS){
            lockPhysKeys();
        }else{
            unlockPhysKeys();
        }
    } // updatePrefCacheAndUi()

    private static String tryGetIpV4Address()
    {
        try
        {
            final Enumeration<NetworkInterface> en =
                    NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements())
            {
                final NetworkInterface intf = en.nextElement();
                final Enumeration<InetAddress> enumIpAddr =
                        intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements())
                {
                    final  InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        final String addr = inetAddress.getHostAddress().toUpperCase();
                        if (InetAddressUtils.isIPv4Address(addr))
                        {
                            return addr;
                        }
                    } // if
                } // while
            } // for
        } // try
        catch (final Exception e)
        {
            // Ignore
        } // catch
        return null;
    } // tryGetIpV4Address()

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SETTINGS:
                if (resultCode == Activity.RESULT_OK) {
                    //left cam
                    streamPrefs = SlaveStreamPreferences.fromGson(data.getStringExtra("stream_prefs"));
                    streamPrefs.setIpAdress(tryGetIpV4Address());

                    SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    savePreferences(editor, streamPrefs);
                    editor.commit();

                    updatePrefCacheAndUi(streamPrefs);
                    tryStartCameraStreamer();

                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        blaubot.stopBlaubot();
        SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        savePreferences(editor, streamPrefs);
        editor.commit();
        unlockPhysKeys();
        homeKeyLocker=null;
        super.onDestroy();
    }

    public String getIP(){
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();


        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));

    }

    private SlaveStreamPreferences loadPreferences(SharedPreferences prefs) {
        SlaveStreamPreferences temp = SlaveStreamPreferences.fromGson(prefs.getString("stream_prefs", SlaveStreamPreferences.defaultGsonString()));
        temp.setIpAdress(tryGetIpV4Address());
        LOCK_PHYS_KEYS = prefs.getBoolean("lock_phys_keys", false);
        Log.d("MJPEG_Cam", "StreamPrefs" + prefs.getString("stream_prefs", SlaveStreamPreferences.defaultGsonString()) + " loaded at startup.");
        return temp;
    }
    private void savePreferences(SharedPreferences.Editor editor, SlaveStreamPreferences streamPrefs){
        editor.putString("stream_prefs", streamPrefs.toGson());
        editor.putBoolean("lock_phys_keys", LOCK_PHYS_KEYS);
    }
    private void lockPhysKeys(){
        homeKeyLocker.lock(this);
    }
    private void unlockPhysKeys(){
        homeKeyLocker.unlock();
    }
    private static InetAddress tryGetIpV4InetAddress()
    {
        try
        {
            final Enumeration<NetworkInterface> en =
                    NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements())
            {
                final NetworkInterface intf = en.nextElement();
                final Enumeration<InetAddress> enumIpAddr =
                        intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements())
                {
                    final  InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        final String addr = inetAddress.getHostAddress().toUpperCase();
                        if (InetAddressUtils.isIPv4Address(addr))
                        {
                            return inetAddress;
                        }
                    } // if
                } // while
            } // for
        } // try
        catch (final Exception e)
        {
            // Ignore
        } // catch
        return null;
    } // tryGetIpV4Address()
} // class StreamCameraActivity

