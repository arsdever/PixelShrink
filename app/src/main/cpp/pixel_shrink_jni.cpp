#include <jni.h>

#include "pixel_shrink.h"

extern "C" JNIEXPORT jint JNICALL
Java_com_pixelshrink_app_MainActivity_nativeClampQuality(JNIEnv*, jobject, jint quality) {
    return pixel_shrink::clamp_quality(quality);
}
