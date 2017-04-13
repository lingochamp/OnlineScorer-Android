package com.liulishuo.jni;

/**
 * SpeexEncoder wrapper for JNI code
 * only support sampleRate 16000 16 bit 1 channel
 */
public class SpeexEncoder
{
    public native int init(int quality);

    public native int header(byte[] buffer);

    public native void release();

    public native byte[] encode(int frameSize, int sampleCount, short[] samples);


    static {
        System.loadLibrary("speex");
    }
}