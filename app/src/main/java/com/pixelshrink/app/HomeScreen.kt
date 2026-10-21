package com.pixelshrink.app

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onProcessImage: (Uri) -> ImageProcessor.PrePostImage) {
    var message by remember { mutableStateOf("Choose a photo to resize or compress") };
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Pixel Shrink", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        val result = remember { mutableStateOf<Uri?>(null) }
        val imageSelector =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
                result.value = it
            }
        Button(onClick = { imageSelector.launch(PickVisualMediaRequest()) }) {
            Text("Choose the image...", style = MaterialTheme.typography.bodyLarge)
        }
        result.value?.let { uri ->
            val processedData = remember(uri) {
                onProcessImage(uri)
            }

            var shouldShowOriginal by remember(uri) { mutableStateOf(false) }

            if (processedData.processed == null) {
                Text(
                    "Could not process the selected image",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                val imageBitmap: ImageBitmap =
                    if (shouldShowOriginal) processedData.original.asImageBitmap() else processedData.processed.asImageBitmap()
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Modified image",
                    modifier = Modifier.pointerInput(uri) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            Log.i("ui", "Press started")
                            if (awaitLongPressOrCancellation(down.id) != null) {
                                shouldShowOriginal = true
                                Log.i("ui", "Long press...")
                                try {
                                    waitForUpOrCancellation()
                                    Log.i("ui", "Long press finished")
                                } finally {
                                    shouldShowOriginal = false
                                    Log.i("ui", "Long press terminated")
                                }
                            }
                        }
                    }
                )
            }
            Text("Image at $uri has been chosen", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
