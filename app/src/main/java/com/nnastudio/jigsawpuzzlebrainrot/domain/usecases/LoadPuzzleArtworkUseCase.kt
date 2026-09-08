package com.nnastudio.jigsawpuzzlebrainrot.domain.usecases

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleArtwork
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleSource
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.PuzzleImageRepository
import javax.inject.Inject

class LoadPuzzleArtworkUseCase @Inject constructor(
    private val puzzleImageRepository: PuzzleImageRepository
) {
    suspend operator fun invoke(source: PuzzleSource): PuzzleArtwork =
        puzzleImageRepository.loadArtwork(source)
}
