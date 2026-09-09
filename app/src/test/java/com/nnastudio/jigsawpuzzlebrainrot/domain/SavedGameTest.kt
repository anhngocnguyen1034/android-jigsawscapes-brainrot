package com.nnastudio.jigsawpuzzlebrainrot.domain

import com.nnastudio.jigsawpuzzlebrainrot.data.models.SavedGameDto
import com.nnastudio.jigsawpuzzlebrainrot.data.models.toDomain
import com.nnastudio.jigsawpuzzlebrainrot.data.models.toDto
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceOffset
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GenerateJigsawPuzzleUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.PrepareTrayUseCase
import com.nnastudio.jigsawpuzzlebrainrot.fake.FakePuzzleRepository
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class SavedGameTest {

    private val image = FakePuzzleRepository.defaultPuzzles.first()
    private val generate = GenerateJigsawPuzzleUseCase()
    private val prepareTray = PrepareTrayUseCase()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Cat lai bang cung hat giong phai ra dung bo manh cu: ban luu chi giu hat giong. */
    @Test
    fun `same seed cuts the same pieces`() {
        val first = generate(image, Difficulty.EASY, Random(SEED))
        val second = generate(image, Difficulty.EASY, Random(SEED))

        assertEquals(first.pieces, second.pieces)
    }

    /** Van dang choi qua mot vong luu - doc phai giu nguyen vi tri manh, khoi, gio, goi y. */
    @Test
    fun `saved game survives a save and load round trip`() {
        val puzzle = generate(image, Difficulty.EASY, Random(SEED))
        val playing = prepareTray(puzzle, Random(SEED))
            .releaseFromTray(0, PieceOffset(0.1f, 0.2f))
        val saved = SavedGame.of(
            puzzleId = image.id,
            seed = SEED,
            playState = playing,
            elapsedSeconds = 42,
            hintsLeft = 2,
            score = 130
        )

        val raw = json.encodeToString(SavedGameDto.serializer(), saved.toDto())
        val restored = json.decodeFromString<SavedGameDto>(raw).toDomain()
        val state = restored.toPlayState(generate(image, Difficulty.EASY, Random(SEED)))

        assertNotNull(state)
        assertEquals(playing.placements, state!!.placements)
        assertEquals(playing.groups, state.groups)
        assertEquals(playing.trayOrder, state.trayOrder)
        assertEquals(playing.moves, state.moves)
        assertEquals(42, restored.elapsedSeconds)
        assertEquals(2, restored.hintsLeft)
        assertEquals(130, restored.score)
    }

    /** Ban luu cua do kho khac khong dung vao bo manh nay: phai bi bo, khong duoc dung buoc. */
    @Test
    fun `saved game from another difficulty is rejected`() {
        val easy = generate(image, Difficulty.EASY, Random(SEED))
        val saved = SavedGame.of(
            puzzleId = image.id,
            seed = SEED,
            playState = prepareTray(easy, Random(SEED)),
            elapsedSeconds = 0,
            hintsLeft = 3,
            score = 0
        )

        val hard = generate(image, Difficulty.HARD, Random(SEED))

        assertNull(saved.toPlayState(hard))
    }

    private companion object {
        const val SEED = 7L
    }
}
