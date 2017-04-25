#include <string.h>
#include <stdio.h>
#include "voice.h"

//初始话压缩器
SpeexPointer voice_encode_init(int quality) {
    SpeexPointer pointer = malloc(sizeof (struct SpeexStruct));
    printf("enc init\n");
    speex_bits_init(&(pointer->ebits));
    pointer->enc_state = speex_encoder_init(&speex_wb_mode);
    speex_encoder_ctl(pointer->enc_state, SPEEX_SET_QUALITY, &quality);
    speex_encoder_ctl(pointer->enc_state, SPEEX_GET_FRAME_SIZE, &(pointer->enc_frame_size));
    printf("enc_frame_size=%d\n", pointer->enc_frame_size);
    return pointer;
}

int get_enc_frame_size(SpeexPointer speexPointer) {
    return speexPointer->enc_frame_size;
}

//销毁压缩器
void voice_encode_release(SpeexPointer speexPointer) {
    printf("enc release\n");
    speex_bits_destroy(&(speexPointer->ebits));
    speex_encoder_destroy(speexPointer->enc_state);
    free(speexPointer);

}
//压缩语音流
int voice_encode(SpeexPointer speexPointer, int enc_frame_size, short in[], int size, char encoded[], int max_buffer_size) {
    short buffer[enc_frame_size];
    char output_buffer[1024 + 4];
    int nsamples = (size - 1) / enc_frame_size + 1;
    int tot_bytes = 0;
    int i = 0;
    for (i = 0; i < nsamples; ++ i) {
        speex_bits_reset(&(speexPointer->ebits));
        memcpy(buffer, in + i * enc_frame_size, 
                    enc_frame_size * sizeof(short));

        speex_encode_int(speexPointer->enc_state, buffer, &(speexPointer->ebits));
        int nbBytes = speex_bits_write(&(speexPointer->ebits), output_buffer + 4,
                                1024 - tot_bytes);
        memcpy(output_buffer, &nbBytes, 4);

        int len = 
                max_buffer_size >= tot_bytes + nbBytes + 4 ? 
                    nbBytes + 4 : max_buffer_size - tot_bytes;

        memcpy(encoded + tot_bytes, output_buffer, len * sizeof(char));
        
        tot_bytes += nbBytes + 4;
    }
    return tot_bytes;
}
