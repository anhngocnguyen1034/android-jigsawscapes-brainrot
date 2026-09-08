package com.nnastudio.jigsawpuzzlebrainrot.domain.usecases

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.PuzzleRepository
import javax.inject.Inject

class GetPuzzleDetailUseCase @Inject constructor(
    private val puzzleRepository: PuzzleRepository
) {
    suspend operator fun invoke(puzzleId: String): PuzzleImage? =
        puzzleRepository.getPuzzleById(puzzleId)
}
