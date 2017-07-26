package com.liulishuo.engzo.onlinescorer;

import android.content.Context;

import java.io.File;

/**
 * Created by rantianhua on 17/7/26.
 * This is a foreign development class, do some initial works and settings
 */

public class OnlineScorer {

    public static void init(Context context) {
        LogCollector.getInstance().initLog(context);
    }

    public static void setDebugEnable(boolean enable) {
        LogCollector.getInstance().setEnable(enable);
    }

    public static File getLogFile() {
        return LogCollector.getInstance().getLogFile();
    }
}
