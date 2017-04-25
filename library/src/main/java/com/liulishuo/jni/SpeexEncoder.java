package com.liulishuo.jni;

/**
 * SpeexEncoder wrapper for JNI code
 * only support sampleRate 16000 16 bit 1 channel
 */
public class SpeexEncoder
{
    public native long init(int quality);

    public native int getFrameSize(long pointer);

    public native byte[] encode(long pointer, int frameSize, int sampleCount, short[] samples);

    public native void release(long pointer);


    static {
        System.loadLibrary("speex");
    }
}