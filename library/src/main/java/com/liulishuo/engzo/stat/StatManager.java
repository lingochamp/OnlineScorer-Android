package com.liulishuo.engzo.stat;

import android.content.Context;
import android.support.annotation.Nullable;

import com.liulishuo.engzo.common.InnerExecutors;
import com.liulishuo.engzo.common.LogCollector;
import com.liulishuo.engzo.common.Utility;
import com.liulishuo.engzo.net.NetTask;
import com.liulishuo.engzo.net.NetTaskListenerAdapter;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

/**
 * Created by rantianhua on 17/7/27.
 * a manager for doing statistics.
 *
 * <p>All kinds of data items are abstracted as
 * @see StatItem which contains specific data points.
 * this manager collect all these items and upload them under
 * a nonblocking, reliable and fault-tolerant strategy.
 *
 * <p>Nonblocking
 * The specific collect actions are delegated to an innerclass {@link BatchStat}.
 * a queue {@linkplain BatchStat#mStatItems} is used to collect all {@link StatItem}, and
 * there is a min time interval {@linkplain BatchStat#mMinUploadTimeInterval} to decide to start
 * upload
 * or not every time an item is added into the queue.
 * if the queue is not empty, all data in it will be assembled as {@link UploadStatItem}.
 * and then they will be uploaded in work thread.
 *
 * <p> Fault-tolarant
 * of course there are some data failed to be uploaded, all the failed {@link UploadStatItem}
 * are collect by a list {@linkplain BatchStat#mFailUploadStatItems}. In order to simplify
 * the process of upload, the new assembled {@link UploadStatItem} will be added into the first
 * positon of {@linkplain BatchStat#mFailUploadStatItems}. During upload, iterate through the list
 * from the first positon and there is a limit for iterating. Because the network operation is in
 * anthoer
 * work thread and item will be added into the tail of the list again, so the dead loop may
 * happen without
 * limit.
 *
 * <p>Reliable
 * After all Activities are destroyed, the {@linkplain #release()} method will be invoked, and
 * shutdown all
 * executors, data have not been upload yet and failed to upload will be saved into files. The
 * {@linkplain #init(Context)} method will create a reusable directory to save for files. and at
 * the constructor
 * of {@link BatchStat}, the directory will be checked and files will be upload immediately.
 */

public class StatManager {

    private BatchStat mBatchStat;
    private File mBackupDir;

    private StatManager() {
    }

    private static final class Holder {
        static final StatManager STAT_MANAGER = new StatManager();
    }

    public static StatManager get() {
        return Holder.STAT_MANAGER;
    }

    public void init(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) cacheDir = context.getCacheDir();

        File existedBackupDir = checkExistedBackupDir(cacheDir);
        if (existedBackupDir != null) {
            mBackupDir = existedBackupDir;
            LogCollector.get().d(
                    "StatManager init with exist backup dir: " + mBackupDir.getAbsolutePath());
        } else {
            mBackupDir = new File(cacheDir, Utility.generateRandomString(9));
            LogCollector.get().d(
                    "StatManager init with new backup dir: " + mBackupDir.getAbsolutePath());
        }
        mBatchStat = new BatchStat();
    }

    /**
     * @param cacheDir application shared cache dir
     * @return existed backup dir, dir name is a random string of 9 bits,
     * the backup files' names in this directory have fixed pattern:
     * backup + random string of 4 bits
     */
    private File checkExistedBackupDir(File cacheDir) {
        final File[] files = cacheDir.listFiles();
        final Pattern pattern = Pattern.compile("backup[a-f0-9]{4}");
        for (File file : files) {
            if (!file.isDirectory()) continue;
            if (file.getName().length() != 9) continue;
            final File[] backups = file.listFiles();
            if (backups.length == 0) continue;
            boolean isBackDir = true;
            for (File backup : backups) {
                final String name = backup.getName();
                if (!pattern.matcher(name).matches()) {
                    isBackDir = false;
                    break;
                }
            }
            if (isBackDir) return file;
        }
        return null;
    }

    public void stat(StatItem statItem) {
        LogCollector.get().d("StatManager add a stat item " + statItem);
        mBatchStat.addItem(statItem);
    }

    public void release() {
        if (mBatchStat == null) return;
        mBatchStat.stopSchedule();
        LogCollector.get().d("StatManager release");

    }

    private final class BatchStat {

        private final ConcurrentLinkedQueue<StatItem> mStatItems;
        private final List<NetTask> mNetTasks;
        private final List<UploadStatItem> mFailUploadStatItems;
        private final int MAX_CONTINUOUS_UPLOAD_LIMITS = 5;
        private final long mMinUploadTimeInterval = 5 * 1000;

        private long mLastUploadTime = System.currentTimeMillis();

        BatchStat() {
            mStatItems = new ConcurrentLinkedQueue<>();
            mNetTasks = new ArrayList<>();
            mFailUploadStatItems = new ArrayList<>();
            uploadBackupStatItems();
        }

        /**
         * upload backup info saved last time.
         */
        private void uploadBackupStatItems() {
            if (mBackupDir == null) return;
            if (!mBackupDir.exists()) return;
            final File[] files = mBackupDir.listFiles();
            if (files.length == 0) return;
            InnerExecutors.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    for (File file : files) {
                        realUploadBackUp(file);
                    }
                }
            });
        }

        private void realUploadBackUp(final File file) {
            final byte[] buff = new byte[1024 * 8];
            int len;
            FileInputStream inputStream = null;
            BufferedOutputStream outputStream = null;
            try {
                inputStream = new FileInputStream(file);
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                outputStream = new BufferedOutputStream(byteArrayOutputStream);
                while ((len = inputStream.read(buff)) != -1) {
                    outputStream.write(buff, 0, len);
                }
                outputStream.flush();
                final byte[] data = byteArrayOutputStream.toByteArray();
                final String str = new String(data);
                LogCollector.get().d("StatManager upload backup file, data is " + str);

                InnerExecutors.getInstance().execute(new NetTask("", NetTask.Method.POST,
                        new NetTaskListenerAdapter() {
                            @Override
                            public void onSuccess(String reponse) {
                                deleteBackupFile(file);
                            }
                        }).setBody(str));

            } catch (Exception e) {
                LogCollector.get().e("StatManager upload backup error, " + e.getMessage(),
                        e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        /**
         * delete backup file after upload
         * if delete failed, clear file content
         *
         * @param file backup file
         */
        private void deleteBackupFile(File file) {
            if (!file.delete()) {
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(file);
                    out.write("".getBytes());
                    out.flush();
                    LogCollector.get().d("StatManager clear backup file success");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                LogCollector.get().d("StatManager delete backup file success");
            }
        }

        /**
         * collect items in
         * @see #mNetTasks and start a net request
         */
        private void uploadStatItems() {
            final UploadStatItem uploadStatItem = assembleBatchData();
            if (uploadStatItem == null) return;
            //anyway, an new UploadStatItem is treated as a failed upload data, so that it's no
            //need to distinguish new data and failed data
            //during upload
            synchronized (mFailUploadStatItems) {
                mFailUploadStatItems.add(0, uploadStatItem);
                final int limits = mFailUploadStatItems.size() > MAX_CONTINUOUS_UPLOAD_LIMITS
                        ? MAX_CONTINUOUS_UPLOAD_LIMITS : mFailUploadStatItems.size();
                int index = 0;

                while (!mFailUploadStatItems.isEmpty()) {
                    LogCollector.get().d(
                            "StatManager start upload stat, total failed size is "
                                    + mFailUploadStatItems.size());
                    final UploadStatItem item = mFailUploadStatItems.remove(0);
                    index++;
                    final NetTask netTask = new NetTask("", NetTask.Method.POST,
                            new NetTaskListenerAdapter() {
                                @Override
                                public void onFailed(int code, String msg) {
                                    synchronized (mFailUploadStatItems) {
                                        mFailUploadStatItems.add(item);
                                        LogCollector.get().d(
                                                "StatManager upload a stat item failed and add to"
                                                        + " mFailUploadStatItems");
                                    }
                                }

                                @Override
                                public void onError(Throwable throwable) {
                                    synchronized (mFailUploadStatItems) {
                                        mFailUploadStatItems.add(item);
                                        LogCollector.get().e(
                                                "StatManager upload a stat item failed and add to"
                                                        + " mFailUploadStatItems"
                                                        + throwable.getMessage(), throwable);
                                    }
                                }
                            }).setBody(item.toJsonString());
                    InnerExecutors.getInstance().execute(netTask);
                    collectAndTrimNetTask(netTask);
                    if (limits <= index) break;
                }
            }
        }

        private void collectAndTrimNetTask(@Nullable NetTask netTask) {
            if (netTask != null) {
                mNetTasks.add(netTask);
            }
            for (int i = 0; i < mNetTasks.size(); i++) {
                if (mNetTasks.get(i).isDone()) {
                    mNetTasks.remove(i);
                    i--;
                }
            }
        }

        /**
         * the items in
         *
         * @return null if there is no items in
         * @see #mStatItems will be assembled as
         * @see UploadStatItem waiting to be uploaded
         * @see #mStatItems or a new
         * @see UploadStatItem
         */
        @Nullable
        private UploadStatItem assembleBatchData() {
            final List<StatItem> statItems = new ArrayList<>();
            while (!mStatItems.isEmpty()) {
                statItems.add(mStatItems.poll());
            }

            LogCollector.get().d(
                    "StatManager assemble batch data, item size is " + statItems.size());
            if (statItems.size() == 0) return null;
            final UploadStatItem uploadStatItem = new UploadStatItem();
            uploadStatItem.stats = statItems;
            return uploadStatItem;
        }

        void addItem(StatItem item) {
            mStatItems.offer(item);
            final long interval = System.currentTimeMillis() - mLastUploadTime;
            LogCollector.get().d("StatManager add an item, interval is " + interval);
            if (interval > mMinUploadTimeInterval) {
                mLastUploadTime += interval;
                uploadStatItems();
            }
        }

        void stopSchedule() {
            saveUnUploadItems();
        }

        /**
         * save rest stat info into file
         *
         * the rest stat info consist of data in
         * @see #mFailUploadStatItems
         * and
         * @see #mStatItems (the data in this queue is checked at a periodic time internal, so
         * there is a case
         * that last data in it have not get chance to be checked, also, they are not be added in
         * @see #mFailUploadStatItems).
         */
        private void saveUnUploadItems() {
            if (mBackupDir == null) return;
            InnerExecutors.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    collectAndTrimNetTask(null);
                    for (NetTask netTask : mNetTasks) {
                        netTask.cancel();
                    }

                    synchronized (mFailUploadStatItems) {
                        final UploadStatItem unCheckedItem = assembleBatchData();
                        if (unCheckedItem != null) {
                            mFailUploadStatItems.add(unCheckedItem);
                        }

                        LogCollector.get().d(
                                "StatManager start save upload failed files, number is "
                                        + mFailUploadStatItems.size());

                        if (!mBackupDir.exists()) {
                            if (!mBackupDir.mkdir()) return;
                        }
                        for (UploadStatItem uploadStatItem : mFailUploadStatItems) {
                            final File file = new File(mBackupDir,
                                    "backup" + Utility.generateRandomString(4));
                            LogCollector.get().d(
                                    "StatManager save an upload failed file in "
                                            + file.getAbsolutePath());
                            FileOutputStream outputStream = null;
                            try {
                                if (!file.createNewFile()) return;
                                outputStream = new FileOutputStream(file);
                                outputStream.write(uploadStatItem.toJsonString().getBytes());
                                outputStream.flush();
                            } catch (IOException e) {
                                LogCollector.get().e(
                                        "StatManager save an upload error " + e.getMessage(), e);
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
                    }
                }
            });
        }
    }
}
