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
import java.net.UnknownHostException;
import java.util.Enumeration;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import org.apache.http.conn.util.InetAddressUtils;

public final class StreamCameraActivity extends Activity
        implements SurfaceHolder.Callback
{
    private static final String TAG = StreamCameraActivity.class.getSimpleName();
    private static final int REQUEST_SETTINGS = 1;
    private static final String WAKE_LOCK_TAG = "peepers";

    private static final int DISCOVER_PORT_DEF = 8888;

    private boolean mRunning = false;
    private boolean mPreviewDisplayCreated = false;
    private SurfaceHolder mPreviewDisplay = null;
    private CameraStreamer mCameraStreamer = null;

    private String mIpAddress = "";

    private StreamPreferences streamPrefs = new StreamPreferences();

    private TextView mIpAddressView = null;
    private WakeLock mWakeLock = null;

    //Networking attributes
    private static String SERVICE_NAME = "BWARG";
    private static String SERVICE_TYPE = "_http._tcp.";
    private NsdManager mNsdManager;
    public static boolean USE_NSD_MANAGER = false;
    private NsdManager.RegistrationListener mRegistrationListener;

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

        final PowerManager powerManager =
                (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                WAKE_LOCK_TAG);

        if(USE_NSD_MANAGER){
            mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
            registerService(DISCOVER_PORT_DEF);
        }
        /*if(netSTask !=null)
            netSTask.closeNetworkService();
        netSTask = new NetworkServerTask();
        netSTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");*/

    } // onCreate(Bundle)

    @Override
    protected void onResume()
    {
        super.onResume();
        mRunning = true;

        updatePrefCacheAndUi(streamPrefs);
        tryStartCameraStreamer();
        mWakeLock.acquire();
        if (mNsdManager != null) {
            registerService(DISCOVER_PORT_DEF);
        }
        /*if(netSTask !=null)
            netSTask.closeNetworkService();*/
    } // onResume()

    @Override
    protected void onPause()
    {
        mWakeLock.release();
        super.onPause();
        mRunning = false;

        ensureCameraStreamerStopped();
        if (mNsdManager != null) {
            mNsdManager.unregisterService(mRegistrationListener);
        }
        /*if(netSTask !=null)
            netSTask.closeNetworkService();*/
        super.onPause();
        unlockPhysKeys();
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

    private void ensureCameraStreamerStopped()
    {
        if (mCameraStreamer != null)
        {
            mCameraStreamer.stop();
            mCameraStreamer = null;
        } // if
    } // stopCameraStreamer()

    public void openSettings(View v){
        Intent intent = new Intent(this, SlaveSettingsActivity.class);
        Gson gson = new Gson();
        String gsonString =  gson.toJson(streamPrefs);
        Log.d(TAG, "GSON string given to settings : "+gsonString);
        intent.putExtra("stream_prefs", gsonString);

        startActivityForResult(intent, REQUEST_SETTINGS);
    }

    private final void updatePrefCacheAndUi(StreamPreferences streamPrefs)
    {
        this.streamPrefs = streamPrefs;
        mIpAddressView.setText("http://" + mIpAddress + ":" + streamPrefs.getIp_port() + "/");
        if(mNsdManager!=null && mRegistrationListener!=null){
            try{
                mNsdManager.unregisterService(mRegistrationListener);
                mRegistrationListener=null;
            }catch (IllegalArgumentException iae){
                iae.printStackTrace();
                mRegistrationListener= null;
                registerService(DISCOVER_PORT_DEF);
            }

        }
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
                    Gson gson = new Gson();
                    streamPrefs = gson.fromJson(data.getStringExtra("stream_prefs"), StreamPreferences.class);

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
        if (mNsdManager != null) {
            mNsdManager.unregisterService(mRegistrationListener);
            mRegistrationListener = null;
        }
        SharedPreferences preferences = getSharedPreferences("SAVED_VALUES", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        savePreferences(editor, streamPrefs);
        editor.commit();
        unlockPhysKeys();
        homeKeyLocker=null;
        super.onDestroy();
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(SERVICE_NAME + streamPrefs.getName());
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(streamPrefs.getIp_port());
        try {
            serviceInfo.setHost(InetAddress.getByName(getIP()));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                String mServiceName = nsdServiceInfo.getServiceName();
                SERVICE_NAME = mServiceName;
                Log.d(TAG, "Registered name : " + mServiceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo,
                                             int errorCode) {
                // Registration failed! Put debugging code here to determine
                // why.
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                // Service has been unregistered. This only happens when you
                // call
                // NsdManager.unregisterService() and pass in this listener.
                Log.d(TAG,
                        "Service Unregistered : " + serviceInfo.getServiceName());
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo,
                                               int errorCode) {
                // Unregistration failed. Put debugging code here to determine
                // why.
            }
        };
        mNsdManager.registerService(serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                mRegistrationListener);
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
    private StreamPreferences loadPreferences(SharedPreferences prefs){
        Gson gson = new Gson();
        StreamPreferences temp = gson.fromJson(prefs.getString("stream_prefs", StreamPreferences.defaultGsonString()), StreamPreferences.class);
        LOCK_PHYS_KEYS = prefs.getBoolean("lock_phys_keys", false);
        Log.d("MJPEG_Cam", "StreamPrefs" + prefs.getString("stream_prefs", StreamPreferences.defaultGsonString()) + " loaded at startup.");
        return temp;
    }
    private void savePreferences(SharedPreferences.Editor editor, StreamPreferences streamPrefs){
        Gson gson = new Gson();
        editor.putString("stream_prefs", gson.toJson(streamPrefs));
        editor.putBoolean("lock_phys_keys", LOCK_PHYS_KEYS);
    }
   /* protected void setLockPhysKeys(boolean lock){
        this.LOCK_PHYS_KEYS = lock;
        if(lock){
            lockPhysKeys();
        }else{
            unlockPhysKeys();
        }
    }
    protected boolean isLockingPhysKeys(){
        return LOCK_PHYS_KEYS;
    }*/
    private void lockPhysKeys(){
        homeKeyLocker.lock(this);
    }
    private void unlockPhysKeys(){
        homeKeyLocker.unlock();
    }
} // class StreamCameraActivity

