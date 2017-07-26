package com.liulishuo.engzo.onlinescorer;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by rantianhua on 17/7/25.
 * a collector to collect debug and error log
 */

final class LogCollector {

    private static LogCollector sLogCollector;
    private static final long mMaxSbLength = 1024 * 1024;
    private static final String TAG = "OnlineScorerRecorder";
    private final StringBuilder mStringBuilder;
    private final SimpleDateFormat mSimpleDateFormat;

    private boolean mEnable;
    private File mFile;

    private LogCollector() {
        mStringBuilder = new StringBuilder();
        mSimpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.CHINA);
    }

    static synchronized LogCollector getInstance() {
        if (sLogCollector == null) {
            sLogCollector = new LogCollector();
        }
        return sLogCollector;
    }

    synchronized void setEnable(boolean enable) {
        mEnable = enable;
        if (!mEnable) {
            flushToFile();
        }
    }

    void initLog(Context context) {
        final File cacheDir = context.getExternalCacheDir();
        mFile = new File(cacheDir, "online-scorer.log");
    }

    private synchronized boolean isEnable() {
        return mEnable;
    }

    public void d(String message) {
        if (isEnable() && !TextUtils.isEmpty(message)) {
            Log.d(TAG, message);

            checkStringBuilderSize();
            mStringBuilder.append(getDateTime());
            mStringBuilder.append(": ");
            mStringBuilder.append(message);
            mStringBuilder.append("\n");
        }
    }

    private void checkStringBuilderSize() {
        if (mStringBuilder.length() > mMaxSbLength) {
            final String log = mStringBuilder.toString();
            mStringBuilder.delete(0, mStringBuilder.length());
            flushToFile(log);
        }
    }

    private void flushToFile() {
        final String log = mStringBuilder.toString();
        if (!TextUtils.isEmpty(log)) {
            flushToFile(log);
            mStringBuilder.delete(0, mStringBuilder.length());
        }
    }

    private void flushToFile(final String log) {
        if (mFile == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileOutputStream outputStream = null;
                try {
                    if (!mFile.exists()) {
                        boolean makeFile = mFile.createNewFile();
                        if (!makeFile) return;
                    }
                    outputStream = new FileOutputStream(mFile, true);
                    outputStream.write(log.getBytes());
                    outputStream.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private String getDateTime() {
        final Date date = new Date(System.currentTimeMillis());
        return mSimpleDateFormat.format(date);
    }

    public void e(String message, Throwable e) {
        if (isEnable() && !TextUtils.isEmpty(message) && e != null) {
            Log.e(TAG, message, e);

            mStringBuilder.append("====error====\n");
            mStringBuilder.append(getDateTime());
            mStringBuilder.append(": ");
            mStringBuilder.append(message);
            mStringBuilder.append("  ");
            mStringBuilder.append(getStackInfo(e));
            mStringBuilder.append("\n");

            flushToFile();
        }
    }

    private String getStackInfo(Throwable e) {
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
        } finally {
            if (sw != null) {
                try {
                    sw.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            if (pw != null) {
                pw.close();
            }
        }
        return sw.toString();
    }

    @Nullable
    File getLogFile() {
        if (isEnable()) {
            flushToFile();
            return mFile;
        }
        return null;
    }

}
