package com.nnastudio.jigsawpuzzlebrainrot.domain.repository

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleProgress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeAllProgress(): Flow<List<PuzzleProgress>>
    fun observeProgress(puzzleId: String, difficulty: Difficulty): Flow<PuzzleProgress>
    suspend fun saveResult(
        puzzleId: String,
        difficulty: Difficulty,
        timeSeconds: Int,
        moves: Int,
        score: Int
    )
}
