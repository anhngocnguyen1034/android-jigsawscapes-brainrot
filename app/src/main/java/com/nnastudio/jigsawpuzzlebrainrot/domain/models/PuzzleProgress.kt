package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

/** Ket qua tot nhat cua nguoi choi cho mot cap (anh, do kho). */
@Immutable
data class PuzzleProgress(
    val puzzleId: String,
    val difficulty: Difficulty,
    val bestTimeSeconds: Int,
    val bestMoves: Int,
    /** Diem cao nhat dat duoc o buc nay - man hinh chinh hien ngay tren the anh. */
    val bestScore: Int = 0,
    val completed: Boolean
) {
    companion object {
        fun empty(puzzleId: String, difficulty: Difficulty) = PuzzleProgress(
            puzzleId = puzzleId,
            difficulty = difficulty,
            bestTimeSeconds = 0,
            bestMoves = 0,
            bestScore = 0,
            completed = false
        )
    }
}
