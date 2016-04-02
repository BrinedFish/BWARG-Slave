package com.bwarg.slave;

import com.google.gson.Gson;


/**
 * Created by LM on 10.02.2016.
 */
public class StreamPreferences {
    public final static String UNKNOWN_NAME = "(Unknown)";

    private int ip_port = 8080;
    private String name = UNKNOWN_NAME;
    private int camIndex =0;
    private int sizeIndex = 0;
    private boolean useFlashLight = false;
    private int quality = 40;

    private boolean auto_exposure_lock = false;
    private boolean auto_white_balance_lock = false;
    private String white_balance = "auto";
    private String focus_mode = "auto";
    private boolean image_stabilization = false;
    private String iso = "auto";
    private String iso_speed = "";
    private int fast_fps_mode = 0;

    public int getIpPort() {
        return ip_port;
    }

    public void setIpPort(int ip_port) {
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

    public boolean getAutoExposureLock() {
        return auto_exposure_lock;
    }

    public void setAutoExposureLock(boolean auto_exposure_lock) {
        this.auto_exposure_lock = auto_exposure_lock;
    }

    public boolean getAutoWhiteBalanceLock() {
        return auto_white_balance_lock;
    }

    public void setAutoWhiteBalanceLock(boolean auto_white_balance_lock) {
        this.auto_white_balance_lock = auto_white_balance_lock;
    }

    public String getFocusMode() {
        return focus_mode;
    }

    public void setFocusMode(String focus_mode) {
        this.focus_mode = focus_mode;
    }

    public boolean getImageStabilization() {
        return image_stabilization;
    }

    public void setImageStabilization(boolean image_stabilization) {
        this.image_stabilization = image_stabilization;
    }

    public String getIso() {
        return iso;
    }

    public void setIso(String iso) {
        this.iso = iso;
    }

    public String getWhiteBalance() {
        return white_balance;
    }

    public void setWhiteBalance(String whitebalance) {
        this.white_balance = whitebalance;
    }

    public int getSizeIndex() {
        return sizeIndex;
    }

    public void setSizeIndex(int sizeIndex) {
        this.sizeIndex = sizeIndex;
    }

    public int getFastFpsMode() {
        return fast_fps_mode;
    }

    public void setFastFpsMode(int fast_fps_mode) {
        this.fast_fps_mode = fast_fps_mode;
    }

    public static String defaultGsonString(){
        Gson gson = new Gson();
        return gson.toJson(new StreamPreferences());
    }

}
