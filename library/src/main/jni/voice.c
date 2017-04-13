#include "speex/speex.h"
#include <string.h>
#include <stdio.h>
#include <stdbool.h>
#include "voice.h"
#include "speex/speex_header.h"

static void *enc_state;
static SpeexBits ebits;

//初始话压缩器
int voice_encode_init(int quality) {
    int enc_frame_size;
    printf("enc init\n");
    speex_bits_init(&ebits);
    enc_state = speex_encoder_init(&speex_wb_mode);
    speex_encoder_ctl(enc_state, SPEEX_SET_QUALITY, &quality);
    speex_encoder_ctl(enc_state, SPEEX_GET_FRAME_SIZE, &enc_frame_size);
    printf("enc_frame_size=%d\n",enc_frame_size);
    return enc_frame_size;
}

int get_header(char* buffer) {
    SpeexHeader speexHeader;
    speex_init_header(&speexHeader, 16000, 1, &speex_wb_mode);
    int header_size;
    char* header = speex_header_to_packet(&speexHeader, &header_size);
    memcpy(buffer, header, header_size);
    return header_size;
}

//销毁压缩器
void voice_encode_release() {
    printf("enc release\n");
    speex_bits_destroy(&ebits);
    speex_encoder_destroy(enc_state);
}
//压缩语音流
int voice_encode(int enc_frame_size, short in[], int size, char encoded[], int max_buffer_size) {
    short buffer[enc_frame_size];
    char output_buffer[1024 + 4];
    int nsamples = (size - 1) / enc_frame_size + 1;
    int tot_bytes = 0;
    int i = 0;
    for (i = 0; i < nsamples; ++ i) {
        speex_bits_reset(&ebits);
        memcpy(buffer, in + i * enc_frame_size, 
                    enc_frame_size * sizeof(short));

        speex_encode_int(enc_state, buffer, &ebits);
        int nbBytes = speex_bits_write(&ebits, output_buffer + 4,
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
