#include <jni.h>

#include "pixel_shrink.h"
#include <iostream>

extern "C" JNIEXPORT int JNICALL
Java_com_pixelshrink_app_MainActivity_nativeClampQuality(JNIEnv *env, jobject,
                                                         jobject buffer) {

    void *nativePtr = env->GetDirectBufferAddress(buffer);
    auto capacity = env->GetDirectBufferCapacity(buffer);
    if (nativePtr == nullptr) {
        // Handle error: Not a direct ByteBuffer or buffer capacity is 0
        return -1;
    }

    int i = 0;
    for (; i < capacity; i++) {
        if (i % 4 == 0) { continue; }
        reinterpret_cast<char *>(nativePtr)[i] = 0x0;
    }

    return i;
}