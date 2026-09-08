package com.nnastudio.jigsawpuzzlebrainrot.data.models

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceOffset
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PiecePlacement
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame
import kotlinx.serialization.Serializable

/**
 * DTO cho van dang choi luu trong DataStore (mot chuoi JSON moi van). Tach khoi domain
 * model de doi cau truc state khi choi khong lam ban luu cu doc khong duoc.
 */
@Serializable
data class SavedGameDto(
    val puzzleId: String,
    val difficultyId: String,
    val seed: Long,
    val pieces: List<SavedPieceDto> = emptyList(),
    val trayOrder: List<Int> = emptyList(),
    val moves: Int = 0,
    val elapsedSeconds: Int = 0,
    val hintsLeft: Int = 0
)

@Serializable
data class SavedPieceDto(
    val id: Int,
    val x: Float = 0f,
    val y: Float = 0f,
    val placed: Boolean = false,
    val inTray: Boolean = false,
    /** Khoi ma manh dang thuoc ve; mac dinh la chinh no (dung mot minh). */
    val group: Int = id
)

fun SavedGameDto.toDomain(): SavedGame = SavedGame(
    puzzleId = puzzleId,
    difficulty = Difficulty.fromId(difficultyId),
    seed = seed,
    placements = pieces.map {
        PiecePlacement(
            pieceId = it.id,
            position = PieceOffset(it.x, it.y),
            isPlaced = it.placed,
            isInTray = it.inTray
        )
    },
    groups = pieces.associate { it.id to it.group },
    trayOrder = trayOrder,
    moves = moves,
    elapsedSeconds = elapsedSeconds,
    hintsLeft = hintsLeft
)

fun SavedGame.toDto(): SavedGameDto = SavedGameDto(
    puzzleId = puzzleId,
    difficultyId = difficulty.id,
    seed = seed,
    pieces = placements.map {
        SavedPieceDto(
            id = it.pieceId,
            x = it.position.x,
            y = it.position.y,
            placed = it.isPlaced,
            inTray = it.isInTray,
            group = groups[it.pieceId] ?: it.pieceId
        )
    },
    trayOrder = trayOrder,
    moves = moves,
    elapsedSeconds = elapsedSeconds,
    hintsLeft = hintsLeft
)
