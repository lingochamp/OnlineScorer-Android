package com.liulishuo.engzo.onlinescorer;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;

/**
 * Created by rantianhua on 17/7/28.
 * record some configurations
 */

public final class Config {


    public String appId;
    public String appSecret;
    public String deviceId;
    public String network;
    public String version;
    public String os;

    private Config() {
        network = "WLAN";
        version = "1.1.0";
        os = "Android";
    }

    private static final class Holder {
        static final Config CONFIG = new Config();
    }

    public static Config get() {
        return Holder.CONFIG;
    }

    void init(Context context, String appId, String appSecret) {
        try {
            deviceId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            deviceId = "0-0";
        }
        this.appSecret = appSecret;
        this.appId = appId;
    }
}
