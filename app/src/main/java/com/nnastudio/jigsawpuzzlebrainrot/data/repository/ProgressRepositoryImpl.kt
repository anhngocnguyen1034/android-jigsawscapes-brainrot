package com.nnastudio.jigsawpuzzlebrainrot.data.repository

import com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local.ProgressDataSource
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleProgress
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val progressDataSource: ProgressDataSource
) : ProgressRepository {

    override fun observeAllProgress(): Flow<List<PuzzleProgress>> =
        progressDataSource.allProgress.catch { emit(emptyList()) }

    override fun observeProgress(puzzleId: String, difficulty: Difficulty): Flow<PuzzleProgress> =
        progressDataSource.progressOf(puzzleId, difficulty)
            .catch { emit(PuzzleProgress.empty(puzzleId, difficulty)) }

    override suspend fun saveResult(
        puzzleId: String,
        difficulty: Difficulty,
        timeSeconds: Int,
        moves: Int,
        score: Int
    ) {
        runCatching {
            progressDataSource.saveResult(puzzleId, difficulty, timeSeconds, moves, score)
        }
    }
}
