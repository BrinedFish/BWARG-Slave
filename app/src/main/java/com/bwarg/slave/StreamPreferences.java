package com.bwarg.slave;

import com.google.gson.Gson;


/**
 * Created by LM on 10.02.2016.
 */
public class StreamPreferences {
    public final static String UNKNOWN_NAME = "(Unknown)";
    /*private int captureWidth = 640;
    private int captureHeight = 480;*/

    private int cameraPreviewIndex = 0;

    private int ip_port = 8080;
    private String name = UNKNOWN_NAME;
    private int camIndex =0;
    private boolean useFlashLight = false;
    private int quality = 40;

    public StreamPreferences(){

    }

    /*public int getCaptureWidth() {
        return captureWidth;
    }

    public int getCaptureHeight() {
        return captureHeight;
    }*/

    public int getIp_port() {
        return ip_port;
    }

   /* public void setCaptureWidth(int captureWidth) {
        this.captureWidth = captureWidth;
    }

    public void setCaptureHeight(int captureHeight) {
        this.captureHeight = captureHeight;
    }*/

    public void setIp_port(int ip_port) {
        this.ip_port = ip_port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCamIndex() {
        return camIndex;
    }

    public void setCamIndex(int camIndex) {
        this.camIndex = camIndex;
    }

    public boolean useFlashLight() {
        return useFlashLight;
    }

    public void setUseFlashLight(boolean useFlashLight) {
        this.useFlashLight = useFlashLight;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getCameraPreviewIndex() {
        return cameraPreviewIndex;
    }

    public void setCameraPreviewIndex(int cameraPreviewIndex) {
        this.cameraPreviewIndex = cameraPreviewIndex;
    }

    public static String defaultGsonString(){
        Gson gson = new Gson();
        return gson.toJson(new StreamPreferences());
    }

}
