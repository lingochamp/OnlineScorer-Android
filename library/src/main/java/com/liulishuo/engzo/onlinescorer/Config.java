package com.liulishuo.engzo.onlinescorer;

import android.app.Application;
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

    public void init(Application application) {
        try {
            deviceId = Settings.Secure.getString(application.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            deviceId = "0-0";
        }
    }
}
