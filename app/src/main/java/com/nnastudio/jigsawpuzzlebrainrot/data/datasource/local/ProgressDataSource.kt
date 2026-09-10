package com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Luu ket qua choi vao DataStore theo key "<puzzleId>|<difficulty>".
 * Khong co backend nen day la nguon su that duy nhat cho tien do.
 */
@Singleton
class ProgressDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val allProgress: Flow<List<PuzzleProgress>> = dataStore.data.map { prefs ->
        prefs.asMap().keys
            .map { it.name }
            .filter { it.startsWith(TIME_PREFIX) }
            .mapNotNull { key -> key.removePrefix(TIME_PREFIX).toProgress(prefs) }
    }

    fun progressOf(puzzleId: String, difficulty: Difficulty): Flow<PuzzleProgress> =
        dataStore.data.map { prefs ->
            entryKey(puzzleId, difficulty).toProgress(prefs)
                ?: PuzzleProgress.empty(puzzleId, difficulty)
        }

    suspend fun saveResult(
        puzzleId: String,
        difficulty: Difficulty,
        timeSeconds: Int,
        moves: Int,
        score: Int
    ) {
        val entry = entryKey(puzzleId, difficulty)
        dataStore.edit { prefs ->
            val bestTime = prefs[timeKey(entry)] ?: Int.MAX_VALUE
            val bestMoves = prefs[movesKey(entry)] ?: Int.MAX_VALUE
            val bestScore = prefs[scoreKey(entry)] ?: 0
            prefs[timeKey(entry)] = minOf(bestTime, timeSeconds)
            prefs[movesKey(entry)] = minOf(bestMoves, moves)
            prefs[scoreKey(entry)] = maxOf(bestScore, score)
        }
    }

    private fun String.toProgress(prefs: Preferences): PuzzleProgress? {
        val (puzzleId, difficultyId) = split(SEPARATOR).takeIf { it.size == 2 } ?: return null
        val time = prefs[timeKey(this)] ?: return null
        return PuzzleProgress(
            puzzleId = puzzleId,
            difficulty = Difficulty.fromId(difficultyId),
            bestTimeSeconds = time,
            bestMoves = prefs[movesKey(this)] ?: 0,
            bestScore = prefs[scoreKey(this)] ?: 0,
            completed = true
        )
    }

    private companion object {
        const val SEPARATOR = "|"
        const val TIME_PREFIX = "best_time_"
        const val MOVES_PREFIX = "best_moves_"
        const val SCORE_PREFIX = "best_score_"

        fun entryKey(puzzleId: String, difficulty: Difficulty) = "$puzzleId$SEPARATOR${difficulty.id}"
        fun timeKey(entry: String) = intPreferencesKey("$TIME_PREFIX$entry")
        fun movesKey(entry: String) = intPreferencesKey("$MOVES_PREFIX$entry")
        fun scoreKey(entry: String) = intPreferencesKey("$SCORE_PREFIX$entry")
    }
}
