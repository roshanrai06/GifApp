package com.roshan.dev.gifapp.interactors

import android.graphics.Bitmap
import android.os.Build
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.ui.geometry.Rect
import com.roshan.dev.gifapp.domain.DataState
import com.roshan.dev.gifapp.domain.DataState.Loading.LoadingState
import com.roshan.dev.gifapp.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_INTERVAL_MS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface CaptureBitmaps {

    /**
     * @param window is only required if [Build.VERSION_CODES] >= [Build.VERSION_CODES.O].
     *  Otherwise this can be null.
     */
    fun execute(
        capturingViewBounds: Rect?,
        view: View?,
        window: Window,
    ): Flow<DataState<List<Bitmap>>>
}

/**
 * Interactor for capturing a list of bitmaps by screenshotting the device every [CAPTURE_INTERVAL_MS].
 * This makes things a little annoying because [PixelCopy.request] has a callback we need to use.
 */
class CaptureBitmapsInteractor
constructor(
    private val pixelCopyJob: PixelCopyJob,
) : CaptureBitmaps {

    override fun execute(
        capturingViewBounds: Rect?,
        view: View?,
        window: Window,
    ): Flow<DataState<List<Bitmap>>> = flow {
        emit(DataState.Loading(LoadingState.Active()))
        try {
            check(capturingViewBounds != null) { "Invalid view bounds." }
            check(view != null) { "Invalid view." }
            var elapsedTime = 0f
            val bitmaps: MutableList<Bitmap> = mutableListOf()
            while (elapsedTime < TOTAL_CAPTURE_TIME_MS) {
                delay(CAPTURE_INTERVAL_MS.toLong())
                elapsedTime += CAPTURE_INTERVAL_MS
                emit(DataState.Loading(LoadingState.Active(elapsedTime / TOTAL_CAPTURE_TIME_MS)))

                val pixelCopyJobState = pixelCopyJob.execute(
                    capturingViewBounds = capturingViewBounds,
                    view = view,
                    window = window
                )
                when (pixelCopyJobState) {
                    is PixelCopyJob.PixelCopyJobState.Done -> {
                        pixelCopyJobState.bitmap
                        bitmaps.add(pixelCopyJobState.bitmap)
                    }

                    is PixelCopyJob.PixelCopyJobState.Error -> {
                        throw Exception(pixelCopyJobState.message)
                    }
                }
                // Every time a new bitmap is captured, emit the updated list.

                emit(DataState.Data(bitmaps.toList()))
            }
        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: CAPTURE_BITMAP_ERROR))
        }
        emit(DataState.Loading(LoadingState.Idle))
    }

    companion object {
        const val TOTAL_CAPTURE_TIME_MS = 4000f
        const val CAPTURE_INTERVAL_MS = 250f
        const val CAPTURE_BITMAP_ERROR = "An error occurred while capturing the bitmaps."
        const val CAPTURE_BITMAP_SUCCESS = "Completed Successfully"
    }
}