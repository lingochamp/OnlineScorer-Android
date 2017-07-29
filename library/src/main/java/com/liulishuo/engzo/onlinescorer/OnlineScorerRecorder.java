package com.liulishuo.engzo.onlinescorer;

import android.annotation.SuppressLint;
import android.util.Base64;

import com.liulishuo.engzo.common.LogCollector;
import com.liulishuo.engzo.lingorecorder.LingoRecorder;
import com.liulishuo.engzo.lingorecorder.processor.AudioProcessor;
import com.liulishuo.engzo.lingorecorder.processor.WavProcessor;
import com.liulishuo.engzo.stat.OnlineRealTimeRecordItem;
import com.liulishuo.engzo.stat.RetryRecordItem;
import com.liulishuo.engzo.stat.StatItem;
import com.liulishuo.engzo.stat.StatManager;

import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

/**
 * Created by wcw on 4/11/17.
 */

public class OnlineScorerRecorder {

    private LingoRecorder lingoRecorder;
    private StatItem statItem;
    private BaseExercise exercise;

    /**
     *
     * @param appId
     * @param appSecret
     * @param exercise
     * @param filePath
     */
    public OnlineScorerRecorder(String appId, String appSecret, BaseExercise exercise, String filePath) {
        Config.get().appId = appId;
        Config.get().appSecret = appSecret;
        lingoRecorder = new LingoRecorder();
        lingoRecorder.sampleRate(16000);
        lingoRecorder.channels(1);
        lingoRecorder.bitsPerSample(16);
        final OnlineScorerProcessor onlineScorerProcessor = new OnlineScorerProcessor(appId, appSecret, exercise);
        lingoRecorder.put("onlineScorer", onlineScorerProcessor);
        final WavProcessor wavProcessor = new WavProcessor(filePath);
        lingoRecorder.put("wavProcessor", wavProcessor);

        lingoRecorder.setOnProcessStopListener(new LingoRecorder.OnProcessStopListener() {
            @Override
            public void onProcessStop(Throwable throwable, Map<String, AudioProcessor> map) {
                if (onProcessStopListener != null) {
                    String filePath = wavProcessor.getFilePath();
                    String message = onlineScorerProcessor.getMessage();

                    if (throwable == null) {
                        try {
                            JSONObject jsonObject = new JSONObject(message);
                            int status = jsonObject.getInt("status");
                            String errorMsg = jsonObject.getString("msg");
                            byte[] data = Base64.decode(jsonObject.getString("result"), Base64.DEFAULT);
                            String report = new String(data, "UTF-8");

                            if (status == 0) {
                                onProcessStopListener.onProcessStop(null, filePath, report);
                                LogCollector.get().d(
                                        "process stop 0, filePath: " + filePath + " report: "
                                                + report);
                            } else {
                                final ScorerException scorerException = new ScorerException(status,
                                        errorMsg);
                                onProcessStopListener.onProcessStop(scorerException, filePath,
                                        null);
                                statItem.onStatCome("errorCode", "lingo:0");
                                LogCollector.get().e(
                                        "process stop 1 " + scorerException.getMessage(),
                                        scorerException);
                            }
                            statItem.onStatCome("errorCode", "lingo:" + status);
                        } catch (Exception e) {
                            onProcessStopListener.onProcessStop(e, filePath, null);
                            LogCollector.get().e("process stop 2 " + e.getMessage(), e);
                        }
                    } else {
                        onProcessStopListener.onProcessStop(throwable, filePath, null);
                        LogCollector.get().e("process stop 3 " + throwable.getMessage(),
                                throwable);
                    }
                    statItem.onStatCome("responseTime", String.valueOf(System.currentTimeMillis()));
                    StatManager.get().stat(statItem);
                } else {
                    LogCollector.get().d("process stop 4, onProcessStopListener is null.");
                }
            }
        });

        lingoRecorder.setOnRecordStopListener(new LingoRecorder.OnRecordStopListener() {
            @Override
            public void onRecordStop(Throwable throwable) {
                if (onRecordListener != null) {
                    onRecordListener.onRecordStop(throwable);
                    statItem.onStatCome("recordEndTime",
                            String.valueOf(System.currentTimeMillis()));
                    if (throwable == null) {
                        LogCollector.get().d("stop record ");
                    } else {
                        LogCollector.get().e("stop record " + throwable.getMessage(),
                                throwable);
                    }
                } else {
                    LogCollector.get().d("stop record, onRecordListener is null.");
                }
            }
        });

        this.exercise = exercise;
    }

    /**
     * start recording from WavFileRecorder
     * @param wavFilePath   Recorder read from wavFilePath
     */
    public void startRecord(String wavFilePath) {
        final boolean available = isAvailable();
        if (available) {
            statItem = new RetryRecordItem(UUID.randomUUID().toString(),
                    Config.get().network, exercise.getType());
            lingoRecorder.testFile(wavFilePath);
            lingoRecorder.start();
            statItem.onStatCome("recordStartTime", String.valueOf(System.currentTimeMillis()));
        }
        LogCollector.get().d(
                "start record, wavFilePath: " + wavFilePath + " available: " + available);
    }

    /**
     * start recording from android audioRecorder
     */
    public void startRecord() {
        final boolean available = isAvailable();
        if (available) {
            statItem = new OnlineRealTimeRecordItem(UUID.randomUUID().toString(),
                    Config.get().network, exercise.getType());
            lingoRecorder.testFile(null);
            lingoRecorder.start();
            statItem.onStatCome("recordStartTime", String.valueOf(System.currentTimeMillis()));
        }
        LogCollector.get().d("start record, available: " + available);
    }

    public void stopRecord() {
        final boolean available = isAvailable();
        if (available) {
            lingoRecorder.stop();
        }
        LogCollector.get().d("stop record, available: " + available);
    }

    public boolean isRecording() {
        return lingoRecorder.isRecording();
    }

    public boolean isAvailable() {
        return lingoRecorder.isAvailable();
    }

    private OnRecordListener onRecordListener = null;
    private OnProcessStopListener onProcessStopListener = null;

    public void setOnRecordStopListener(OnRecordListener onRecordListener) {
        this.onRecordListener = onRecordListener;
    }

    public void setOnProcessStopListener(OnProcessStopListener onProcessStopListener) {
        this.onProcessStopListener = onProcessStopListener;
    }

    public interface OnRecordListener {
        void onRecordStop(Throwable error);
    }

    public interface OnProcessStopListener {
        void onProcessStop(Throwable error, String filePath, String report);
    }

    /**
     * 0 - 成功
     * 2 - 客户端 网络数据传输错误
     * 1 - 客户端 response timeout
     * -1 - 参数有误
     * -20 - 认证失败
     * -30 - 请求过于频繁
     * -31 - 余额不足
     * -41 - 排队超时
     * -99 - 计算资源不可用
     */
    public static class ScorerException extends Exception {
        private int status;
        private String msg;

        public ScorerException(int status, Throwable cause) {
            super(cause);
            this.status = status;
            if (cause != null) {
                this.msg = cause.getMessage();
            }
        }

        @SuppressLint("DefaultLocale")
        public ScorerException(int status, String msg) {
            super(String.format("response error status = %d msg = %s", status, msg));
            this.status = status;
            this.msg = msg;
        }

        public int getStatus() {
            return status;
        }

        public String getMsg() {
            return msg;
        }
    }

}
