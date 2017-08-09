package com.liulishuo.engzo.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;

/**
 * Created by rantianhua on 17/7/27.
 * manage a ExecutorService so that it's easy to reuse it in the library
 */

public class InnerExecutors {

    private ExecutorService mExecutorService;

    private InnerExecutors() {
        mExecutorService = Executors.newCachedThreadPool();
    }

    private static final class Holder {
        static final InnerExecutors INNER_EXECUTORS = new InnerExecutors();
    }

    public static InnerExecutors getInstance() {
        return Holder.INNER_EXECUTORS;
    }

    public void execute(RunnableFuture runnableFuture) {
        mExecutorService.execute(runnableFuture);
    }

    public void execute(Runnable runnable) {
        mExecutorService.execute(runnable);
    }

}
