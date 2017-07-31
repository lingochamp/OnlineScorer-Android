package com.liulishuo.engzo.onlinescorer;

import android.annotation.SuppressLint;

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

    private static final String SERVER = "wss://openapi.llsapp.com/openapi/stream/upload";

    /**
     * The timeout value in milliseconds for socket connection.
     */
    private static final int TIMEOUT = 5000;

    private WebSocket ws;

    private String meta;

    private String message;

    private CountDownLatch latch = new CountDownLatch(1);

    private boolean encodeToSpeex = false;

    private boolean socketError = false;
    private WebSocketException webSocketException;

    private BaseExercise exercise;

    OnlineScorerProcessor(String appId, String appSecret, BaseExercise exercise) {
        meta = generateMeta(appId, appSecret, exercise);
        this.exercise = exercise;
        if (exercise.getQuality() >= 0) {
            encodeToSpeex = true;
        }
    }

    private SpeexEncoder encoder;
    private int frameSize;

    private long pointer;

    @Override
    public void start() throws Exception {
        socketError = false;

        if (encodeToSpeex) {
            encoder = new SpeexEncoder();
            pointer = encoder.init(exercise.getQuality());
            frameSize = encoder.getFrameSize(pointer);
        }
        ws = connect();
        byte[] metaArray = meta.getBytes("UTF-8");
        ws.sendBinary(ByteBuffer.allocate(4 + metaArray.length)
                .putInt(metaArray.length)
                .put(metaArray)
                .array());
        LogCollector.getInstance().d("start online processor, encodeToSpeex: " + encodeToSpeex);
    }

    @Override
    public void flow(byte[] bytes, int size) throws Exception {
        if (ws != null) {
            if (encodeToSpeex) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
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
            LogCollector.getInstance().d("OnlineScorerProcessor send eos");
            ws.sendBinary(close);
            LogCollector.getInstance().d("OnlineScorerProcessor try to wait");
            boolean success = latch.await(15, TimeUnit.SECONDS);
            if (!success) {
                LogCollector.getInstance().d("OnlineScorerProcessor response timeout");
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
            LogCollector.getInstance().d("release online processor disconnecting web socket.");
        }
        if (encodeToSpeex) {
            if (encoder != null) {
                encoder.release(pointer);
                encoder = null;
            }
            LogCollector.getInstance().d("release online processor releasing speex encoder.");
        }
    }

    public String getMessage() {
        return message;
    }

    private WebSocket connect() throws IOException, WebSocketException {
        return new WebSocketFactory()
                .setConnectionTimeout(TIMEOUT)
                .createSocket(SERVER)
                .addListener(new WebSocketAdapter() {

                    @Override
                    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                        super.onError(websocket, cause);
                        LogCollector.getInstance().d("OnlineScorerProcessor websocket error " + cause);
                        socketError = true;
                        webSocketException = cause;
                    }
                    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                        super.onCloseFrame(websocket, frame);
                        LogCollector.getInstance().d("online processor receive close frame.");
                        latch.countDown();
                    }

                    @Override
                    public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                        super.onBinaryFrame(websocket, frame);
                        byte[] bytes = frame.getPayload();

                        if (bytes != null && bytes.length > 4) {
                            OnlineScorerProcessor.this.message = new String(bytes, 4, bytes.length - 4);
                        }
                        latch.countDown();
                    }

                })
                .connect();
    }

    private String generateMeta(String appId, String appSecret, BaseExercise exercise) {
        String meta = null;
        LogCollector.getInstance().d(
                "online processor generate meta: exercise type is " + exercise.getType() +
                        ", exercise quality is " + exercise.getQuality());
        try {
            @SuppressLint("DefaultLocale") String salt = String.format("%d:%s",
                    System.currentTimeMillis() / 1000, Utility.generateRandomString(8));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("item", exercise.toJson());
            jsonObject.put("appID", appId);
            jsonObject.put("salt", salt);
            String json = jsonObject.toString();
            String hash = Utility.md5(String.format("%s+%s+%s+%s", appId, json, salt, appSecret));
            meta = Base64.encode(String.format("%s;hash=%s", json, hash));
        } catch (JSONException e) {
            LogCollector.getInstance().e("online processor generate meta error " + e.getMessage(),
                    e);
        }
        return meta;
    }
}
