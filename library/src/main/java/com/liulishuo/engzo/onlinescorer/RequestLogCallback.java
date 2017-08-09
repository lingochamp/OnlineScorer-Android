package com.liulishuo.engzo.onlinescorer;

import java.io.File;

/**
 * Created by rantianhua on 17/7/31.
 * a callback for getting log dir
 */

public interface RequestLogCallback {
    void onDirResponse(File logDir);
}
