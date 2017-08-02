package com.liulishuo.engzo.onlinescorersdk;

import android.app.Application;

import com.liulishuo.engzo.onlinescorer.OnlineScorer;

/**
 * Created by rantianhua on 17/7/31.
 */

public class DemoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        OnlineScorer.init(this, "test", "test");
        OnlineScorer.setDebugEnable(true);
    }

}
