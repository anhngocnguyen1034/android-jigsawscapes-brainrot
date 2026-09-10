package com.nnastudio.jigsawpuzzlebrainrot.data.repository

import com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local.ArtworkDataSource
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleArtwork
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleSource
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.PuzzleImageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PuzzleImageRepositoryImpl @Inject constructor(
    private val artworkDataSource: ArtworkDataSource
) : PuzzleImageRepository {

    override suspend fun loadArtwork(source: PuzzleSource, maxDimension: Int): PuzzleArtwork {
        val bitmap = artworkDataSource.decodeSquare(source, maxDimension)
            ?: artworkDataSource.placeholder(maxDimension, source.seedText())

        return PuzzleArtwork(
            width = bitmap.width,
            height = bitmap.height,
            nativeImage = bitmap
        )
    }

    private fun PuzzleSource.seedText(): String = when (this) {
        is PuzzleSource.Asset -> path
    }
}
