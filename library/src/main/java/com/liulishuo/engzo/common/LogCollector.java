package com.liulishuo.engzo.common;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.liulishuo.engzo.onlinescorer.OnlineScorer;
import com.liulishuo.engzo.onlinescorer.RequestLogCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

/**
 * Created by rantianhua on 17/7/25.
 *
 * a simple collector to collect debug and error log if collector is enabled.
 * the log info will be printed into console and written into files.
 *
 * <p>this class will create a log directory under application shared cache directory to save log
 * files.
 * the log directory's name is a random string of 8 bytes, and every log file has the same name
 * pattern.
 * In this way, the directory is reusable, so I will not create so many directories under cache dir.
 * In the meantime, I can set thresholds for log directory's size and single log file's size. If
 * single log file's
 * size is over, the new log file will be created. If the directory's size is over, the files
 * created at the first time
 * will be deleted until the size is under threshold.
 *
 * <p>By the way,
 *
 * @see StringBuilder is used to cache log info to avoid writting files too frequently, so there
 * aslo
 * is a threshold set for it. However, if there is an error log info occurs, the thresold is
 * invalid.
 *
 * <p>to start this collector, you just need to invoke
 * {@link OnlineScorer#setDebugEnable(boolean)} method.
 * to get the log directory, you just need to invoke
 * {@link RequestLogCallback#onDirResponse(File)}
 * method. This methos is asynchronous because there is a situation that some log info are still
 * cached and I need to write it into file.
 */

public final class LogCollector {

    private static LogCollector sLogCollector;

    private static final long mSbLengthThreshold = 1024 * 50;
    private static final long mLogFileSizeThreshold = 1024 * 1024 / 2;
    private static final long mLogDirSizeThreshold = 1024 * 1024 * 3;

    private static final String TAG = "OnlineScorerRecorder";
    private static final String LOG_FILE_PREFIX = "log";
    private static final Object LOG_DIRECTORY_LOCK = new Object();

    private final StringBuilder mStringBuilder;
    private final SimpleDateFormat mSimpleDateFormat;

    private boolean mEnable;
    private File mLogDir;

    private LogCollector() {
        mStringBuilder = new StringBuilder();
        mSimpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.CHINA);
    }

    public static synchronized LogCollector get() {
        if (sLogCollector == null) {
            sLogCollector = new LogCollector();
        }
        return sLogCollector;
    }

    public synchronized void setEnable(boolean enable) {
        mEnable = enable;
        if (!mEnable) {
            flushToFile();
        }
    }

    public void initLog(Context context) {
        if (mLogDir != null && mLogDir.exists()) return;

        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null || cacheDir.listFiles() == null) {
            cacheDir = context.getCacheDir();
        }

        if (cacheDir.listFiles() == null) return;

        final File existedLogDir = getExistedLogDir(cacheDir);
        if (existedLogDir != null) {
            mLogDir = existedLogDir;
        } else {
            mLogDir = new File(cacheDir, Utility.generateRandomString(8));
        }
    }

    /**
     * @param cacheDir directory of log files, dir's name is a random string of 8 bytes
     *                 and all the log file's name is a confirmed pattern such as log0.txt,
     *                 log1.txt,...,logN.txt, N is an increasing number.
     * @return an exit log dir under cacheDir
     */
    @Nullable
    private File getExistedLogDir(File cacheDir) {
        File[] files = cacheDir.listFiles();
        Pattern pattern = Pattern.compile("log\\d+\\.txt");
        for (File file : files) {
            if (!file.isDirectory()) continue;
            if (file.getName().length() != 8) continue;
            File[] arr = file.listFiles();
            if (arr.length == 0) continue;
            boolean isLogDir = true;
            for (File inner : arr) {
                final String name = inner.getName();
                if (!pattern.matcher(name).matches()) {
                    isLogDir = false;
                    break;
                }
            }
            if (isLogDir) {
                return file;
            }
        }
        return null;
    }

    /**
     * @return current log file, which N in pattern
     * {@link #getExistedLogDir(File)} is largest.
     * if there is no file in log dir, create the first log file.
     */
    @NonNull
    private File getCurrentLogFile() {
        final File[] logs = mLogDir.listFiles();
        if (logs.length == 0) {
            //create the first log file
            return new File(mLogDir, LOG_FILE_PREFIX + "0.txt");
        } else {
            //return latest created file
            sortFilesByName(logs);
            return logs[logs.length - 1];
        }
    }

    private void sortFilesByName(File[] files) {
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File first, File second) {
                final int beginIndex = LOG_FILE_PREFIX.length();
                final int firstNum = Integer.valueOf(
                        first.getName().replace(".txt", "").substring(beginIndex));
                final int secondNum = Integer.valueOf(
                        second.getName().replace(".txt", "").substring(beginIndex));
                return firstNum - secondNum;
            }
        });
    }

    /**
     * @param file current log file
     * @return current log file, create a new file if file's size is larger than
     * @see #mLogFileSizeThreshold
     */
    private File createNewFileIfSizeOver(File file) {
        if (file.length() > mLogFileSizeThreshold) {
            // create a new File
            final String fileName = file.getName().replace(".txt", "");
            final int suffix = Integer.valueOf(fileName.substring(LOG_FILE_PREFIX.length()));
            return new File(file.getParent(), LOG_FILE_PREFIX + (suffix + 1) + ".txt");
        }
        return file;
    }

    /**
     * this method will ensure log dir's space is under
     *
     * @see #mLogDirSizeThreshold
     */
    private void controlLogDirSpace() {
        long logDirSpace = 0L;
        for (File file : mLogDir.listFiles()) {
            logDirSpace += file.length();
        }
        while (logDirSpace > mLogDirSizeThreshold) {
            final File deleteFile = mLogDir.listFiles()[0];
            final long deleteFileLen = deleteFile.length();
            if (deleteFile.delete()) {
                logDirSpace -= deleteFileLen;
                File[] files = mLogDir.listFiles();
                sortFilesByName(files);
                for (int i = 0; i < files.length; i++) {
                    final File restFile = files[i];
                    final File renameFile = new File(restFile.getParent(),
                            LOG_FILE_PREFIX + i + ".txt");
                    if (!restFile.renameTo(renameFile)) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
    }

    private synchronized boolean isEnable() {
        return mEnable;
    }

    public void d(String message) {
        if (isEnable() && !TextUtils.isEmpty(message)) {
            Log.d(TAG, message);

            mStringBuilder.append(getDateTime());
            mStringBuilder.append(": ");
            mStringBuilder.append(message);
            mStringBuilder.append("\n");
            checkStringBuilderSize();
        }
    }

    private void checkStringBuilderSize() {
        if (mStringBuilder.length() > mSbLengthThreshold) {
            final String log = mStringBuilder.toString();
            mStringBuilder.delete(0, mStringBuilder.length());
            flushToFile(log);
        }
    }

    private FutureTask<Boolean> flushToFile() {
        final String log = mStringBuilder.toString();
        if (!TextUtils.isEmpty(log)) {
            mStringBuilder.delete(0, mStringBuilder.length());
            return flushToFile(log);
        } else {
            return null;
        }
    }

    private FutureTask<Boolean> flushToFile(final String log) {
        Callable<Boolean> callable = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                synchronized (LOG_DIRECTORY_LOCK) {
                    if (mLogDir == null) return false;

                    if (!mLogDir.exists()) {
                        if (!mLogDir.mkdir()) return false;
                    }
                    FileOutputStream outputStream = null;
                    File file = getCurrentLogFile();
                    try {
                        if (!file.exists()) {
                            boolean makeFile = file.createNewFile();
                            if (!makeFile) return false;
                        } else {
                            file = createNewFileIfSizeOver(file);
                        }
                        outputStream = new FileOutputStream(file, true);
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
                    controlLogDirSpace();
                    return true;
                }
            }
        };
        FutureTask<Boolean> futureTask = new FutureTask<>(callable);
        InnerExecutors.getInstance().execute(futureTask);
        return futureTask;
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

    /**
     * @param requestLogCallback return log dir until last log info is written
     *                           into log file.
     */
    public void requestLogDir(final RequestLogCallback requestLogCallback) {
        final FutureTask<Boolean> lastFlush = flushToFile();
        if (lastFlush == null) {
            requestLogCallback.onDirResponse(mLogDir);
            return;
        }

        final FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    lastFlush.get();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            requestLogCallback.onDirResponse(mLogDir);
                        }
                    });
                }
                return true;
            }
        });
        InnerExecutors.getInstance().execute(futureTask);
    }
}
