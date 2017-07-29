package com.liulishuo.engzo.net;

import android.text.TextUtils;

import com.liulishuo.engzo.common.LogCollector;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


/**
 * Created by rantianhua on 17/7/27.
 * a simple network action's wrapper
 */

public final class NetTask implements Runnable {

    private final String mUrl;
    private final Method mMethod;
    private final NetTaskListener mTaskListener;

    private Map<String, String> mHeader;
    private String mBody;
    private Map<String, String> mParameters;
    private boolean mCancel;
    private boolean mDone;

    public NetTask(String url, Method method, NetTaskListener netTaskListener) {
        this.mUrl = url;
        this.mMethod = method;
        this.mTaskListener = netTaskListener;
    }

    public NetTask addHeader(String name, String value) {
        if (TextUtils.isEmpty(name)) return this;
        if (TextUtils.isEmpty(value)) return this;
        if (mHeader == null) mHeader = new HashMap<>();
        mHeader.put(name, value);
        return this;
    }

    public NetTask setBody(String body) {
        LogCollector.get().d("NetTask set body: " + body);
        if (TextUtils.isEmpty(body)) return this;
        this.mBody = body;
        return this;
    }

    public NetTask addParameter(String name, String value) {
        if (TextUtils.isEmpty(name)) return this;
        if (TextUtils.isEmpty(value)) return this;
        if (mParameters == null) mParameters = new HashMap<>();
        mParameters.put(name, value);
        return this;
    }

    public String getBody() {
        return mBody;
    }

    @Override
    public void run() {
        if (isCancel()) {
            LogCollector.get().d("NetTask ready to run but canceled");
            return;
        }
        switch (mMethod) {
            case GET:
                doGet();
            case POST:
                doPost();
        }
        setDone(true);
    }

    private void doPost() {
        LogCollector.get().d("NetTask start to do a post");
        HttpsURLConnection urlConnection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            final URL realUrl = new URL(mUrl);
            urlConnection = (HttpsURLConnection) realUrl.openConnection();
            urlConnection.setRequestMethod(mMethod.getName());
            urlConnection.setDoOutput(true);
            addHeaderForResponse(urlConnection);

            outputStream = urlConnection.getOutputStream();
            outputStream.write(mBody.getBytes());
            outputStream.flush();
            outputStream.close();
            outputStream = null;

            if (mTaskListener == null) return;
            int code = urlConnection.getResponseCode();
            if (code == 200) {
                inputStream = urlConnection.getInputStream();
                final String reponse = inputStreamToString(inputStream);
                mTaskListener.onSuccess(reponse);
                inputStream = null;
                LogCollector.get().d("NetTask do post finish, response is " + reponse);
            } else {
                final String msg = urlConnection.getResponseMessage();
                mTaskListener.onFailed(code, msg);
                LogCollector.get().d("NetTask do post finish, http code is " + code);
            }
        } catch (Exception e) {
            if (mTaskListener != null) {
                mTaskListener.onError(e);
            } else {
                e.printStackTrace();
            }
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
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private void doGet() {
        String completeUrl = null;
        if (mParameters != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(mUrl).append("?");
            for (Map.Entry<String, String> entry : mParameters.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            sb.delete(sb.length() - 1, sb.length());
            completeUrl = sb.toString();
        } else {
            completeUrl = mUrl;
        }

        HttpsURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            final URL server = new URL(completeUrl);
            urlConnection = (HttpsURLConnection) server.openConnection();
            urlConnection.setRequestMethod(mMethod.getName());
            addHeaderForResponse(urlConnection);

            final int code = urlConnection.getResponseCode();
            if (mTaskListener == null) return;
            if (code == 200) {
                inputStream = urlConnection.getInputStream();
                final String reponse = inputStreamToString(inputStream);
                mTaskListener.onSuccess(reponse);
                inputStream = null;
            } else {
                final String msg = urlConnection.getResponseMessage();
                mTaskListener.onFailed(code, msg);
            }
        } catch (Exception e) {
            if (mTaskListener != null) {
                mTaskListener.onError(e);
            } else {
                e.printStackTrace();
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private void addHeaderForResponse(HttpsURLConnection urlConnection) {
        if (mHeader != null) {
            for (Map.Entry<String, String> entry : mHeader.entrySet()) {
                urlConnection.addRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    private String inputStreamToString(InputStream is) {
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        byte[] buffer = new byte[1024 * 8];
        int length;
        try {
            while ((length = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }
            bos.flush();
            byte[] bytes = baos.toByteArray();
            return new String(bytes, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void cancel() {
        mCancel = true;
    }

    public synchronized boolean isCancel() {
        return mCancel;
    }

    private synchronized void setDone(boolean done) {
        mDone = done;
    }

    public synchronized boolean isDone() {
        return mDone;
    }

    public enum Method {

        GET("GET"),
        POST("POST");

        private String name;

        Method(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
