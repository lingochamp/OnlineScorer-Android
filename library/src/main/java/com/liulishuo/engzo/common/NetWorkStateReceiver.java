package com.liulishuo.engzo.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;

import com.liulishuo.engzo.onlinescorer.Config;

/**
 * Created by rantianhua on 17/7/28.
 * receive wifi state changes
 */

public class NetWorkStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            Parcelable parcelableExtra = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (null != parcelableExtra) {
                final NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                final NetworkInfo.State state = networkInfo.getState();
                boolean isConnected = state == NetworkInfo.State.CONNECTED;
                if (isConnected) {
                    Config.get().network = "WLAN";
                } else {
                    Config.get().network = "CELLULAR";
                }
                LogCollector.get().d(
                        "NetWorkStateReceiver wifi is connected: " + isConnected);
            }
        }
    }
}
