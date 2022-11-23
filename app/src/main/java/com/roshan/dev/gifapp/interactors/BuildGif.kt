package com.roshan.dev.gifapp.interactors

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import com.roshan.dev.gifapp.domain.CacheProvider
import com.roshan.dev.gifapp.domain.DataState
import com.roshan.dev.gifapp.domain.DataState.Data
import com.roshan.dev.gifapp.domain.DataState.Error
import com.roshan.dev.gifapp.domain.DataState.Loading
import com.roshan.dev.gifapp.domain.DataState.Loading.LoadingState
import com.roshan.dev.gifapp.domain.DataState.Loading.LoadingState.Idle
import com.roshan.dev.gifapp.domain.FileNameBuilder
import com.roshan.dev.gifapp.domain.utils.AnimatedGIFWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream
import java.io.File

interface BuildGif {

    fun execute(
        contentResolver: ContentResolver,
        bitmaps: List<Bitmap>,
    ): Flow<DataState<BuildGifResult>>

    data class BuildGifResult(
        val uri: Uri,
        val gifSize: Int,
    )
}

/**
 * Interactor for building a gif given a list of [Bitmap]'s. The resulting gif is saved it to internal storage.
 * We do not need read/write permission because saving to the cache does not require it.
 */
class BuildGifInteractor
constructor(
    private val cacheProvider: CacheProvider,
) : BuildGif {

    override fun execute(
        contentResolver: ContentResolver,
        bitmaps: List<Bitmap>,
    ): Flow<DataState<BuildGif.BuildGifResult>> = flow {
        emit(Loading(LoadingState.Active()))
        try {
            val result = buildGifAndSaveToInternalStorage(
                contentResolver = contentResolver,
                cacheProvider = cacheProvider,
                bitmaps = bitmaps
            )
            emit(Data(result))
        } catch (e: Exception) {
            emit(Error(e.message ?: BUILD_GIF_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val BUILD_GIF_ERROR = "An error occurred while building the gif."
        const val NO_BITMAPS_ERROR = "You can't build a gif when there are no Bitmaps!"
        const val SAVE_GIF_TO_INTERNAL_STORAGE_ERROR =
            "An error occurred while trying to save the" +
                    " gif to internal storage."

        /**
         * Build a Gif from a list of [Bitmap]'s and save to internal storage in [CacheProvider.gifCache].
         * Return a [BuildGifResult] containing the [Uri] and the Size of the new [Bitmap].
         */
        private fun buildGifAndSaveToInternalStorage(
            contentResolver: ContentResolver,
            cacheProvider: CacheProvider,
            bitmaps: List<Bitmap>
        ): BuildGif.BuildGifResult {
            check(bitmaps.isNotEmpty()) { NO_BITMAPS_ERROR }
            val writer = AnimatedGIFWriter(true)
            val bos = ByteArrayOutputStream()
            writer.prepareForWrite(bos, -1, -1)
            for (bitmap in bitmaps) {
                writer.writeFrame(bos, bitmap)
            }
            writer.finishWrite(bos)
            val byteArray = bos.toByteArray()
            val uri = saveGifToInternalStorage(
                contentResolver = contentResolver,
                bytes = byteArray,
                cacheProvider = cacheProvider
            )
            return BuildGif.BuildGifResult(uri, byteArray.size)
        }

        /**
         * Save a [ByteArray] to internal storage.
         * You do not need permissions to write/read to internal storage at any API level.
         */
        private fun saveGifToInternalStorage(
            contentResolver: ContentResolver,
            bytes: ByteArray,
            cacheProvider: CacheProvider,
        ): Uri {
            val fileName = "${FileNameBuilder.buildFileName()}.gif"

            val file = File.createTempFile(fileName, null, cacheProvider.gifCache())
            val uri = file.toUri()
            return contentResolver.openOutputStream(uri)?.let { os ->
                os.write(bytes)
                os.flush()
                os.close()
                uri
            } ?: throw Exception(SAVE_GIF_TO_INTERNAL_STORAGE_ERROR)
        }
    }
}