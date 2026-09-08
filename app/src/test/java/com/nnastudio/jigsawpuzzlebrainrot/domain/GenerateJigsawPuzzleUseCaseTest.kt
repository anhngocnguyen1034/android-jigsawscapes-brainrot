package com.nnastudio.jigsawpuzzlebrainrot.domain

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.EdgeType
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GenerateJigsawPuzzleUseCase
import com.nnastudio.jigsawpuzzlebrainrot.fake.FakePuzzleRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.random.Random

class GenerateJigsawPuzzleUseCaseTest {

    private val generatePuzzle = GenerateJigsawPuzzleUseCase()
    private val image = FakePuzzleRepository.defaultPuzzles.first()

    @Test
    fun `should create one piece per cell of the grid`() {
        val puzzle = generatePuzzle(image, Difficulty.HARD, Random(42))

        assertEquals(Difficulty.HARD.pieceCount, puzzle.pieces.size)
        assertEquals(puzzle.pieces.size, puzzle.pieces.map { it.id }.distinct().size)
    }

    @Test
    fun `should keep the outer border flat`() {
        val puzzle = generatePuzzle(image, Difficulty.MEDIUM, Random(7))
        val difficulty = puzzle.difficulty

        puzzle.pieces.forEach { piece ->
            if (piece.row == 0) assertEquals(EdgeType.FLAT, piece.edges.top.type)
            if (piece.col == 0) assertEquals(EdgeType.FLAT, piece.edges.left.type)
            if (piece.row == difficulty.rows - 1) assertEquals(EdgeType.FLAT, piece.edges.bottom.type)
            if (piece.col == difficulty.cols - 1) assertEquals(EdgeType.FLAT, piece.edges.right.type)
        }
    }

    @Test
    fun `should give neighbouring pieces complementary edges`() {
        val puzzle = generatePuzzle(image, Difficulty.EXPERT, Random(11))
        val difficulty = puzzle.difficulty

        for (row in 0 until difficulty.rows) {
            for (col in 0 until difficulty.cols) {
                val piece = puzzle.pieceAt(row, col)
                assertNotNull(piece)
                requireNotNull(piece)

                // Canh chung phai la cung mot duong cong doc nguoc chieu, khong chi
                // nguoc loi/lom: hai manh ke nhau moi khit vao nhau khong ho.
                puzzle.pieceAt(row, col + 1)?.let { right ->
                    assertEquals(piece.edges.right.reversed(), right.edges.left)
                }
                puzzle.pieceAt(row + 1, col)?.let { below ->
                    assertEquals(piece.edges.bottom.reversed(), below.edges.top)
                }
            }
        }
    }

    @Test
    fun `should reverse an edge back to itself`() {
        val puzzle = generatePuzzle(image, Difficulty.MEDIUM, Random(19))

        puzzle.pieces.forEach { piece ->
            assertEquals(piece.edges.top, piece.edges.top.reversed().reversed())
        }
    }

    @Test
    fun `should never leave an inner edge flat`() {
        val puzzle = generatePuzzle(image, Difficulty.MEDIUM, Random(3))
        val difficulty = puzzle.difficulty

        puzzle.pieces.forEach { piece ->
            if (piece.row > 0) assert(piece.edges.top.type != EdgeType.FLAT)
            if (piece.col > 0) assert(piece.edges.left.type != EdgeType.FLAT)
            if (piece.row < difficulty.rows - 1) assert(piece.edges.bottom.type != EdgeType.FLAT)
            if (piece.col < difficulty.cols - 1) assert(piece.edges.right.type != EdgeType.FLAT)
        }
    }
}
