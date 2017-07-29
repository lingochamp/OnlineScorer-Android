package com.liulishuo.engzo.onlinescorer;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.liulishuo.engzo.common.LogCollector;
import com.liulishuo.engzo.stat.StatManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rantianhua on 17/7/26.
 * This is a class that do some initial works and settings
 */

public class OnlineScorer {

    public static void init(Application application) {
        Config.get().init(application);
        StatManager.get().init(application);
        LogCollector.get().initLog(application);
        application.registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {

                    private List<Activity> mActivities = new ArrayList<>();

                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                        mActivities.add(activity);
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {

                    }

                    @Override
                    public void onActivityResumed(Activity activity) {

                    }

                    @Override
                    public void onActivityPaused(Activity activity) {

                    }

                    @Override
                    public void onActivityStopped(Activity activity) {

                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        mActivities.remove(activity);
                        if (mActivities.size() == 0) {
                            LogCollector.get().d(
                                    "OnlineScorer all activity destroyed, start to release.");
                            StatManager.get().release();
                        }
                    }
                });
    }

    public static void setDebugEnable(boolean enable) {
        LogCollector.get().setEnable(enable);
    }

    /**
     * request log dir
     * @param requestLogCallback return dir of log files. Because write log to file is asynchronous,
     *                           so there needs a callback
     */
    public static void requestLogDir(RequestLogCallback requestLogCallback) {
        LogCollector.get().requestLogDir(requestLogCallback);
    }

}
