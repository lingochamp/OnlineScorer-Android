package com.liulishuo.engzo.onlinescorer;

import com.liulishuo.engzo.lingorecorder.processor.AudioProcessor;
import com.liulishuo.engzo.lingorecorder.utils.LOG;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by wcw on 3/28/17.
 */

public class OnlineScorerProcessor implements AudioProcessor {

    private static final String SERVER =
            BuildConfig.DEBUG ? "wss://rating.llsstaging.com/openapi/stream/upload"
                    : "wss://openapi.llsapp.com/openapi/stream/upload";

    /**
     * The timeout value in milliseconds for socket connection.
     */
    private static final int TIMEOUT = 5000;

    private WebSocket ws;

    private String meta;

    private String message;

    private CountDownLatch latch = new CountDownLatch(1);

    private boolean encodeToSpeex = false;

    private BaseExercise exercise;

    public OnlineScorerProcessor(String appId, String appSecret, BaseExercise exercise) {
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
        byte[] close = {0x45, 0x4f, 0x53};
        LOG.d("OnlineScorerProcessor send eos");
        ws.sendBinary(close);
        LOG.d("OnlineScorerProcessor try to wait");
        boolean success = latch.await(15, TimeUnit.SECONDS);
        if (!success) {
            LOG.d("OnlineScorerProcessor response timeout");
            throw new OnlineScorerRecorder.ScorerException(1, "response timeout");
        }
    }

    @Override
    public void release() {
        if (ws != null) {
            ws.disconnect();
            ws = null;
        }
        if (encodeToSpeex) {
            if (encoder != null) {
                encoder.release(pointer);
                encoder = null;
            }
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
                    public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                        super.onSendingFrame(websocket, frame);
                        LOG.d("OnlineScorerProcessor onSendingFrame + " + frame.toString());
                    }

                    @Override
                    public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception {
                        super.onFrameSent(websocket, frame);
                        LOG.d("OnlineScorerProcessor onFrameSent + " + frame.toString());
                    }

                    @Override
                    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                        super.onError(websocket, cause);
                        LOG.d("OnlineScorerProcessor websocket error " + cause);
                    }
                    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                        super.onCloseFrame(websocket, frame);
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

                    @Override
                    public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                        super.onFrame(websocket, frame);
                        LOG.d("OnlineScorerProcessor onFrame + " + frame.toString());

                    }
                })
                .connect();
    }

    private String generateMeta(String appId, String appSecret, BaseExercise exercise) {
        String meta = null;
        try {
            String salt = String.format("%d:%s", System.currentTimeMillis() / 1000, generateRandomString(8));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("item", exercise.toJson());
            jsonObject.put("appID", appId);
            jsonObject.put("salt", salt);
            String json = jsonObject.toString();
            String hash = md5(String.format("%s+%s+%s+%s", appId, json, salt, appSecret));
            meta = Base64.encode(String.format("%s;hash=%s", json, hash));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return meta;
    }

    private static String generateRandomString(int length){
        String alphabet =
                new String("0123456789abcdef"); //9
        int n = alphabet.length(); //10

        String result = new String();
        Random r = new Random(); //11
        for (int i=0; i<length; i++) //12
            result = result + alphabet.charAt(r.nextInt(n)); //13

        return result;
    }

    private static final String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
