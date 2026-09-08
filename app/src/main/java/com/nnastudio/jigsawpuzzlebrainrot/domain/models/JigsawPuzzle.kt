package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

/** Bo manh ghep da duoc cat tu mot buc anh. */
@Immutable
data class JigsawPuzzle(
    val image: PuzzleImage,
    val difficulty: Difficulty,
    val pieces: List<JigsawPiece>
) {
    fun pieceAt(row: Int, col: Int): JigsawPiece? =
        pieces.firstOrNull { it.row == row && it.col == col }
}
