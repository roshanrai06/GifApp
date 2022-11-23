package com.roshan.dev.gifapp.interactors

import com.roshan.dev.gifapp.domain.CacheProvider
import com.roshan.dev.gifapp.domain.DataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ClearGifCache {
    fun execute(): Flow<DataState<Unit>>
}

/**
 * Interactor for clearing all the cached files from the path provided via [CacheProvider].
 */
class ClearGifCacheInteractor
constructor(
    private val cacheProvider: CacheProvider
) : ClearGifCache {
    override fun execute(): Flow<DataState<Unit>> = flow {
        emit(DataState.Loading(DataState.Loading.LoadingState.Active()))
        try {
            clearGifCache(cacheProvider)
            emit(DataState.Data(Unit)) // Done
        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: CLEAR_CACHED_FILES_ERROR))
        }
        emit(DataState.Loading(DataState.Loading.LoadingState.Idle))
    }

    companion object {
        const val CLEAR_CACHED_FILES_ERROR = "An error occurred deleting the cached files."

        /**
         * Clears all the cached files from the path provided via [CacheProvider].
         */
        private fun clearGifCache(
            cacheProvider: CacheProvider
        ) {
            val internalStorageDirectory = cacheProvider.gifCache()
            val files = internalStorageDirectory.listFiles()
            for (file in files) {
                file.delete()
            }
        }
    }
}