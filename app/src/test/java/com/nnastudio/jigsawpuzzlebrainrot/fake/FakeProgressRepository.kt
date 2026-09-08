package com.nnastudio.jigsawpuzzlebrainrot.fake

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleProgress
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeProgressRepository : ProgressRepository {

    private val entries = MutableStateFlow<List<PuzzleProgress>>(emptyList())

    override fun observeAllProgress(): Flow<List<PuzzleProgress>> = entries

    override fun observeProgress(puzzleId: String, difficulty: Difficulty): Flow<PuzzleProgress> =
        entries.map { list ->
            list.firstOrNull { it.puzzleId == puzzleId && it.difficulty == difficulty }
                ?: PuzzleProgress.empty(puzzleId, difficulty)
        }

    override suspend fun saveResult(
        puzzleId: String,
        difficulty: Difficulty,
        timeSeconds: Int,
        moves: Int
    ) {
        val existing = entries.value.firstOrNull {
            it.puzzleId == puzzleId && it.difficulty == difficulty
        }
        val updated = PuzzleProgress(
            puzzleId = puzzleId,
            difficulty = difficulty,
            bestTimeSeconds = minOf(existing?.bestTimeSeconds ?: Int.MAX_VALUE, timeSeconds),
            bestMoves = minOf(existing?.bestMoves ?: Int.MAX_VALUE, moves),
            completed = true
        )
        entries.value = entries.value.filterNot { it == existing } + updated
    }
}
