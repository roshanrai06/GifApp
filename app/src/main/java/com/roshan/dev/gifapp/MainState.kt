package com.roshan.dev.gifapp

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Rect

sealed class MainState {

    object Initial : MainState()

    object DisplaySelectBackgroundAsset : MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmap: Bitmap?
    ) : MainState()
}