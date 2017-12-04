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
import com.liulishuo.engzo.stat.StatisticManager;
import com.neovisionaries.ws.client.WebSocketException;

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
    private OnlineScorerProcessor onlineScorerProcessor;

    public OnlineScorerRecorder(BaseExercise exercise, String filePath) {
        initRecorder(exercise, filePath);
        this.exercise = exercise;
    }

    @Deprecated
    public OnlineScorerRecorder(String appId, String appSecret, BaseExercise exercise, String filePath) {
        Config.get().appId = appId;
        Config.get().appSecret = appSecret;
        initRecorder(exercise, filePath);
        this.exercise = exercise;
    }

    private void initRecorder(BaseExercise exercise, String filePath) {
        lingoRecorder = new LingoRecorder();
        lingoRecorder.sampleRate(16000);
        lingoRecorder.channels(1);
        lingoRecorder.bitsPerSample(16);
        onlineScorerProcessor = new OnlineScorerProcessor(exercise);
        lingoRecorder.put("onlineScorer", onlineScorerProcessor);
        final WavProcessor wavProcessor = new WavProcessor(filePath);
        lingoRecorder.put("wavProcessor", wavProcessor);

        lingoRecorder.setOnProcessStopListener(new LingoRecorder.OnProcessStopListener() {
            @Override
            public void onProcessStop(Throwable throwable, Map<String, AudioProcessor> map) {
                if (onProcessStopListener != null) {
                    String filePath = wavProcessor.getFilePath();
                    String message = onlineScorerProcessor.getMessage();

                    LogCollector.get().d("process stop, filePath is " + filePath + ", message is null:" + (message == null));

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
                                LogCollector.get().e(
                                        "process stop 1 " + scorerException.getMessage(),
                                        scorerException);
                            }
                            statItem.collectStatPoint("errorCode", "lingo:" + status);
                        } catch (Exception e) {
                            onProcessStopListener.onProcessStop(e, filePath, null);
                            statItem.collectStatPoint("errorCode", "lingo:" + e.getMessage());
                            LogCollector.get().e("process stop 2 " + e.getMessage(), e);
                        }
                    } else {
                        onProcessStopListener.onProcessStop(throwable, filePath, null);
                        if (throwable instanceof WebSocketException) {
                            WebSocketException webSocketException = (WebSocketException) throwable;
                            statItem.collectStatPoint("errorCode", "websocket:" + webSocketException.getMessage());
                        } else {
                            statItem.collectStatPoint("errorCode", "lingo:" + throwable.getMessage());
                        }
                        LogCollector.get().e("process stop 3 " + throwable.getMessage(),
                                throwable);
                    }
                    statItem.collectStatPoint("responseTime", String.valueOf(System.currentTimeMillis()));
                    StatisticManager.get().stat(statItem);
                } else {
                    LogCollector.get().d("process stop 4, onProcessStopListener is null.");
                }
            }
        });

        lingoRecorder.setOnRecordStopListener(new LingoRecorder.OnRecordStopListener() {

            @Override
            public void onRecordStop(Throwable throwable,
                    Result result) {
                if (onRecordListener != null) {
                    final OnlineScorerRecorder.Result recorderResult = new OnlineScorerRecorder
                            .Result();
                    recorderResult.durationInMills = result.getDurationInMills();
                    onRecordListener.onRecordStop(throwable, recorderResult);
                    statItem.collectStatPoint("recordEndTime",
                            String.valueOf(System.currentTimeMillis()));
                    if (throwable == null) {
                        LogCollector.get().d("stop record, duration is " + result.getDurationInMills());
                    } else {
                        LogCollector.get().e("stop record " + throwable.getMessage(),
                                throwable);
                    }
                } else {
                    LogCollector.get().d("stop record, onRecordListener is null.");
                }
            }
        });
    }

    /**
     * start recording from WavFileRecorder
     * @param wavFilePath   Recorder read from wavFilePath
     */
    public void startRecord(String wavFilePath) {
        final boolean available = isAvailable();
        if (available) {
            final String audioId = UUID.randomUUID().toString();
            onlineScorerProcessor.setAudioId(audioId);
            if (wavFilePath == null) {
                statItem = new OnlineRealTimeRecordItem(audioId, Config.get().network, exercise.getType());
            } else {
                statItem = new RetryRecordItem(audioId, Config.get().network, exercise.getType());
            }
            lingoRecorder.testFile(wavFilePath);
            lingoRecorder.start();
            statItem.collectStatPoint("recordStartTime", String.valueOf(System.currentTimeMillis()));
        }
        LogCollector.get().d(
                "start record, wavFilePath: " + wavFilePath + " available: " + available);
    }

    /**
     * start recording from android audioRecorder
     */
    public void startRecord() {
        startRecord(null);
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

    public int getConnectTimeoutMillis() {
        return onlineScorerProcessor.getConnectTimeoutMillis();
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        onlineScorerProcessor.setConnectTimeoutMillis(connectTimeoutMillis);
    }

    public int getResponseTimeoutMillis() {
        return onlineScorerProcessor.getResponseTimeoutMillis();
    }

    public void setResponseTimeoutMillis(int responseTimeoutMillis) {
        onlineScorerProcessor.setResponseTimeoutMillis(responseTimeoutMillis);
    }

    public interface OnRecordListener {
        void onRecordStop(Throwable error, Result result);
    }

    public interface OnProcessStopListener {
        void onProcessStop(Throwable error, String filePath, String report);
    }

    /**
     * 2 - 客户端 网络数据传输错误
     * 1 - 客户端 response timeout
     * 0 - 成功
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

        ScorerException(int status, Throwable cause) {
            super(cause);
            this.status = status;
            if (cause != null) {
                this.msg = cause.getMessage();
            }
        }

        @SuppressLint("DefaultLocale")
        ScorerException(int status, String msg) {
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

    public static class Result {
        private long durationInMills;
        public long getDurationInMills() {
            return durationInMills;
        }
    }

}
