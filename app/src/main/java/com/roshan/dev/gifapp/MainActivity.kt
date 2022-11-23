package com.roshan.dev.gifapp

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.roshan.dev.gifapp.domain.RealCacheProvider
import com.roshan.dev.gifapp.ui.compose.BackgroundAsset
import com.roshan.dev.gifapp.ui.compose.Gif
import com.roshan.dev.gifapp.ui.compose.SelectBackgroundAsset
import com.roshan.dev.gifapp.ui.theme.GifAppTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var imageLoader: ImageLoader

    private val cropAssetLauncher: ActivityResultLauncher<CropImageContractOptions> =
        this@MainActivity.registerForActivityResult(
            CropImageContract()
        ) { result ->
            if (result.isSuccessful) {
                result.uriContent?.let {
                    when (val state = viewModel.state.value) {
                        is MainState.DisplaySelectBackgroundAsset,
                        is MainState.DisplayBackgroundAsset -> {
                            viewModel.updateState(
                                MainState.DisplayBackgroundAsset(
                                    backgroundAssetUri = it,
                                    capturingViewBounds = null,
                                )
                            )
                        }

                        else -> throw Exception("Invalid state: $state")
                    }
                }
            } else {
                viewModel.showToast(message = "Something went wrong cropping the image.")
            }
        }

    private val backgroundAssetPickerLauncher: ActivityResultLauncher<String> =
        this@MainActivity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) {
            cropAssetLauncher.launch(
                options(
                    uri = it,
                ) {
                    setGuidelines(CropImageView.Guidelines.ON)
                }
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO("Will remove this when we add Hilt for DI.")
        imageLoader = ImageLoader.Builder(application)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

        // TODO("Will remove this when we add Hilt for DI.")
        viewModel.setCacheProvider(RealCacheProvider(application))

        viewModel.toastEventRelay.onEach { toastEvent ->
            if (toastEvent != null) {
                Toast.makeText(this@MainActivity, toastEvent.message, Toast.LENGTH_LONG).show()
            }
        }.launchIn(lifecycleScope)

        setContent {
            GifAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val state = viewModel.state.value
                    val view = LocalView.current
                    Column(modifier = Modifier.fillMaxSize()) {
                        when (state) {
                            MainState.Initial -> {
                                // TODO("Show loading UI")
                                viewModel.updateState(
                                    MainState.DisplaySelectBackgroundAsset
                                )
                            }

                            is MainState.DisplaySelectBackgroundAsset -> SelectBackgroundAsset(
                                launchImagePicker = {
                                    backgroundAssetPickerLauncher.launch("image/*")
                                }
                            )

                            is MainState.DisplayBackgroundAsset -> BackgroundAsset(
                                backgroundAssetUri = state.backgroundAssetUri,
                                updateCapturingViewBounds = { rect ->
                                    viewModel.updateState(
                                        state.copy(capturingViewBounds = rect)
                                    )
                                },
                                startBitmapCaptureJob = {
                                    viewModel.runBitmapCaptureJob(
                                        contentResolver = contentResolver,
                                        view = view,
                                        window = window
                                    )
                                },
                                endBitmapCaptureJob = viewModel::endBitmapCaptureJob,
                                bitmapCaptureLoadingState = state.bitmapCaptureLoadingState,
                                launchImagePicker = {
                                    backgroundAssetPickerLauncher.launch("image/*")
                                },
                                loadingState = state.loadingState
                            )

                            is MainState.DisplayGif -> Gif(
                                imageLoader = imageLoader,
                                gifUri = state.gifUri,
                                discardGif = viewModel::deleteGif
                            )
                        }
                    }
                }
            }
        }
    }
}