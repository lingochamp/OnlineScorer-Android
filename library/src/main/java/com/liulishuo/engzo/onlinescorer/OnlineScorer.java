package com.liulishuo.engzo.onlinescorer;

import android.content.Context;

import java.io.File;

/**
 * Created by rantianhua on 17/7/26.
 * This is a class that do some initial works and settings
 */

public class OnlineScorer {

    public static void init(Context context) {
        LogCollector.getInstance().initLog(context);
    }

    public static void setDebugEnable(boolean enable) {
        LogCollector.getInstance().setEnable(enable);
    }

    /**
     * @return the dir of log files, because write log to file is asynchronous,
     * so there needs a callback
     */
    public static void requestLogDir(RequestLogCallback requestLogCallback) {
        LogCollector.getInstance().requestLogDir(requestLogCallback);
    }

    public interface RequestLogCallback {
        void onDirResponse(File logDir);
    }
}
