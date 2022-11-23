package com.roshan.dev.gifapp

import android.content.ContentResolver
import android.view.View
import android.view.Window
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roshan.dev.gifapp.domain.CacheProvider
import com.roshan.dev.gifapp.domain.DataState
import com.roshan.dev.gifapp.domain.DataState.Loading.LoadingState.Active
import com.roshan.dev.gifapp.domain.DataState.Loading.LoadingState.Idle
import com.roshan.dev.gifapp.interactors.BuildGif
import com.roshan.dev.gifapp.interactors.BuildGifInteractor
import com.roshan.dev.gifapp.interactors.CaptureBitmaps
import com.roshan.dev.gifapp.interactors.CaptureBitmapsInteractor
import com.roshan.dev.gifapp.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_ERROR
import com.roshan.dev.gifapp.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_SUCCESS
import com.roshan.dev.gifapp.interactors.ClearGifCache
import com.roshan.dev.gifapp.interactors.ClearGifCacheInteractor
import com.roshan.dev.gifapp.interactors.PixelCopyJob
import com.roshan.dev.gifapp.interactors.PixelCopyJobInteractor
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import java.util.UUID

class MainViewModel : ViewModel() {

    private val dispatcher = IO
    private val pixelCopy: PixelCopyJob = PixelCopyJobInteractor()
    private val captureBitmaps: CaptureBitmaps = CaptureBitmapsInteractor(
        pixelCopyJob = pixelCopy
    )
    private var cacheProvider: CacheProvider? = null

    private val _state: MutableState<MainState> = mutableStateOf(MainState.Initial)
    val state: State<MainState> get() = _state
    private val _toastEventRelay: MutableStateFlow<ToastEvent?> = MutableStateFlow(null)
    val toastEventRelay: StateFlow<ToastEvent?> get() = _toastEventRelay
    private val _errorRelay: MutableStateFlow<Set<ErrorEvent>> = MutableStateFlow(setOf())
    val errorRelay: StateFlow<Set<ErrorEvent>> get() = _errorRelay

    fun setCacheProvider(
        cacheProvider: CacheProvider
    ) {
        this.cacheProvider = cacheProvider
    }

    private fun buildGif(
        contentResolver: ContentResolver,
    ) {
        check(state.value is MainState.DisplayBackgroundAsset) { "buildGif: Invalid state: ${state.value}" }
        val capturedBitmaps = (state.value as MainState.DisplayBackgroundAsset).capturedBitmaps
        check(capturedBitmaps.isNotEmpty()) { "You have no bitmaps to build a gif from!" }
        updateState(
            (state.value as MainState.DisplayBackgroundAsset).copy(loadingState = Active())
        )
        // TODO("This will be injected into the ViewModel later.")
        val buildGif: BuildGif = BuildGifInteractor(
            cacheProvider = cacheProvider!!, // !! for now to force it to work.
        )
        buildGif.execute(
            contentResolver = contentResolver,
            bitmaps = capturedBitmaps,
        ).onEach { dataState ->
            when (dataState) {
                is DataState.Data -> {
                    (state.value as MainState.DisplayBackgroundAsset).let {
                        val gifSize = dataState.data?.gifSize ?: 0
                        val gifUri = dataState.data?.uri
                        updateState(
                            MainState.DisplayGif(
                                gifUri = gifUri,
                                originalGifSize = gifSize,
                                backgroundAssetUri = it.backgroundAssetUri
                            )
                        )
                    }
                }

                is DataState.Error -> {
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = dataState.message
                        )
                    )
                    updateState(
                        (state.value as MainState.DisplayBackgroundAsset).copy(loadingState = Idle)
                    )
                }

                is DataState.Loading -> {
                    // Need to check here since there is a state change to DisplayGif and loading
                    //  emissions can technically still come after the job is complete
                    if (state.value is MainState.DisplayBackgroundAsset) {
                        updateState(
                            (state.value as MainState.DisplayBackgroundAsset).copy(loadingState = dataState.loadingState)
                        )
                    }
                }
            }
        }.flowOn(dispatcher).launchIn(viewModelScope)
    }

    fun runBitmapCaptureJob(
        contentResolver: ContentResolver,
        view: View,
        window: Window
    ) {
        check(state.value is MainState.DisplayBackgroundAsset) { "Invalid state: $state" }
        updateState(
            (state.value as MainState.DisplayBackgroundAsset).copy(
                bitmapCaptureLoadingState = Active(
                    0f
                )
            )
        )

        // We need a way to stop the job if a user presses "STOP". So create a Job for this.
        val bitmapCaptureJob = Job()
        // Create convenience function for checking if the user pressed "STOP".
        val checkShouldCancelJob: (MainState) -> Unit = { mainState ->
            val shouldCancel = when (mainState) {
                is MainState.DisplayBackgroundAsset -> {
                    mainState.bitmapCaptureLoadingState !is Active
                }

                else -> true
            }
            if (shouldCancel) {
                bitmapCaptureJob.cancel(CAPTURE_BITMAP_SUCCESS)
            }
        }
        // Execute the use-case.
        captureBitmaps.execute(
            capturingViewBounds = (state.value as MainState.DisplayBackgroundAsset).capturingViewBounds,
            window = window,
            view = view,
        ).onEach { dataState ->
            // If the user hits the "STOP" button, complete the job by canceling.
            // Also cancel if there was some kind of state change.
            checkShouldCancelJob(state.value)
            when (dataState) {
                is DataState.Data -> {
                    dataState.data?.let { bitmaps ->
                        updateState(
                            (state.value as MainState.DisplayBackgroundAsset).copy(
                                capturedBitmaps = bitmaps
                            )
                        )
                    }
                }

                is DataState.Error -> {
                    // For this use-case, if an error occurs we need to stop the job.
                    // Otherwise it will keep trying to capture bitmaps and failing over and over.
                    bitmapCaptureJob.cancel(CAPTURE_BITMAP_ERROR)

                    updateState(
                        (state.value as MainState.DisplayBackgroundAsset).copy(
                            bitmapCaptureLoadingState = Idle
                        )
                    )
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = dataState.message
                        )
                    )
                }

                is DataState.Loading -> {
                    updateState(
                        (state.value as MainState.DisplayBackgroundAsset).copy(
                            bitmapCaptureLoadingState = dataState.loadingState
                        )
                    )
                }
            }
        }.flowOn(dispatcher).launchIn(viewModelScope + bitmapCaptureJob)
            .invokeOnCompletion { throwable ->
                updateState(
                    (state.value as MainState.DisplayBackgroundAsset).copy(
                        bitmapCaptureLoadingState = Idle
                    )
                )
                val onSuccess: () -> Unit = {
                    buildGif(contentResolver = contentResolver)
                }
                // If the throwable is null OR the message = CAPTURE_BITMAP_SUCCESS, it was successful.
                when (throwable) {
                    null -> onSuccess()
                    else -> {
                        if (throwable.message == CAPTURE_BITMAP_SUCCESS) {
                            onSuccess()
                        } else { // If an error occurs, do not try to build the gif.
                            publishErrorEvent(
                                ErrorEvent(
                                    id = UUID.randomUUID().toString(),
                                    message = throwable.message ?: CAPTURE_BITMAP_ERROR
                                )
                            )
                        }
                    }
                }
            }
    }

    private fun clearCachedFiles() {
        // TODO("We will be injecting this later when we add Hilt for DI.")
        val clearGifCache: ClearGifCache = ClearGifCacheInteractor(
            cacheProvider = cacheProvider!! // !! for now to force
        )
        clearGifCache.execute().onEach { _ ->
            // Don't update UI here. Should just succeed or fail silently.
        }.flowOn(dispatcher).launchIn(viewModelScope)
    }

    fun deleteGif() {
        clearCachedFiles()
        check(state.value is MainState.DisplayGif) { "deleteGif: Invalid state: ${state.value}" }
        _state.value = MainState.DisplayBackgroundAsset(
            backgroundAssetUri = (state.value as MainState.DisplayGif).backgroundAssetUri,
        )
    }

    fun endBitmapCaptureJob() {
        updateState((state.value as MainState.DisplayBackgroundAsset).copy(bitmapCaptureLoadingState = Idle))
    }

    fun updateState(mainState: MainState) {
        _state.value = mainState
    }

    fun showToast(
        id: String = UUID.randomUUID().toString(),
        message: String
    ) {
        _toastEventRelay.tryEmit(
            ToastEvent(
                id = id,
                message = message
            )
        )
    }

    private fun publishErrorEvent(errorEvent: ErrorEvent) {
        val current = _errorRelay.value.toMutableSet()
        current.add(errorEvent)
        _errorRelay.value = current
    }

    fun clearErrorEvents() {
        _errorRelay.value = setOf()
    }
}