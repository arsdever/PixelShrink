package com.pixelshrink.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import coil.compose.AsyncImage
class MainActivity : ComponentActivity() {
    companion object {
        init {
            System.loadLibrary("pixel_shrink_jni")
        }
    }

    private external fun nativeClampQuality(quality: Int): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result = nativeClampQuality(140)
        android.util.Log.i("PixelShrink", "Native clamp returned $result")

        setContent {
            PixelShrinkTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen() {
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
            AsyncImage(
                model = uri,
                contentDescription = ""
            )
            Text("Image at $uri has been chosen", style = MaterialTheme.typography.bodyLarge)
        }
    }
}