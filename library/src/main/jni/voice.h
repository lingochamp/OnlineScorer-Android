#ifndef VOICE_H
#define VOICE_H

#include "speex/speex.h"

struct SpeexStruct {
    void *enc_state;
    SpeexBits ebits;
    int enc_frame_size;
};

typedef struct SpeexStruct* SpeexPointer;

SpeexPointer voice_encode_init(int quality);
int get_enc_frame_size(SpeexPointer speexPointer);
void voice_encode_release(SpeexPointer pointer);
int voice_encode(SpeexPointer pointer, int enc_frame_size, short in[], int size, char encoded[], int max_buffer_size);
#endif //define VOICE_H
