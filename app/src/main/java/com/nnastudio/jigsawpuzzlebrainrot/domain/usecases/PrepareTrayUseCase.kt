package com.nnastudio.jigsawpuzzlebrainrot.domain.usecases

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPuzzle
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceOffset
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PiecePlacement
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import javax.inject.Inject
import kotlin.random.Random

/**
 * Bat dau van moi: moi manh nam trong khay cuon ngang ben duoi, thu tu duoc tron de nguoi
 * choi khong doan duoc vi tri. Manh chi nhan toa do tren ban co khi duoc keo hoac chon ra.
 */
class PrepareTrayUseCase @Inject constructor() {

    operator fun invoke(
        puzzle: JigsawPuzzle,
        random: Random = Random.Default
    ): PuzzlePlayState {
        val trayOrder = puzzle.pieces.shuffled(random).map { it.id }
        val placements = puzzle.pieces.associate { piece ->
            piece.id to PiecePlacement(
                pieceId = piece.id,
                position = PieceOffset(0f, 0f),
                isInTray = true
            )
        }

        return PuzzlePlayState(
            puzzle = puzzle,
            placements = placements,
            trayOrder = trayOrder
        )
    }
}
