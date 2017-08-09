package com.liulishuo.engzo.net;

/**
 * Created by rantianhua on 17/7/27.
 */

public interface NetTaskListener {

    void onSuccess(String reponse);

    void onFailed(int code, String msg);

    void onError(Throwable throwable);
}
