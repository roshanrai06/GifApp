package com.roshan.dev.gifapp.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.roshan.dev.gifapp.domain.DataState

@Composable
fun RecordActionBar(
    modifier: Modifier,
    bitmapCaptureLoadingState: DataState.Loading.LoadingState,
    startBitmapCaptureJob: () -> Unit,
    endBitmapCaptureJob: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(3f)
                .height(50.dp)
                .background(Color.Transparent)
        ) {
            when (bitmapCaptureLoadingState) {
                is DataState.Loading.LoadingState.Active -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = Color.Black,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(45.dp)
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        progress = bitmapCaptureLoadingState.progress ?: 0f,
                        backgroundColor = Color.White,
                        color = MaterialTheme.colors.primary
                    )
                }

                else -> {}
            }
        }
        RecordButton(
            modifier = Modifier.weight(1f),
            bitmapCaptureLoadingState = bitmapCaptureLoadingState,
            startBitmapCaptureJob = startBitmapCaptureJob,
            endBitmapCaptureJob = endBitmapCaptureJob
        )
    }
}

@Composable
fun RecordButton(
    modifier: Modifier,
    bitmapCaptureLoadingState: DataState.Loading.LoadingState,
    startBitmapCaptureJob: () -> Unit,
    endBitmapCaptureJob: () -> Unit,
) {
    val isRecording = when (bitmapCaptureLoadingState) {
        is DataState.Loading.LoadingState.Active -> true
        DataState.Loading.LoadingState.Idle -> false
    }
    Button(
        modifier = modifier
            .wrapContentWidth(),
        colors = if (isRecording) {
            ButtonDefaults.buttonColors(
                backgroundColor = Color.Red
            )
        } else {
            ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            )
        },
        onClick = {
            if (!isRecording) {
                startBitmapCaptureJob()
            } else {
                endBitmapCaptureJob()
            }
        }
    ) {
        Text(
            text = if (isRecording) {
                "End"
            } else {
                "Record"
            }
        )
    }
}