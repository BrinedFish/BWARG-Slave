package com.bwarg.slave;

import android.hardware.Camera;

import com.google.gson.Gson;


/**
 * Created by LM on 10.02.2016.
 */
public class StreamPreferences {
    public final static String UNKNOWN_NAME = "(Unknown)";
    /*private int captureWidth = 640;
    private int captureHeight = 480;*/

    private int ip_port = 8080;
    private String name = UNKNOWN_NAME;
    private int camIndex =0;
    private int sizeIndex = 0;
    private boolean useFlashLight = false;
    private int quality = 40;

    private boolean auto_exposure_lock = false;
    private boolean auto_white_balance_lock = false;
    private String whitebalance = "auto";
    private String focus_mode = "auto";
    private boolean image_stabilization = false;
    private String iso = "auto";
    private String iso_speed = "";
    private int fast_fps_mode = 0;

    public int getIp_port() {
        return ip_port;
    }

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

    public boolean isUseFlashLight() {
        return useFlashLight;
    }

    public boolean isAuto_exposure_lock() {
        return auto_exposure_lock;
    }

    public void setAuto_exposure_lock(boolean auto_exposure_lock) {
        this.auto_exposure_lock = auto_exposure_lock;
    }

    public boolean isAuto_white_balance_lock() {
        return auto_white_balance_lock;
    }

    public void setAuto_white_balance_lock(boolean auto_white_balance_lock) {
        this.auto_white_balance_lock = auto_white_balance_lock;
    }

    public String getFocus_mode() {
        return focus_mode;
    }

    public void setFocus_mode(String focus_mode) {
        this.focus_mode = focus_mode;
    }

    public boolean isImage_stabilization() {
        return image_stabilization;
    }

    public void setImage_stabilization(boolean image_stabilization) {
        this.image_stabilization = image_stabilization;
    }

    public String getIso() {
        return iso;
    }

    public void setIso(String iso) {
        this.iso = iso;
    }

    public String getWhitebalance() {
        return whitebalance;
    }

    public void setWhitebalance(String whitebalance) {
        this.whitebalance = whitebalance;
    }

    public int getSizeIndex() {
        return sizeIndex;
    }

    public void setSizeIndex(int sizeIndex) {
        this.sizeIndex = sizeIndex;
    }

    public int getFast_fps_mode() {
        return fast_fps_mode;
    }

    public void setFast_fps_mode(int fast_fps_mode) {
        this.fast_fps_mode = fast_fps_mode;
    }

    public static String defaultGsonString(){
        Gson gson = new Gson();
        return gson.toJson(new StreamPreferences());
    }

}
