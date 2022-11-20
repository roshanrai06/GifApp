package com.roshan.dev.gifapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.roshan.dev.gifapp.ui.BackgroundAsset
import com.roshan.dev.gifapp.ui.SelectBackgroundAsset
import com.roshan.dev.gifapp.ui.theme.GifAppTheme

class MainActivity : ComponentActivity() {
    private val cropAssetLauncher: ActivityResultLauncher<CropImageContractOptions> =
        this@MainActivity.registerForActivityResult(
            CropImageContract()
        ) { result ->
            if (result.isSuccessful) {
                result.uriContent?.let {
                    when (val state = _state.value) {
                        is MainState.DisplaySelectBackgroundAsset,
                        is MainState.DisplayBackgroundAsset -> {
                            _state.value = MainState.DisplayBackgroundAsset(
                                backgroundAssetUri = it,
                            )
                        }

                        else -> throw Exception("Invalid state: $state")
                    }
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong cropping the image.",
                    Toast.LENGTH_LONG
                ).show()
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

    private val _state: MutableState<MainState> = mutableStateOf(MainState.Initial)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GifAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val state = _state.value
                    Column(modifier = Modifier.fillMaxSize()) {
                        when (state) {
                            MainState.Initial -> {
                                _state.value = MainState.DisplaySelectBackgroundAsset
                            }

                            is MainState.DisplaySelectBackgroundAsset -> SelectBackgroundAsset(
                                launchImagePicker = {
                                    backgroundAssetPickerLauncher.launch("image/*")
                                }
                            )

                            is MainState.DisplayBackgroundAsset -> BackgroundAsset(
                                backgroundAssetUri = state.backgroundAssetUri,
                                launchImagePicker = {
                                    backgroundAssetPickerLauncher.launch("image/*")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}