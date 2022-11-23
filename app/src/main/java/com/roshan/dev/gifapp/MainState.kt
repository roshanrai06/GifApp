import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import com.roshan.dev.gifapp.domain.DataState

sealed class MainState {

    object Initial: MainState()

    object DisplaySelectBackgroundAsset: MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmaps: List<Bitmap> = listOf(),
        // Displayed as a LinearProgressIndicator in the RecordActionBar
        val bitmapCaptureLoadingState: DataState.Loading.LoadingState = DataState.Loading.LoadingState.Idle
    ): MainState()
}