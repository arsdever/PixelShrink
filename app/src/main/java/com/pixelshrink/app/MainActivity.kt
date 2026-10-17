package com.pixelshrink.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelshrink.app.ui.theme.PixelShrinkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.nio.ByteBuffer

class MainActivity : ComponentActivity() {
    companion object {
        init {
            System.loadLibrary("pixel_shrink_jni")
        }
    }

    private external fun nativeProcessImage(
        rgbPixels: ByteArray,
        width: Int,
        height: Int,
        quality: Int,
    ): ByteArray?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val processImageCallback: (Uri) -> Bitmap? = { uri: Uri ->
            val imageBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }

            if (imageBytes == null || imageBytes.isEmpty()) {
                Log.w("imgproc", "No image bytes available")
                null
            } else {
                val sourceBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (sourceBitmap == null) {
                    Log.w("imgproc", "Could not decode the selected image")
                    null
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
                        nativeProcessImage(rgbPixels, width, height, 85)
                    } catch (error: RuntimeException) {
                        Log.e("imgproc", "Native image processing failed", error)
                        null
                    }
                    if (processedBytes == null) {
                        null
                    } else {
                        BitmapFactory.decodeByteArray(processedBytes, 0, processedBytes.size)
                    }
                }
            }
        }

        setContent {
            PixelShrinkTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen(processImageCallback)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onProcessImage: (Uri) -> Bitmap?) {
    var message by remember { mutableStateOf("Choose a photo to resize or compress") };
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Pixel Shrink", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        val result = remember {mutableStateOf<Uri?>(null)}
        val imageSelector = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            result.value = it
        }
        Button(onClick = { imageSelector.launch( PickVisualMediaRequest()) }) {
            Text("Choose the image...", style = MaterialTheme.typography.bodyLarge )
        }
        result.value?.let { uri ->
            val processedBitmap = onProcessImage(uri)
            if (processedBitmap == null) {
                Text("Could not process the selected image", style = MaterialTheme.typography.bodyLarge)
            } else {
                val imageBitmap: ImageBitmap = processedBitmap.asImageBitmap()
                Image(bitmap = imageBitmap, contentDescription = "Modified image")
            }
            Text("Image at $uri has been chosen", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
