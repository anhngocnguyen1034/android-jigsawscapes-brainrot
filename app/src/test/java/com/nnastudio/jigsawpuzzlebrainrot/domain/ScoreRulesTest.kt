package com.nnastudio.jigsawpuzzlebrainrot.domain

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceOffset
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ScoreRules
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GenerateJigsawPuzzleUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.PrepareTrayUseCase
import com.nnastudio.jigsawpuzzlebrainrot.fake.FakePuzzleRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ScoreRulesTest {

    private val image = FakePuzzleRepository.defaultPuzzles.first()
    private val puzzle = GenerateJigsawPuzzleUseCase().invoke(image, Difficulty.EASY, Random(5))
    private val prepareTray = PrepareTrayUseCase()

    private fun newTrayState(): PuzzlePlayState = prepareTray(puzzle, Random(5))

    @Test
    fun `should pay a bigger bonus for pictures with more pieces`() {
        val small = ScoreRules.solvedBonus(Difficulty.MASTER)
        val big = ScoreRules.solvedBonus(Difficulty.LEGEND)

        assertEquals(
            ScoreRules.POINTS_SOLVED + ScoreRules.POINTS_PER_PIECE_SOLVED * 64,
            small
        )
        assertEquals(
            ScoreRules.POINTS_SOLVED + ScoreRules.POINTS_PER_PIECE_SOLVED * 400,
            big
        )
        assertTrue(big > small)
    }

    @Test
    fun `should give no points for moving a piece without fitting it`() {
        val state = newTrayState()
        // Manh giua canh: manh goc thi cho nay con nam trong vung hut rong cua goc khung.
        val piece = puzzle.pieces.first { it.row == 0 && it.col == 1 }

        // Cho nay cach xa o dung cua manh va xa moi manh ke ben: chua ghep vao dau.
        val released = state.releaseFromTray(piece.id, PieceOffset(0.15f, 0.15f))

        assertEquals(0, ScoreRules.gain(state, released))
    }

    @Test
    fun `should give points for a piece dropped into its own slot`() {
        val state = newTrayState()
        val piece = puzzle.pieces.first()
        val target = state.targetOf(piece)

        val released = state.releaseFromTray(
            piece.id,
            PieceOffset(target.x + 0.02f, target.y + 0.02f)
        )

        assertEquals(ScoreRules.POINTS_PER_PLACED, ScoreRules.gain(state, released))
    }

    @Test
    fun `should give points for joining two loose pieces`() {
        val first = puzzle.pieces.first { it.row == 0 && it.col == 1 }
        val second = puzzle.pieces.first { it.row == 1 && it.col == 1 }
        // Hai manh giua ban co: khong manh nao dung o cua no nen chi an diem noi khoi.
        val spot = PieceOffset(0.28f, 0.28f)
        val state = newTrayState().releaseFromTray(first.id, spot)
        val target = state.targetOf(first)
        val other = state.targetOf(second)

        val joined = state.releaseFromTray(
            second.id,
            PieceOffset(spot.x + other.x - target.x, spot.y + other.y - target.y)
        )

        assertEquals(1, joined.joinCount)
        assertEquals(ScoreRules.POINTS_PER_JOIN, ScoreRules.gain(state, joined))
    }

    @Test
    fun `should not take points back when a piece is pulled out again`() {
        val state = newTrayState()
        val piece = puzzle.pieces.first()
        val target = state.targetOf(piece)
        val placed = state.releaseFromTray(
            piece.id,
            PieceOffset(target.x + 0.02f, target.y + 0.02f)
        )

        assertEquals(0, ScoreRules.gain(placed, state))
    }
}
