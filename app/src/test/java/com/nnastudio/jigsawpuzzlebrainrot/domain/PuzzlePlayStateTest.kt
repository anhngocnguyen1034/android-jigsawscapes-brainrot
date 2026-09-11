package com.nnastudio.jigsawpuzzlebrainrot.domain

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPiece
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceBounds
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceOffset
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GenerateJigsawPuzzleUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.PrepareTrayUseCase
import com.nnastudio.jigsawpuzzlebrainrot.fake.FakePuzzleRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PuzzlePlayStateTest {

    private val image = FakePuzzleRepository.defaultPuzzles.first()
    private val puzzle = GenerateJigsawPuzzleUseCase().invoke(image, Difficulty.EASY, Random(5))
    private val prepareTray = PrepareTrayUseCase()

    /** Van moi: moi manh nam trong khay. */
    private fun newTrayState(): PuzzlePlayState = prepareTray(puzzle, Random(5))

    /**
     * Van da dua het manh ra ban co de kiem tra phan keo/hut tren ban co. Moi manh nam
     * chong nhau tai [LOOSE_SPOT]: cach xa o dung cua minh va cach xa tuong quan cua moi
     * cap manh ke ben, nen chua co manh nao dinh vao dau.
     */
    private fun newBoardState(): PuzzlePlayState {
        val tray = newTrayState()
        return tray.copy(
            placements = tray.placements.mapValues { (_, placement) ->
                placement.copy(position = LOOSE_SPOT, isInTray = false)
            },
            trayOrder = emptyList()
        )
    }

    /** Doan can keo de [piece] dung ngay canh o cua no, con lech [gap] tren truc x. */
    private fun offsetTo(state: PuzzlePlayState, piece: JigsawPiece, gap: Float): PieceOffset {
        val current = state.placements.getValue(piece.id).position
        val target = state.targetOf(piece)
        return PieceOffset(target.x - current.x + gap, target.y - current.y)
    }

    private companion object {
        const val TOLERANCE = 1e-4f

        /** Cho dat manh roi: lech 0.15 so voi moi o (luoi 3x3 nen nguong hut la 0.06). */
        val LOOSE_SPOT = PieceOffset(0.15f, 0.15f)
    }

    @Test
    fun `should start with every piece in the tray`() {
        val state = newTrayState()

        assertEquals(puzzle.pieces.size, state.placements.size)
        assertEquals(puzzle.pieces.size, state.trayOrder.size)
        assertTrue(state.placements.values.all { it.isInTray })
        assertFalse(state.isSolved)
    }

    @Test
    fun `should ignore drags on a piece still in the tray`() {
        val state = newTrayState()
        val piece = puzzle.pieces.first()

        assertEquals(state, state.movePiece(piece.id, 0.2f, 0.2f))
        assertEquals(state, state.dropPiece(piece.id))
    }

    @Test
    fun `should leave the tray once a piece is released onto the board`() {
        val state = newTrayState()
        val piece = puzzle.pieces.first()

        val released = state.releaseFromTray(piece.id, PieceOffset(0.3f, 0.4f))

        val placement = released.placements.getValue(piece.id)
        assertFalse(placement.isInTray)
        assertFalse(released.trayOrder.contains(piece.id))
        assertEquals(0.3f, placement.position.x, TOLERANCE)
        assertEquals(0.4f, placement.position.y, TOLERANCE)
    }

    @Test
    fun `should snap a piece released from the tray near its target`() {
        val state = newTrayState()
        val piece = puzzle.pieces.first()
        val target = state.targetOf(piece)

        val released = state.releaseFromTray(
            piece.id,
            PieceOffset(target.x + 0.02f, target.y + 0.02f)
        )

        val placement = released.placements.getValue(piece.id)
        assertTrue(placement.isPlaced)
        assertEquals(target, placement.position)
    }

    @Test
    fun `should let a loose piece move outside the board frame`() {
        // Man hinh rong hon ban co: manh roi duoc ra ca hai ben ngoai khung.
        val bounds = PieceBounds(minX = -0.2f, minY = -0.1f, maxX = 1.2f, maxY = 1.1f)
        val state = newBoardState().withBounds(bounds)
        val piece = puzzle.pieces.first()

        val moved = state.movePiece(piece.id, -0.5f, -0.5f)

        val position = moved.placements.getValue(piece.id).position
        assertEquals(bounds.minX, position.x, TOLERANCE)
        assertEquals(bounds.minY, position.y, TOLERANCE)
    }

    @Test
    fun `should keep a loose piece inside the play area`() {
        val bounds = PieceBounds(minX = -0.2f, minY = -0.1f, maxX = 1.2f, maxY = 1.1f)
        val state = newBoardState().withBounds(bounds)
        val piece = puzzle.pieces.first()

        val moved = state.movePiece(piece.id, 5f, 5f)

        val position = moved.placements.getValue(piece.id).position
        assertEquals(bounds.maxX, position.x, TOLERANCE)
        assertEquals(bounds.maxY, position.y, TOLERANCE)
    }

    @Test
    fun `should return a loose piece to any slot in the tray`() {
        var state = newTrayState()
        val piece = puzzle.pieces.first { it.row == 0 && it.col == 1 }
        state = state.releaseFromTray(piece.id, LOOSE_SPOT)
        val trayBefore = state.trayOrder

        state = state.returnToTray(piece.id, index = 2)

        assertTrue(state.isInTray(piece.id))
        assertEquals(piece.id, state.trayOrder[2])
        assertEquals(trayBefore.size + 1, state.trayOrder.size)
        // Thu tu cac manh con lai khong doi.
        assertEquals(trayBefore, state.trayOrder - piece.id)
    }

    @Test
    fun `should move a piece to the dropped slot inside the tray`() {
        val state = newTrayState()
        val moved = state.trayOrder[1]

        // Cho chen tinh theo danh sach con nguyen manh dang keo: 4 = ngay sau manh thu 3.
        val reordered = state.moveInTray(moved, index = 4)

        assertEquals(moved, reordered.trayOrder[3])
        assertEquals(state.trayOrder.size, reordered.trayOrder.size)
        // Thu tu cac manh con lai khong doi.
        assertEquals(state.trayOrder - moved, reordered.trayOrder - moved)
    }

    @Test
    fun `should keep a piece where it is when dropped on its own slot in the tray`() {
        val state = newTrayState()
        val moved = state.trayOrder[2]

        assertEquals(state, state.moveInTray(moved, index = 2))
        assertEquals(state, state.moveInTray(moved, index = 3))
    }

    @Test
    fun `should not reorder the tray with a piece that is on the board`() {
        var state = newTrayState()
        val piece = puzzle.pieces.first()
        state = state.releaseFromTray(piece.id, LOOSE_SPOT)

        assertEquals(state, state.moveInTray(piece.id, index = 0))
    }

    @Test
    fun `should keep a joined group on the board instead of returning it to the tray`() {
        val state = newBoardState()
        val anchor = puzzle.pieceAt(0, 0)!!
        val neighbour = puzzle.pieceAt(0, 1)!!
        val joined = state
            .movePiece(neighbour.id, 1f / puzzle.difficulty.cols, 0f)
            .dropPiece(neighbour.id)
        assertEquals(joined.groupOf(anchor.id), joined.groupOf(neighbour.id))

        assertFalse(joined.canReturnToTray(anchor.id))
        assertEquals(joined, joined.returnToTray(anchor.id, index = 0))
    }

    @Test
    fun `should not return a locked piece to the tray`() {
        val piece = puzzle.pieces.first()
        val locked = newBoardState()
            .movePiece(piece.id, -LOOSE_SPOT.x, -LOOSE_SPOT.y)
            .dropPiece(piece.id)

        assertFalse(locked.canReturnToTray(piece.id))
        assertEquals(locked, locked.returnToTray(piece.id, index = 0))
    }

    @Test
    fun `should list every loose piece on the board in grid order`() {
        val state = newBoardState()

        assertEquals(puzzle.pieces.map { it.id }, state.loosePieces.map { it.id })
    }

    @Test
    fun `should leave joined and locked pieces out of the loose list`() {
        val anchor = puzzle.pieceAt(0, 0)!!
        val neighbour = puzzle.pieceAt(0, 1)!!
        val locked = puzzle.pieceAt(2, 2)!!
        val board = newBoardState()
        val target = board.targetOf(locked)
        val state = board
            // Hai manh ke ben dinh vao nhau thanh khoi, mot manh khac vao dung o cua no.
            .movePiece(neighbour.id, 1f / puzzle.difficulty.cols, 0f)
            .dropPiece(neighbour.id)
            .movePiece(locked.id, target.x - LOOSE_SPOT.x, target.y - LOOSE_SPOT.y)
            .dropPiece(locked.id)

        val loose = state.loosePieces.map { it.id }

        assertFalse(loose.contains(anchor.id))
        assertFalse(loose.contains(neighbour.id))
        assertFalse(loose.contains(locked.id))
        assertEquals(puzzle.pieces.size - 3, loose.size)
    }

    @Test
    fun `should clean every loose piece to the end of the tray`() {
        var state = newTrayState()
        val cleaned = listOf(puzzle.pieceAt(1, 1)!!, puzzle.pieceAt(2, 0)!!)
        cleaned.forEach { state = state.releaseFromTray(it.id, LOOSE_SPOT) }
        val trayBefore = state.trayOrder

        // Nut don: manh nao bay den khay thi duoc chen vao cuoi danh sach.
        state.loosePieces.forEach { piece ->
            state = state.returnToTray(piece.id, state.trayOrder.size)
        }

        assertTrue(state.loosePieces.isEmpty())
        assertEquals(trayBefore + cleaned.map { it.id }, state.trayOrder)
    }

    @Test
    fun `should shrink the snap threshold as the pieces get smaller`() {
        val easy = newBoardState()
        val master = prepareTray(
            GenerateJigsawPuzzleUseCase().invoke(image, Difficulty.MASTER, Random(5)),
            Random(5)
        )

        // Nguong luon la 18% be rong mot manh, nen luoi cang day nguong cang chat.
        assertEquals(0.18f / 3f, easy.snapThreshold, TOLERANCE)
        assertEquals(0.18f / 8f, master.snapThreshold, TOLERANCE)
    }

    @Test
    fun `should snap and lock a piece dropped near its target`() {
        val state = newBoardState()
        val piece = puzzle.pieces.first()
        val target = state.targetOf(piece)
        val current = state.placements.getValue(piece.id).position

        val dropped = state
            .movePiece(piece.id, target.x - current.x + 0.02f, target.y - current.y - 0.02f)
            .dropPiece(piece.id)

        val placement = dropped.placements.getValue(piece.id)
        assertTrue(placement.isPlaced)
        assertEquals(target, placement.position)
    }

    @Test
    fun `should leave a piece dropped far from its target untouched`() {
        val state = newBoardState()
        val piece = puzzle.pieces.first()
        val moved = state.movePiece(piece.id, 0.4f, 0.4f)

        val dropped = moved.dropPiece(piece.id)

        assertFalse(dropped.placements.getValue(piece.id).isPlaced)
        assertEquals(moved.placements.getValue(piece.id).position, dropped.placements.getValue(piece.id).position)
    }

    @Test
    fun `should pull an edge piece into its slot while it is still being dragged`() {
        val state = newBoardState()
        // Manh giua canh tren: van la manh vien du khong phai manh goc.
        val piece = puzzle.pieces.first { it.row == 0 && it.col == 1 }
        val target = state.targetOf(piece)
        val current = state.placements.getValue(piece.id).position
        val dx = target.x - current.x + 0.02f
        val dy = target.y - current.y - 0.02f

        val snap = state.dragSnapOffset(piece.id, dx, dy)

        // Doan hut cong voi doan dang keo phai dua manh dung vao o cua no.
        assertEquals(-0.02f, snap!!.x, TOLERANCE)
        assertEquals(0.02f, snap.y, TOLERANCE)
    }

    @Test
    fun `should lock an edge piece as soon as it snaps in mid drag`() {
        val state = newBoardState()
        val piece = puzzle.pieces.first { it.row == 0 && it.col == 1 }
        val target = state.targetOf(piece)
        val current = state.placements.getValue(piece.id).position

        val snapped = state.snapWhileDragging(
            piece.id,
            target.x - current.x + 0.02f,
            target.y - current.y - 0.02f
        )!!

        val placement = snapped.placements.getValue(piece.id)
        assertTrue(placement.isPlaced)
        assertEquals(target.x, placement.position.x, TOLERANCE)
        assertEquals(target.y, placement.position.y, TOLERANCE)
        assertEquals(state.moves + 1, snapped.moves)
        // Da khoa thi keo tiep khong lam no nhuc nhich.
        assertEquals(
            placement.position,
            snapped.movePiece(piece.id, 0.2f, 0.2f).placements.getValue(piece.id).position
        )
    }

    @Test
    fun `should not lock an inner piece mid drag`() {
        val state = newBoardState()
        val piece = puzzle.pieces.first { it.row == 1 && it.col == 1 }
        val target = state.targetOf(piece)
        val current = state.placements.getValue(piece.id).position

        assertNull(
            state.snapWhileDragging(
                piece.id,
                target.x - current.x + 0.02f,
                target.y - current.y - 0.02f
            )
        )
    }

    @Test
    fun `should not pull an inner piece into its slot while it is being dragged`() {
        val state = newBoardState()
        val piece = puzzle.pieces.first { it.row == 1 && it.col == 1 }
        val target = state.targetOf(piece)
        val current = state.placements.getValue(piece.id).position

        val snap = state.dragSnapOffset(
            piece.id,
            target.x - current.x + 0.02f,
            target.y - current.y - 0.02f
        )

        assertNull(snap)
    }

    @Test
    fun `should not pull an edge piece that is still far from its slot`() {
        val state = newBoardState()
        val piece = puzzle.pieces.first { it.row == 0 && it.col == 1 }

        assertNull(state.dragSnapOffset(piece.id, 0.4f, 0.4f))
    }

    @Test
    fun `should pull an edge piece into its slot while it is dragged out of the tray`() {
        val state = newTrayState()
        val piece = puzzle.pieces.first { it.row == 0 && it.col == 1 }
        val target = state.targetOf(piece)

        val snap = state.trayDragSnapOffset(
            piece.id,
            PieceOffset(target.x + 0.02f, target.y - 0.02f)
        )

        assertEquals(-0.02f, snap!!.x, TOLERANCE)
        assertEquals(0.02f, snap.y, TOLERANCE)
        // Dat manh vao dung cho vua hut = manh khoa luon, nhu khi tha tren ban co.
        val placement = state
            .releaseFromTray(piece.id, PieceOffset(target.x + 0.02f, target.y - 0.02f) + snap)
            .placements
            .getValue(piece.id)
        assertTrue(placement.isPlaced)
        assertEquals(target.x, placement.position.x, TOLERANCE)
        assertEquals(target.y, placement.position.y, TOLERANCE)
    }

    @Test
    fun `should pull a corner piece out of the tray from farther than an edge piece`() {
        val state = newTrayState()
        val corner = puzzle.pieces.first { it.row == 0 && it.col == 0 }
        val edge = puzzle.pieces.first { it.row == 0 && it.col == 1 }
        // Luoi 3x3: nguong thuong la 0.06, nguong cua manh goc la 0.083.
        val gap = 0.07f

        val edgeSpot = state.targetOf(edge).let { PieceOffset(it.x + gap, it.y) }
        val cornerSpot = state.targetOf(corner).let { PieceOffset(it.x + gap, it.y) }

        assertNull(state.trayDragSnapOffset(edge.id, edgeSpot))
        assertEquals(-gap, state.trayDragSnapOffset(corner.id, cornerSpot)!!.x, TOLERANCE)
    }

    @Test
    fun `should not pull an inner piece while it is dragged out of the tray`() {
        val state = newTrayState()
        val piece = puzzle.pieces.first { it.row == 1 && it.col == 1 }
        val target = state.targetOf(piece)

        assertNull(
            state.trayDragSnapOffset(piece.id, PieceOffset(target.x + 0.02f, target.y - 0.02f))
        )
    }

    @Test
    fun `should not pull an edge piece out of the tray while it is far from its slot`() {
        val state = newTrayState()
        val piece = puzzle.pieces.first { it.row == 0 && it.col == 1 }
        val target = state.targetOf(piece)

        assertNull(
            state.trayDragSnapOffset(piece.id, PieceOffset(target.x + 0.4f, target.y + 0.4f))
        )
    }

    @Test
    fun `should pull a corner piece into its slot from farther than an edge piece`() {
        val state = newBoardState()
        val corner = puzzle.pieces.first { it.row == 0 && it.col == 0 }
        val edge = puzzle.pieces.first { it.row == 0 && it.col == 1 }
        // Luoi 3x3: nguong thuong la 0.06, nguong cua manh goc la 0.083.
        val gap = 0.07f

        val edgeDrag = offsetTo(state, edge, gap)
        val cornerDrag = offsetTo(state, corner, gap)

        assertNull(state.dragSnapOffset(edge.id, edgeDrag.x, edgeDrag.y))
        val snap = state.dragSnapOffset(corner.id, cornerDrag.x, cornerDrag.y)

        assertEquals(-gap, snap!!.x, TOLERANCE)
        assertEquals(0f, snap.y, TOLERANCE)
    }

    @Test
    fun `should leave a corner piece loose when it is still far from the frame corner`() {
        val state = newBoardState()
        val corner = puzzle.pieces.first { it.row == 0 && it.col == 0 }
        // Nguong cua manh goc chi noi hon manh thuong mot chut: keo ngang qua goc khung ma
        // con cach nua manh thi manh khong tu nhay vao.
        val gap = 0.15f

        assertNull(offsetTo(state, corner, gap).let { state.dragSnapOffset(corner.id, it.x, it.y) })
    }

    @Test
    fun `should put a corner piece in its slot instead of gluing it to a loose neighbour`() {
        val state = newBoardState()
        val corner = puzzle.pieces.first { it.row == 0 && it.col == 0 }
        val neighbour = puzzle.pieces.first { it.row == 0 && it.col == 1 }
        val target = state.targetOf(corner)
        // Tha manh goc dung vao cho ma tuong quan hai manh doi hoi (manh ke ben con roi),
        // nhung cho do van nam trong vung hut cua goc khung: goc khung phai thang.
        val neighbourTarget = state.targetOf(neighbour)
        val loose = PieceOffset(neighbourTarget.x + 0.05f, neighbourTarget.y + 0.05f)
        val glued = PieceOffset(loose.x - 1f / puzzle.difficulty.cols, loose.y)
        val staged = state
            .withBounds(PieceBounds(minX = -1f, minY = -1f, maxX = 2f, maxY = 2f))
            .let {
                it.copy(
                    placements = it.placements +
                        (neighbour.id to it.placements.getValue(neighbour.id).copy(position = loose)) +
                        (corner.id to it.placements.getValue(corner.id).copy(position = glued))
                )
            }

        val dropped = staged.dropPiece(corner.id)

        val placement = dropped.placements.getValue(corner.id)
        assertTrue(placement.isPlaced)
        assertEquals(target.x, placement.position.x, TOLERANCE)
        assertEquals(target.y, placement.position.y, TOLERANCE)
        // Manh ke ben con roi thi khong bi keo theo vao khung.
        assertFalse(dropped.placements.getValue(neighbour.id).isPlaced)
    }

    @Test
    fun `should snap a corner piece into its slot when the finger is lifted`() {
        val state = newBoardState()
        val corner = puzzle.pieces.first { it.row == 0 && it.col == 0 }
        val target = state.targetOf(corner)

        val drag = offsetTo(state, corner, 0.07f)
        val dropped = state
            .movePiece(corner.id, drag.x, drag.y)
            .dropPiece(corner.id)

        val placement = dropped.placements.getValue(corner.id)
        assertTrue(placement.isPlaced)
        assertEquals(target.x, placement.position.x, TOLERANCE)
        assertEquals(target.y, placement.position.y, TOLERANCE)
    }

    @Test
    fun `should ignore drags on a piece that is already locked`() {
        val state = newBoardState()
        val piece = puzzle.pieces.first()
        val target = state.targetOf(piece)
        val current = state.placements.getValue(piece.id).position
        val locked = state
            .movePiece(piece.id, target.x - current.x, target.y - current.y)
            .dropPiece(piece.id)

        val afterDrag = locked.movePiece(piece.id, 0.3f, 0.3f)

        assertEquals(target, afterDrag.placements.getValue(piece.id).position)
    }

    @Test
    fun `should snap a piece onto its neighbour away from the board`() {
        val state = newBoardState()
        val anchor = puzzle.pieceAt(0, 0)!!
        val neighbour = puzzle.pieceAt(0, 1)!!
        // Dat manh ke ben lech mot chut so voi tuong quan dung - van trong nguong hut.
        val anchorPosition = state.placements.getValue(anchor.id).position
        val expectedX = anchorPosition.x + 1f / puzzle.difficulty.cols
        val current = state.placements.getValue(neighbour.id).position

        val dropped = state
            .movePiece(
                neighbour.id,
                expectedX - current.x + 0.02f,
                anchorPosition.y - current.y - 0.02f
            )
            .dropPiece(neighbour.id)

        val snapped = dropped.placements.getValue(neighbour.id).position
        assertEquals(expectedX, snapped.x, TOLERANCE)
        assertEquals(anchorPosition.y, snapped.y, TOLERANCE)
        // Khop voi nhau thi thanh mot khoi, nhung chua nam tren ban co nen chua khoa.
        assertEquals(dropped.groupOf(anchor.id), dropped.groupOf(neighbour.id))
        assertFalse(dropped.placements.getValue(neighbour.id).isPlaced)
        assertFalse(dropped.isSolved)
    }

    @Test
    fun `should drag a whole group of snapped pieces together`() {
        val state = newBoardState()
        val anchor = puzzle.pieceAt(0, 0)!!
        val neighbour = puzzle.pieceAt(0, 1)!!
        val anchorPosition = state.placements.getValue(anchor.id).position
        val current = state.placements.getValue(neighbour.id).position
        val joined = state
            .movePiece(
                neighbour.id,
                anchorPosition.x + 1f / puzzle.difficulty.cols - current.x,
                anchorPosition.y - current.y
            )
            .dropPiece(neighbour.id)

        val moved = joined.movePiece(anchor.id, 0.1f, 0.2f)

        val movedAnchor = moved.placements.getValue(anchor.id).position
        val movedNeighbour = moved.placements.getValue(neighbour.id).position
        assertEquals(anchorPosition.x + 0.1f, movedAnchor.x, TOLERANCE)
        assertEquals(anchorPosition.y + 0.2f, movedAnchor.y, TOLERANCE)
        // Khoang cach giua hai manh khong doi -> ca khoi di cung nhau.
        assertEquals(1f / puzzle.difficulty.cols, movedNeighbour.x - movedAnchor.x, TOLERANCE)
        assertEquals(0f, movedNeighbour.y - movedAnchor.y, TOLERANCE)
    }

    @Test
    fun `should lock a joined group once it snaps onto a placed piece`() {
        val cellWidth = 1f / puzzle.difficulty.cols
        val cellHeight = 1f / puzzle.difficulty.rows
        val anchor = puzzle.pieceAt(0, 0)!!
        val head = puzzle.pieceAt(0, 1)!!
        val tail = puzzle.pieceAt(1, 1)!!

        // Manh dau tien vao dung o cua no tren ban co.
        var state = newBoardState()
        state = state
            .movePiece(anchor.id, -LOOSE_SPOT.x, -LOOSE_SPOT.y)
            .dropPiece(anchor.id)
        assertTrue(state.placements.getValue(anchor.id).isPlaced)

        // Hai manh khac ghep voi nhau thanh mot khoi o giua ban co, chua dinh ban co.
        state = state.movePiece(tail.id, 0f, cellHeight).dropPiece(tail.id)
        assertEquals(state.groupOf(head.id), state.groupOf(tail.id))
        assertFalse(state.placements.getValue(head.id).isPlaced)

        // Keo ca khoi ap vao manh da khoa - lech mot chut nhung trong nguong hut.
        state = state
            .movePiece(head.id, cellWidth - LOOSE_SPOT.x + 0.02f, -LOOSE_SPOT.y + 0.02f)
            .dropPiece(head.id)

        // Khoi dinh vao manh da khoa -> ca khoi vao dung o va bi khoa theo.
        assertEquals(state.groupOf(anchor.id), state.groupOf(head.id))
        listOf(head, tail).forEach { piece ->
            val placement = state.placements.getValue(piece.id)
            assertTrue(placement.isPlaced)
            assertEquals(state.targetOf(piece).x, placement.position.x, TOLERANCE)
            assertEquals(state.targetOf(piece).y, placement.position.y, TOLERANCE)
        }
    }

    @Test
    fun `should fill the missing slots from the top left corner`() {
        val state = newTrayState()

        val missing = state.missingPieces(3)

        assertEquals(listOf(0 to 0, 0 to 1, 0 to 2), missing.map { it.row to it.col })
    }

    @Test
    fun `should skip slots that are already filled when hinting`() {
        val state = newTrayState().placeHint(0)

        val missing = state.missingPieces(2)

        assertEquals(listOf(0 to 1, 0 to 2), missing.map { it.row to it.col })
    }

    @Test
    fun `should lock a hinted piece straight into its slot`() {
        val state = newTrayState()
        val piece = puzzle.pieces.first { it.row == 1 && it.col == 1 }

        val hinted = state.placeHint(piece.id)

        val placement = hinted.placements.getValue(piece.id)
        assertTrue(placement.isPlaced)
        assertFalse(placement.isInTray)
        assertEquals(state.targetOf(piece), placement.position)
        assertFalse(hinted.trayOrder.contains(piece.id))
    }

    @Test
    fun `should carry the whole group when hinting a joined piece`() {
        val state = newBoardState()
        val left = puzzle.pieceAt(0, 0)!!
        val right = puzzle.pieceAt(0, 1)!!
        // Dat manh phai dung tuong quan voi manh trai roi tha cho hai manh dinh vao nhau.
        val joined = state
            .movePiece(right.id, 1f / 3f, 0f)
            .dropPiece(right.id)
        assertEquals(joined.groupOf(left.id), joined.groupOf(right.id))

        val hinted = joined.placeHint(left.id)

        assertTrue(hinted.placements.getValue(left.id).isPlaced)
        assertTrue(hinted.placements.getValue(right.id).isPlaced)
        assertEquals(joined.targetOf(right), hinted.placements.getValue(right.id).position)
    }

    @Test
    fun `should be solved once every piece is placed`() {
        var state = newBoardState()

        puzzle.pieces.forEach { piece ->
            val target = state.targetOf(piece)
            val current = state.placements.getValue(piece.id).position
            state = state
                .movePiece(piece.id, target.x - current.x, target.y - current.y)
                .dropPiece(piece.id)
        }

        assertTrue(state.isSolved)
        assertEquals(puzzle.pieces.size, state.placedCount)
    }
}
