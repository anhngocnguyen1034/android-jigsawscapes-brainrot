package com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local

import android.content.Context
import com.nnastudio.jigsawpuzzlebrainrot.core.Constants
import com.nnastudio.jigsawpuzzlebrainrot.data.models.PuzzleCatalogDto
import com.nnastudio.jigsawpuzzlebrainrot.data.models.PuzzleImageDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nguon du lieu catalog. App khong co backend: doc `assets/puzzles/catalog.json`,
 * neu file khong ton tai thi fallback ve catalog mac dinh hard-code.
 */
@Singleton
class PuzzleCatalogDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) {

    suspend fun loadCatalog(): List<PuzzleImageDto> = withContext(ioDispatcher) {
        runCatching {
            val raw = context.assets.open(Constants.CATALOG_ASSET).bufferedReader().use { it.readText() }
            json.decodeFromString<PuzzleCatalogDto>(raw).puzzles
        }.getOrElse { DEFAULT_CATALOG }
    }

    private companion object {
        val DEFAULT_CATALOG = listOf(
            PuzzleImageDto("tralalero", "Tralalero Tralala", "brainrot", "puzzles/tralalero.webp"),
            PuzzleImageDto("bombardiro", "Bombardiro Crocodilo", "brainrot", "puzzles/bombardiro.webp"),
            PuzzleImageDto("tung_sahur", "Tung Tung Sahur", "brainrot", "puzzles/tung_sahur.webp"),
            PuzzleImageDto("lirili", "Lirili Larila", "brainrot", "puzzles/lirili.webp", premium = true),
            PuzzleImageDto("cappuccino", "Ballerina Cappuccina", "meme", "puzzles/cappuccino.webp"),
            PuzzleImageDto("capybara", "Chill Capybara", "animal", "puzzles/capybara.webp")
        )
    }
}
