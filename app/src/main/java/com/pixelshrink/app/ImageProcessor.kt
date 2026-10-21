package com.pixelshrink.app

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlin.collections.isEmpty

object ImageProcessor {
    data class PrePostImage(val original: Bitmap, val processed: Bitmap?)

    fun processImageCallback(contentResolver: ContentResolver, uri: Uri): PrePostImage {
            val imageBytes = contentResolver.openInputStream(uri)
                ?.use { it.readBytes() }
            var result =
                PrePostImage(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), null)


            if (imageBytes == null || imageBytes.isEmpty()) {
                Log.w("imgproc", "No image bytes available")
                return result
            } else {
                val sourceBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (sourceBitmap == null) {
                    Log.w("imgproc", "Could not decode the selected image")
                    return result
                } else {
                    val width = sourceBitmap.width
                    val height = sourceBitmap.height
                    val argbPixels = IntArray(width * height)
                    sourceBitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)

                    val rgbPixels = ByteArray(argbPixels.size * 3)
                    argbPixels.forEachIndexed { index, color ->
                        val offset = index * 3
                        rgbPixels[offset] = ((color shr 16) and 0xFF).toByte()
                        rgbPixels[offset + 1] = ((color shr 8) and 0xFF).toByte()
                        rgbPixels[offset + 2] = (color and 0xFF).toByte()
                    }

                    val processedBytes = try {
                        NativeImageProcessor.processImage(rgbPixels, width, height, 85)
                    } catch (error: RuntimeException) {
                        Log.e("imgproc", "Native image processing failed", error)
                        null
                    }
                    if (processedBytes == null) {
                        result = PrePostImage(sourceBitmap, null)
                    } else {
                        val processedImage =
                            BitmapFactory.decodeByteArray(processedBytes, 0, processedBytes.size)
                        result = PrePostImage(sourceBitmap, processedImage)
                    }
                }
            }

            return result
    }
}
