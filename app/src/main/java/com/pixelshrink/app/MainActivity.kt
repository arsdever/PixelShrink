package com.pixelshrink.app

import android.content.ContentResolver
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
import coil.compose.AsyncImage
import java.nio.ByteBuffer
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get

class MainActivity : ComponentActivity() {
    companion object {
        init {
            System.loadLibrary("pixel_shrink_jni")
        }
    }

    private external fun nativeClampQuality(buffer:ByteBuffer): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val processImageCallback: (Uri) -> Bitmap = { uri: Uri ->
            val resolver: ContentResolver = contentResolver
            val imageBytes = resolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            }

            if (imageBytes == null) {
                Log.w("imgproc", "No image bytes available")
                Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
            } else {
                val bitmap: Bitmap? = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap == null || bitmap.byteCount == 0) {
                    Log.w("imgproc", "Bitmap was empty")
                    Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
                }

                val byteBuffer = ByteBuffer.allocateDirect(bitmap!!.byteCount)
                bitmap.copyPixelsToBuffer(byteBuffer)
                Log.i("imgproc", "Creating the image")
                Log.i("imgproc", "Bitmap config: $bitmap.config")
                val modifiedBitmap: Bitmap = createBitmap(
                    bitmap.width,
                    bitmap.height,
                    bitmap.config ?: Bitmap.Config.ARGB_8888
                )

                byteBuffer.rewind()
                val processed = nativeClampQuality(byteBuffer)
                val color = bitmap.get(0,0)
                Log.i("imgproc", "Processed $processed, First pixel is $color")

                byteBuffer.rewind()
                modifiedBitmap.copyPixelsFromBuffer(byteBuffer)

                modifiedBitmap
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
private fun HomeScreen(onProcessImage: (Uri)->Bitmap) {
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
            val bmp: ImageBitmap = onProcessImage(uri).asImageBitmap()
            Image(
                bitmap = bmp,
                contentDescription = "Modified image"
            )
            Text("Image at $uri has been chosen", style = MaterialTheme.typography.bodyLarge)
        }
    }
}