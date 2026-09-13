package com.nnastudio.jigsawpuzzlebrainrot.domain

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ScoreRules
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ShopRules
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GenerateJigsawPuzzleUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.PrepareTrayUseCase
import com.nnastudio.jigsawpuzzlebrainrot.fake.FakePuzzleRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShopRulesTest {

    private val image = FakePuzzleRepository.defaultPuzzles.first()
    private val puzzle = GenerateJigsawPuzzleUseCase().invoke(image, Difficulty.EASY, Random(5))
    private val prepareTray = PrepareTrayUseCase()

    /**
     * Ghep xong ca buc theo cach thang nhat: dua tung manh tu khay vao dung o cua no. Tra ve
     * tong diem an duoc, gom ca thuong ket van - dung nhu [com.nnastudio.jigsawpuzzlebrainrot
     * .presentation.viewmodels.GameViewModel] cong diem.
     */
    private fun playThrough(): Int {
        var state: PuzzlePlayState = prepareTray(puzzle, Random(5))
        var score = 0
        puzzle.pieces.forEach { piece ->
            val next = state.releaseFromTray(piece.id, state.targetOf(piece))
            score += ScoreRules.gain(state, next)
            state = next
        }
        assertTrue(state.isSolved)
        return score + ScoreRules.solvedBonus(Difficulty.EASY)
    }

    @Test
    fun `should value a perfect run exactly as the game pays for it`() {
        assertEquals(ShopRules.perfectRunScore(Difficulty.EASY), playThrough())
    }

    @Test
    fun `should price a locked picture at a whole number of finished runs`() {
        val price = ShopRules.unlockPrice()
        val run = ShopRules.perfectRunScore(Difficulty.DEFAULT)

        // Gia tron boi cua PRICE_STEP va khong re hon so van da dinh - lam tron len nen co
        // the dat hon mot chut, nhung khong duoc dat den muc thanh mot van thua.
        assertEquals(0, price % ShopRules.PRICE_STEP)
        assertTrue(price >= run * ShopRules.RUNS_PER_UNLOCK)
        assertTrue(price < run * (ShopRules.RUNS_PER_UNLOCK + 1))
    }

    @Test
    fun `should cost more to unlock a picture than a single run pays`() {
        assertTrue(ShopRules.unlockPrice() > ShopRules.perfectRunScore(Difficulty.DEFAULT))
    }
}
