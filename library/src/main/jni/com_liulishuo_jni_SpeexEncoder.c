#include "com_liulishuo_jni_SpeexEncoder.h"

#include <stdlib.h>
#include <string.h>

#include "voice.h"

#ifdef SUPPRESS_ANDROID_LOG
#define LOGD(...) do { } while(0)
#else
#include <android/log.h>

#define LOGD(...)	__android_log_print(ANDROID_LOG_DEBUG  , "libspeex", __VA_ARGS__) 
#endif


  /*
   * Class:     com_liulishuo_jni_SpeexEncoder
   * Method:    init
   * Signature: ()V
   */
  JNIEXPORT jint JNICALL Java_com_liulishuo_jni_SpeexEncoder_init
    (JNIEnv *env, jobject object, jint quality) {
      return voice_encode_init(quality);
    }

  JNIEXPORT jint JNICALL Java_com_liulishuo_jni_SpeexEncoder_header
            (JNIEnv *env, jobject object, jbyteArray buffer) {
      char* output_frame = (*env)->GetByteArrayElements(env, buffer, 0);
      int count = get_header(output_frame);
      (*env)->ReleaseByteArrayElements(env, buffer, output_frame, 0);
      return count;
    }

  /*
   * Class:     com_liulishuo_jni_SpeexEncoder
   * Method:    release
   * Signature: ()V
   */
  JNIEXPORT void JNICALL Java_com_liulishuo_jni_SpeexEncoder_release
    (JNIEnv *env, jobject object) {
        voice_encode_release();
    }

  /*
   * Class:     com_liulishuo_jni_SpeexEncoder
   * Method:    encode
   * Signature: (I[S)[B
   */
  JNIEXPORT jbyteArray JNICALL Java_com_liulishuo_jni_SpeexEncoder_encode
    (JNIEnv *env, jobject object, jint frameSize, jint readCount, jshortArray input_frame) {
        short* in = (*env)->GetShortArrayElements(env, input_frame, 0);

        char encoded[readCount * 2];
        int encoded_count = voice_encode(frameSize, in, readCount, encoded, readCount * 2);

        (*env)->ReleaseShortArrayElements(env, input_frame, in, 0);

        jbyteArray rval;
        rval = (*env)->NewByteArray(env, encoded_count);

        char* output_frame = (*env)->GetByteArrayElements(env, rval, 0);

        memcpy(output_frame, encoded, encoded_count);

        (*env)->ReleaseByteArrayElements(env, rval, output_frame, 0);

        return rval;
    }
