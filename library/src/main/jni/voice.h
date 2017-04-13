#ifndef VOICE_H
#define VOICE_H

int voice_encode_init(int quality);
int get_header(char* buffer);
void voice_encode_release();
int voice_encode(int enc_frame_size, short in[], int size, char encoded[], int max_buffer_size);
#endif //define VOICE_H
