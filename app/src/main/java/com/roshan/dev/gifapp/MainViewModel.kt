package com.roshan.dev.gifapp

import android.view.View
import android.view.Window
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.roshan.dev.gifapp.interactors.PixelCopyJob
import com.roshan.dev.gifapp.interactors.PixelCopyJobInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel : ViewModel() {
    private val _state: MutableState<MainState> = mutableStateOf(MainState.Initial)
    val state: State<MainState> get() = _state
    private val _toastEventRelay: MutableStateFlow<ToastEvent?> = MutableStateFlow(null)
    val toastEventRelay: StateFlow<ToastEvent?> get() = _toastEventRelay
    private val _errorEventRelay: MutableStateFlow<Set<ErrorEvent>> = MutableStateFlow(emptySet())
    val errorEventRelay: StateFlow<Set<ErrorEvent>> get() = _errorEventRelay

    fun updateState(mainState: MainState) {
        _state.value = mainState
    }

    fun publishErrorEvent(errorEvent: ErrorEvent) {
        val current = _errorEventRelay.value.toMutableSet()
        current.add(errorEvent)
        _errorEventRelay.value = current
    }

    fun clearErrorEvents() {
        _errorEventRelay.value = setOf()
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

    fun captureScreenshot(view: View, window: Window) {
        val state = state.value
        check(state is MainState.DisplayBackgroundAsset) { "Invalid State : $state" }
        CoroutineScope(Dispatchers.IO).launch {
            val result = PixelCopyJobInteractor().execute(
                capturingViewBounds = state.capturingViewBounds,
                view = view,
                window = window
            )
            when (result) {
                is PixelCopyJob.PixelCopyJobState.Done -> {
                    _state.value = state.copy(capturedBitmap = result.bitmap)
                }

                is PixelCopyJob.PixelCopyJobState.Error -> {
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = result.message
                        )
                    )
                }
            }
        }
    }

}