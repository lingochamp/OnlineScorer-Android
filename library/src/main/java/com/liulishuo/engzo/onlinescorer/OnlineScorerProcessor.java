package com.liulishuo.engzo.onlinescorer;

import android.annotation.SuppressLint;

import com.liulishuo.engzo.common.Base64;
import com.liulishuo.engzo.common.LogCollector;
import com.liulishuo.engzo.common.Utility;
import com.liulishuo.engzo.lingorecorder.processor.AudioProcessor;
import com.liulishuo.jni.SpeexEncoder;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by wcw on 3/28/17.
 */

class OnlineScorerProcessor implements AudioProcessor {

    public static String SERVER = "wss://openapi.llsapp.com/openapi/stream/upload";

    private WebSocket ws;

    private String meta;

    private String message;

    private CountDownLatch latch;

    private boolean encodeToSpeex = false;

    private boolean socketError = false;
    private WebSocketException webSocketException;

    private BaseExercise exercise;
    private String audioId;
    private SpeexEncoder encoder;
    private int frameSize;
    private long pointer;

    private byte[] leftBytes;
    private int leftSize;
    private int frameByteSize;

    private int connectTimeoutMillis = 5000;
    private int responseTimeoutMillis = 15000;

    OnlineScorerProcessor(BaseExercise exercise) {
        this.exercise = exercise;
        if (exercise.getQuality() >= 0) {
            encodeToSpeex = true;
        }
    }

    void setAudioId(String audioId) {
        this.audioId = audioId;
    }

    @Override
    public void start() throws Exception {
        socketError = false;
        message = null;
        latch = new CountDownLatch(1);

        if (encodeToSpeex) {
            encoder = new SpeexEncoder();
            pointer = encoder.init(exercise.getQuality());
            frameSize = encoder.getFrameSize(pointer);

            frameByteSize = frameSize * 2;
            leftBytes = new byte[frameByteSize];
            leftSize = 0;
        }
        ws = connect();
        meta = generateMeta(audioId, exercise);
        byte[] metaArray = meta.getBytes("UTF-8");
        ws.sendBinary(ByteBuffer.allocate(4 + metaArray.length)
                .putInt(metaArray.length)
                .put(metaArray)
                .array());
        LogCollector.get().d("start online processor, encodeToSpeex: " + encodeToSpeex);
    }

    @Override
    public void flow(byte[] bytes, int size) throws Exception {
        if (ws != null) {
            if (encodeToSpeex) {

                if (leftSize > 0) {
                    byte[] temp = getBytes(size + leftSize);
                    System.arraycopy(leftBytes, 0, temp, 0, leftSize);
                    System.arraycopy(bytes, 0, temp, leftSize, size);

                    size += leftSize;
                    bytes = temp;
                    leftSize = 0;
                }

                if (size > frameByteSize) {
                    int encodeSize = size / frameByteSize * frameByteSize;
                    leftSize = size - encodeSize;
                    System.arraycopy(bytes, encodeSize, leftBytes, 0, leftSize);
                    size = encodeSize;
                }

                ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, size);

                short[] buf = new short[size / 2];
                buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(buf);

                bytes = encoder.encode(pointer, frameSize, buf.length, buf);
            }
            ws.sendBinary(bytes);
        }
    }

    @Override
    public boolean needExit() {
        return false;
    }

    @Override
    public void end() throws Exception {
        if (socketError) {
            throw new OnlineScorerRecorder.ScorerException(2, webSocketException);
        } else if (ws != null) {
            byte[] close = {0x45, 0x4f, 0x53};
            ws.sendBinary(close);
            LogCollector.get().d(
                    "end online processor to send eos frame and wait until success.");
            boolean success = latch.await(responseTimeoutMillis, TimeUnit.MILLISECONDS);
            if (!success) {
                LogCollector.get().d("end online processor, eos frame response timeout.");
                throw new OnlineScorerRecorder.ScorerException(1, "response timeout");
            }

        } else {
            throw new OnlineScorerRecorder.ScorerException(1, "socket init and connect error");
        }
    }

    @Override
    public void release() {
        if (ws != null) {
            ws.disconnect();
            ws = null;
            LogCollector.get().d("release online processor disconnecting web socket.");
        }
        if (encodeToSpeex) {
            if (encoder != null) {
                encoder.release(pointer);
                encoder = null;
            }
            LogCollector.get().d("release online processor releasing speex encoder.");
        }
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getResponseTimeoutMillis() {
        return responseTimeoutMillis;
    }

    public void setResponseTimeoutMillis(int responseTimeoutMillis) {
        this.responseTimeoutMillis = responseTimeoutMillis;
    }

    public String getMessage() {
        return message;
    }

    private WebSocket connect() throws IOException, WebSocketException {
        return new WebSocketFactory()
                .setConnectionTimeout(connectTimeoutMillis)
                .createSocket(SERVER)
                .addListener(new WebSocketAdapter() {

                    @Override
                    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                        super.onError(websocket, cause);
                        socketError = true;
                        webSocketException = cause;
                        LogCollector.get().e("online processor error " + cause.getMessage(),
                                cause);
                    }
                    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                        super.onCloseFrame(websocket, frame);
                        LogCollector.get().d("online processor receive close frame.");
                        latch.countDown();
                    }

                    @Override
                    public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                        super.onBinaryFrame(websocket, frame);
                        byte[] bytes = frame.getPayload();
                        if (bytes != null && bytes.length > 4) {
                            OnlineScorerProcessor.this.message = new String(bytes, 4, bytes.length - 4);
                        }
                        LogCollector.get().d("online processor onBinaryFrame, parse message is " + OnlineScorerProcessor.this.message);
                        latch.countDown();
                    }

                })
                .connect();
    }

    private String generateMeta(String audioId, BaseExercise exercise) {
        String meta = null;
        LogCollector.get().d(
                "online processor generate meta: exercise type is " + exercise.getType() +
                        ", exercise quality is " + exercise.getQuality());
        try {
            @SuppressLint("DefaultLocale") String salt = String.format("%d:%s",
                    System.currentTimeMillis() / 1000, Utility.generateRandomString(8));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("item", exercise.toJson());
            jsonObject.put("appID", Config.get().appId);
            jsonObject.put("salt", salt);
            jsonObject.put("audioID", audioId);
            String json = jsonObject.toString();
            String hash = Utility.md5(String.format("%s+%s+%s+%s", Config.get().appId, json, salt, Config.get().appSecret));
            meta = Base64.encode(String.format("%s;hash=%s", json, hash));
        } catch (JSONException e) {
            LogCollector.get().e("online processor generate meta error " + e.getMessage(),
                    e);
        }
        return meta;
    }

    private byte[] bytesPool;

    private byte[] getBytes(int size) {
        if (bytesPool == null) {
            bytesPool = new byte[size];
        } else if (bytesPool.length < size){
            bytesPool = new byte[size];
        }
        return bytesPool;
    }
}
