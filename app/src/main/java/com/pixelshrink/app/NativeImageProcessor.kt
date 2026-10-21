package com.pixelshrink.app

/** The low-level JNI boundary for image processing in C++. */
object NativeImageProcessor {
    init {
        System.loadLibrary("ps_jni")
    }

    external fun processImage(
        rgbPixels: ByteArray,
        width: Int,
        height: Int,
        quality: Int,
    ): ByteArray?
}
