package com.roshan.dev.gifapp

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import com.roshan.dev.gifapp.domain.DataState.Loading.LoadingState
import com.roshan.dev.gifapp.domain.DataState.Loading.LoadingState.Idle

sealed class MainState {

    object Initial: MainState()

    object DisplaySelectBackgroundAsset: MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmaps: List<Bitmap> = listOf(),

        // Displayed as a LinearProgressIndicator in the RecordActionBar
        val bitmapCaptureLoadingState: LoadingState = Idle,

        // Displayed as a CircularIndeterminateProgressBar overlay in the center of the screen.
        val loadingState: LoadingState = Idle,
    ): MainState()

    data class DisplayGif(
        val gifUri: Uri?,
        val originalGifSize: Int,

        // Carry around the original background asset URI in-case user resets the gif.
        val backgroundAssetUri: Uri,

        // Displayed as a CircularIndeterminateProgressBar overlay in the center of the screen.
        val loadingState: LoadingState = Idle,
    ): MainState()
}
