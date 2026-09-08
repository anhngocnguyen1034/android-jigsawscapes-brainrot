package com.nnastudio.jigsawpuzzlebrainrot.domain.usecases

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.ProgressRepository
import javax.inject.Inject

class SavePuzzleResultUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    suspend operator fun invoke(
        puzzleId: String,
        difficulty: Difficulty,
        timeSeconds: Int,
        moves: Int
    ) = progressRepository.saveResult(puzzleId, difficulty, timeSeconds, moves)
}
