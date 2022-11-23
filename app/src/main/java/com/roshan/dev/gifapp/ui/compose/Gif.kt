package com.roshan.dev.gifapp.ui.compose

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter

@Composable
fun Gif(
    imageLoader: ImageLoader,
    gifUri: Uri?,
    discardGif: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (gifUri != null) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = discardGif,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Red
                        )
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Discard",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            // TODO("Save the gif")
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Green
                        ),
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Save",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                }
                val image: Painter =
                    rememberAsyncImagePainter(model = gifUri, imageLoader = imageLoader)
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((configuration.screenHeightDp * 0.6).dp),
                    contentScale = ContentScale.Crop,
                    painter = image,
                    contentDescription = ""
                )
                // TODO("Add footer for gif screen. This will be for resizing.")
            }
        }
    }
}