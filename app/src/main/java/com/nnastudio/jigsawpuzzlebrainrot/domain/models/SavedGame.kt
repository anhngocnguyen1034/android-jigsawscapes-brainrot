package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

/**
 * Mot van dang choi giua dong, luu lai de nguoi choi vao lai la choi tiep.
 *
 * [seed] la hat giong dung de cat bo manh: cat lai bang dung hat nay thi hinh cac duong cat
 * y nguyen, nho vay chi phai luu vi tri cac manh chu khong phai luu ca bo manh.
 */
@Immutable
data class SavedGame(
    val puzzleId: String,
    val difficulty: Difficulty,
    val seed: Long,
    val placements: List<PiecePlacement>,
    /** pieceId -> groupId: cac manh da khop thanh khoi, xem [PuzzlePlayState.groups]. */
    val groups: Map<Int, Int>,
    val trayOrder: List<Int>,
    val moves: Int,
    val elapsedSeconds: Int,
    val hintsLeft: Int
) {

    /**
     * Dung lai trang thai van tren [puzzle] vua cat lai tu [seed]. Tra ve null khi ban luu
     * khong con khop voi bo manh (doi do kho, doi so manh...) - luc do phai choi van moi.
     *
     * [PuzzlePlayState.bounds] khong duoc luu: no la vung man hinh, do man hinh bao lai
     * ngay sau khi ban co duoc do.
     */
    fun toPlayState(puzzle: JigsawPuzzle): PuzzlePlayState? {
        val byId = placements.associateBy { it.pieceId }
        val pieceIds = puzzle.pieces.map { it.id }.toSet()
        if (byId.keys != pieceIds) return null
        if (trayOrder.toSet() != byId.values.filter { it.isInTray }.map { it.pieceId }.toSet()) {
            return null
        }
        return PuzzlePlayState(
            puzzle = puzzle,
            placements = byId,
            // Manh thieu trong ban luu thi coi nhu dung mot minh mot khoi.
            groups = pieceIds.associateWith { groups[it] ?: it },
            trayOrder = trayOrder,
            moves = moves
        )
    }

    companion object {
        fun of(
            puzzleId: String,
            seed: Long,
            playState: PuzzlePlayState,
            elapsedSeconds: Int,
            hintsLeft: Int
        ) = SavedGame(
            puzzleId = puzzleId,
            difficulty = playState.puzzle.difficulty,
            seed = seed,
            placements = playState.placements.values.toList(),
            groups = playState.groups,
            trayOrder = playState.trayOrder,
            moves = playState.moves,
            elapsedSeconds = elapsedSeconds,
            hintsLeft = hintsLeft
        )
    }
}
