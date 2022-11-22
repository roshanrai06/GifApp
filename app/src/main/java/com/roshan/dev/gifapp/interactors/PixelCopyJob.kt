package com.roshan.dev.gifapp.interactors

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

interface PixelCopyJob {
    suspend fun execute(capturingViewBounds: Rect?, view: View, window: Window): PixelCopyJobState
    sealed class PixelCopyJobState {
        data class Done(val bitmap: Bitmap) : PixelCopyJobState()
        data class Error(val message: String) : PixelCopyJobState()
    }
}


class PixelCopyJobInteractor : PixelCopyJob {
    override suspend fun execute(
        capturingViewBounds: Rect?,
        view: View,
        window: Window
    ): PixelCopyJob.PixelCopyJobState = suspendCancellableCoroutine { cont ->
        try {
            check(capturingViewBounds != null) { "Invalid capture area." }
            val bitmap = Bitmap.createBitmap(
                view.width, view.height,
                Bitmap.Config.ARGB_8888
            )

            val locationOfViewInWindow = IntArray(2)
            view.getLocationInWindow(locationOfViewInWindow)
            val xCoordinate = locationOfViewInWindow[0]
            val yCoordinate = locationOfViewInWindow[1]
            val scope = android.graphics.Rect(
                xCoordinate,
                yCoordinate,
                xCoordinate + view.width,
                yCoordinate + view.height
            )
            // Take screenshot
            PixelCopy.request(
                window,
                scope,
                bitmap,
                { p0 ->
                    if (p0 == PixelCopy.SUCCESS) {
                        // Crop the screenshot
                        val bmp = Bitmap.createBitmap(
                            bitmap,
                            capturingViewBounds.left.toInt(),
                            capturingViewBounds.top.toInt(),
                            capturingViewBounds.width.roundToInt(),
                            capturingViewBounds.height.roundToInt()
                        )
                        cont.resume(PixelCopyJob.PixelCopyJobState.Done(bmp))
                    } else {
                        cont.resume(PixelCopyJob.PixelCopyJobState.Error(PIXEL_COPY_ERROR))
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            cont.resume(PixelCopyJob.PixelCopyJobState.Error((e.message ?: PIXEL_COPY_ERROR)))
        }
    }

    companion object {
        const val PIXEL_COPY_ERROR = "An error occurred while running PixelCopy."
    }
}
