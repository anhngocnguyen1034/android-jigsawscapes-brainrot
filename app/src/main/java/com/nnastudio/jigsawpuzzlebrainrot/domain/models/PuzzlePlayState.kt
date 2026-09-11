package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Toa do trong "don vi ban co": 1.0 = canh cua ban co (vuong).
 * Nho toa do tuong doi nen state khong phu thuoc kich thuoc man hinh hay xoay ngang doc.
 */
@Immutable
data class PieceOffset(val x: Float, val y: Float) {
    operator fun plus(other: PieceOffset) = PieceOffset(x + other.x, y + other.y)

    operator fun minus(other: PieceOffset) = PieceOffset(x - other.x, y - other.y)

    companion object {
        val Zero = PieceOffset(0f, 0f)
    }
}

/**
 * Vung ma manh roi duoc phep nam, tinh theo toa do ban co cho goc tren-trai cua o manh.
 *
 * Manh khong bi bo trong khung ban co: vung nay la ca man hinh phia tren khay, nen gia tri
 * co the am (ben trai / ben tren ban co) hoac lon hon canh ban co.
 */
@Immutable
data class PieceBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    fun clamp(position: PieceOffset) = PieceOffset(
        x = position.x.coerceIn(minX, maxX.coerceAtLeast(minX)),
        y = position.y.coerceIn(minY, maxY.coerceAtLeast(minY))
    )

    companion object {
        /** Truoc khi man hinh do xong thi tam gioi han trong ban co. */
        fun forBoard(difficulty: Difficulty) = PieceBounds(
            minX = 0f,
            minY = 0f,
            maxX = (1f - 1f / difficulty.cols).coerceAtLeast(0f),
            maxY = (1f - 1f / difficulty.rows).coerceAtLeast(0f)
        )
    }
}

/**
 * [isInTray] = manh con nam trong khay cuon ngang ben duoi, chua duoc dua vao ban co nen
 * [position] chua co y nghia (khong ve, khong hut vao manh khac).
 */
@Immutable
data class PiecePlacement(
    val pieceId: Int,
    val position: PieceOffset,
    val isPlaced: Boolean = false,
    val isInTray: Boolean = false
)

/**
 * Trang thai mot van dang choi: bo manh + vi tri hien tai cua tung manh.
 *
 * [groups] gom cac manh da khop vao nhau thanh tung khoi (pieceId -> groupId): keo mot manh
 * la ca khoi di theo. Manh khop truc tiep voi manh ke ben nen nguoi choi khong can dat dung
 * o tren ban co; khi ca buc anh lien thanh mot khoi thi no tu dong fit vao ban co.
 *
 * [trayOrder] la thu tu cac manh con trong khay ben duoi (da tron); manh chi co toa do tren
 * ban co sau khi nguoi choi keo hoac chon no ra khoi khay, va co the keo tra ve khay bat cu
 * cho nao trong danh sach.
 *
 * [bounds] la vung man hinh manh roi duoc phep nam, do man hinh bao lai qua [withBounds].
 */
@Immutable
data class PuzzlePlayState(
    val puzzle: JigsawPuzzle,
    val placements: Map<Int, PiecePlacement>,
    val groups: Map<Int, Int> = placements.keys.associateWith { it },
    val trayOrder: List<Int> = emptyList(),
    val bounds: PieceBounds = PieceBounds.forBoard(puzzle.difficulty),
    val moves: Int = 0
) {
    val isSolved: Boolean get() = placements.values.all { it.isPlaced }

    /**
     * Nguong hut manh (toa do ban co). Tinh theo canh cua mot o chu khong theo canh ban co:
     * cung mot ti le cua canh ban co thi o luoi 8x8 se la nua manh - manh bi hut tu rat xa,
     * con o luoi 3x3 lai gan nhu phai dat trung khop. Theo canh o thi moi do kho deu can
     * dat manh lech duoi [SNAP_RATIO] be rong manh la vua.
     */
    val snapThreshold: Float
        get() = SNAP_RATIO * minOf(1f / puzzle.difficulty.cols, 1f / puzzle.difficulty.rows)

    val placedCount: Int get() = placements.values.count { it.isPlaced }

    /**
     * So moi noi da ghep dung: cu hai khoi hut vao nhau la so khoi giam mot. Manh con trong
     * khay tinh la mot khoi rieng nen dua manh ra khoi khay khong tu lam doi so nay.
     */
    val joinCount: Int get() = placements.size - groups.values.distinct().size

    /** Cac manh con trong khay, theo thu tu hien thi. */
    val trayPieces: List<JigsawPiece>
        get() {
            val pieces = puzzle.pieces.associateBy { it.id }
            return trayOrder.mapNotNull(pieces::get)
        }

    fun isInTray(pieceId: Int): Boolean = placements[pieceId]?.isInTray == true

    /** Man hinh bao lai vung choi thuc te (ca phan ngoai khung ban co). */
    fun withBounds(bounds: PieceBounds): PuzzlePlayState =
        if (bounds == this.bounds) this else copy(bounds = bounds)

    /** Khoi ma manh dang thuoc ve. Manh chua khop voi ai thi khoi chi co mot manh. */
    fun groupOf(pieceId: Int): Int = groups[pieceId] ?: pieceId

    fun groupMembers(groupId: Int): Set<Int> =
        placements.keys.filterTo(mutableSetOf()) { groupOf(it) == groupId }

    /** Vi tri dung cua manh tren ban co. */
    fun targetOf(piece: JigsawPiece): PieceOffset = PieceOffset(
        x = piece.col.toFloat() / puzzle.difficulty.cols,
        y = piece.row.toFloat() / puzzle.difficulty.rows
    )

    /**
     * Dua mot manh tu khay len ban co tai [position] (toa do ban co, goc tren-trai cua o),
     * roi thu hut ngay vao manh ke ben / o dung nhu khi tha binh thuong.
     */
    fun releaseFromTray(
        pieceId: Int,
        position: PieceOffset,
        snapThreshold: Float = this.snapThreshold
    ): PuzzlePlayState {
        val placement = placements[pieceId] ?: return this
        if (!placement.isInTray) return this
        val released = copy(
            placements = placements + (pieceId to placement.copy(
                position = bounds.clamp(position),
                isInTray = false
            )),
            trayOrder = trayOrder - pieceId
        )
        return released.dropPiece(pieceId, snapThreshold)
    }

    /**
     * Cac manh dang roi le tren ban co, theo thu tu quet luoi - dung khi don het ve khay.
     * Khoi da ghep khong bi don: don ca khoi la pha cong nguoi choi vua ghep.
     */
    val loosePieces: List<JigsawPiece>
        get() = puzzle.pieces.filter { canReturnToTray(it.id) }

    /** Chi manh roi le, chua khoa, moi tra ve khay duoc - khoi da ghep thi giu tren man hinh. */
    fun canReturnToTray(pieceId: Int): Boolean {
        val placement = placements[pieceId] ?: return false
        return !placement.isPlaced &&
            !placement.isInTray &&
            groupMembers(groupOf(pieceId)).size == 1
    }

    /** Keo manh tra ve khay, chen vao dung [index] trong danh sach. */
    fun returnToTray(pieceId: Int, index: Int): PuzzlePlayState {
        if (!canReturnToTray(pieceId)) return this
        val placement = placements.getValue(pieceId)
        val rest = trayOrder - pieceId
        return copy(
            placements = placements + (pieceId to placement.copy(isInTray = true)),
            trayOrder = rest.toMutableList().apply { add(index.coerceIn(0, size), pieceId) }
        )
    }

    /**
     * Doi cho mot manh dang o trong khay: keo no ngang roi tha lai trong khay thi no ve
     * dung [index] vua tha, khong bat ve cho cu.
     *
     * [index] la cho chen tinh theo danh sach hien tai (danh sach van con [pieceId]), nen
     * khi keo sang phai phai tru mot cho vi mang manh ra khoi cho cu lam cac manh sau don len.
     */
    fun moveInTray(pieceId: Int, index: Int): PuzzlePlayState {
        if (!isInTray(pieceId)) return this
        val from = trayOrder.indexOf(pieceId)
        if (from < 0) return this
        val to = (if (index > from) index - 1 else index).coerceIn(0, trayOrder.size - 1)
        if (to == from) return this
        return copy(
            trayOrder = trayOrder.toMutableList().apply { add(to, removeAt(from)) }
        )
    }

    /**
     * Cac o con thieu, quet tu tren xuong duoi va tu trai sang phai. Goi y dien vao dung
     * thu tu nay nen nguoi choi luon thay buc anh hien dan tu goc tren-trai.
     */
    fun missingPieces(count: Int): List<JigsawPiece> = puzzle.pieces
        .filter { placements[it.id]?.isPlaced != true }
        .sortedWith(compareBy({ it.row }, { it.col }))
        .take(count)

    /**
     * Goi y: dat thang mot manh vao dung o cua no roi khoa lai, du manh dang o trong khay
     * hay dang nam roi tren ban co.
     *
     * Manh dang thuoc mot khoi thi ca khoi di theo - cac manh trong khoi da dung tuong quan
     * voi nhau nen tat ca cung vao dung o mot luc.
     */
    fun placeHint(pieceId: Int): PuzzlePlayState {
        val placement = placements[pieceId] ?: return this
        if (placement.isPlaced) return this
        val piece = puzzle.pieces.firstOrNull { it.id == pieceId } ?: return this
        val target = targetOf(piece)

        val counted = copy(moves = moves + 1)
        val staged = if (placement.isInTray) {
            counted.copy(
                placements = counted.placements +
                    (pieceId to placement.copy(position = target, isInTray = false)),
                trayOrder = counted.trayOrder - pieceId
            )
        } else {
            counted.translate(
                counted.groupMembers(counted.groupOf(pieceId)),
                target.x - placement.position.x,
                target.y - placement.position.y
            )
        }

        val groupId = staged.groupOf(pieceId)
        return staged.lock(staged.groupMembers(groupId))
            .mergeAlignedNeighbours(groupId)
            .settle(groupId)
    }

    /**
     * Manh nam o vien ban co (mot trong 4 canh xung quanh). Vien la moc de nguoi choi dung
     * khung nen cac manh nay duoc hut vao o ngay trong luc keo - xem [dragSnapOffset].
     */
    fun isEdgePiece(pieceId: Int): Boolean {
        val piece = puzzle.pieces.firstOrNull { it.id == pieceId } ?: return false
        return piece.row == 0 ||
            piece.row == puzzle.difficulty.rows - 1 ||
            piece.col == 0 ||
            piece.col == puzzle.difficulty.cols - 1
    }

    /**
     * Manh o mot trong 4 goc khung. Goc la diem tua de dung khung nen no duoc hut noi tay hon
     * cac manh khac mot chut ([CORNER_SNAP_RATIO]), ca trong luc di chuyen lan khi tha tay.
     */
    fun isCornerPiece(pieceId: Int): Boolean {
        val piece = puzzle.pieces.firstOrNull { it.id == pieceId } ?: return false
        val lastRow = puzzle.difficulty.rows - 1
        val lastCol = puzzle.difficulty.cols - 1
        return (piece.row == 0 || piece.row == lastRow) && (piece.col == 0 || piece.col == lastCol)
    }

    /** Nguong hut vao dung o cua rieng mot manh: manh goc duoc hut noi tay hon mot chut. */
    private fun boardSnapThresholdOf(pieceId: Int, threshold: Float): Float =
        if (isCornerPiece(pieceId)) threshold * CORNER_SNAP_RATIO / SNAP_RATIO else threshold

    /**
     * Doan hut them can cong vao doan dang keo ([dx], [dy] theo toa do ban co) de manh vao
     * dung o cua no ngay giua luc keo; null neu khong hut.
     *
     * Chi cac manh vien duoc hut som nhu vay, cac manh ben trong chi nhay vao cho khi tha
     * ([dropPiece]) - manh trong long buc anh khong co moc nao de doi chieu nen hut som chi
     * lam manh giat khoi ngon tay. Rieng 4 manh goc duoc hut noi tay hon ([isCornerPiece]).
     */
    fun dragSnapOffset(
        pieceId: Int,
        dx: Float,
        dy: Float,
        snapThreshold: Float = this.snapThreshold
    ): PieceOffset? {
        val placement = placements[pieceId] ?: return null
        if (placement.isPlaced || placement.isInTray) return null
        if (!isEdgePiece(pieceId)) return null
        // Chi do rieng manh dang keo: manh nay dung o thi ca khoi cung dung, vi tuong quan
        // giua cac manh trong mot khoi da chinh xac.
        val fit = findBoardFit(setOf(pieceId), snapThreshold, PieceOffset(dx, dy)) ?: return null
        return PieceOffset(fit.dx, fit.dy)
    }

    /**
     * Manh vien vua vao dung o giua luc keo: dat no (va ca khoi) vao o roi khoa luon, nen
     * nguoi choi khong keo no di duoc nua. Tra ve null neu chua den luc hut (xem
     * [dragSnapOffset]).
     *
     * [dx], [dy] la doan da keo tinh tu vi tri hien tai cua manh, theo toa do ban co.
     */
    fun snapWhileDragging(
        pieceId: Int,
        dx: Float,
        dy: Float,
        snapThreshold: Float = this.snapThreshold
    ): PuzzlePlayState? {
        val snap = dragSnapOffset(pieceId, dx, dy, snapThreshold) ?: return null
        // Tinh la mot luot dat manh nhu khi tha binh thuong (xem [dropPiece]).
        val moved = movePiece(pieceId, dx + snap.x, dy + snap.y).copy(moves = moves + 1)
        val groupId = moved.groupOf(pieceId)
        return moved
            .lock(moved.groupMembers(groupId))
            .mergeAlignedNeighbours(groupId)
            .settle(groupId)
    }

    /**
     * Keo manh di mot doan (toa do ban co). Ca khoi di theo; khoi da khop thi khoa lai.
     * Khoi bi gioi han trong [bounds] - ca man hinh phia tren khay, khong chi khung ban co.
     */
    fun movePiece(pieceId: Int, dx: Float, dy: Float): PuzzlePlayState {
        val placement = placements[pieceId] ?: return this
        if (placement.isPlaced || placement.isInTray) return this
        val delta = dragRange(pieceId).clamp(PieceOffset(dx, dy))
        return translate(groupMembers(groupOf(pieceId)), delta.x, delta.y)
    }

    /** Doan keo toi da (toa do ban co) de ca khoi chua [pieceId] con nam trong [bounds]. */
    private fun dragRange(pieceId: Int): PieceBounds {
        val placement = placements[pieceId]
        if (placement == null || placement.isPlaced || placement.isInTray) {
            return PieceBounds(minX = 0f, minY = 0f, maxX = 0f, maxY = 0f)
        }
        val positions = groupMembers(groupOf(pieceId)).map { placements.getValue(it).position }
        return PieceBounds(
            minX = bounds.minX - positions.minOf { it.x },
            minY = bounds.minY - positions.minOf { it.y },
            maxX = bounds.maxX - positions.maxOf { it.x },
            maxY = bounds.maxY - positions.maxOf { it.y }
        )
    }

    /**
     * Tha manh. Uu tien hut vao manh ke ben (chi can dat gan dung tuong quan, khong can
     * dung o tren ban co); neu khong co manh nao ke ben thi thu hut vao dung o cua minh.
     *
     * Rieng manh goc thi o tren ban co duoc uu tien truoc: goc khung la moc ro rang nhat,
     * dat vao goc la nguoi choi muon no vao khung chu khong phai dinh vao mot manh roi
     * ngau nhien nam ke ben.
     */
    fun dropPiece(pieceId: Int, snapThreshold: Float = this.snapThreshold): PuzzlePlayState {
        val placement = placements[pieceId] ?: return this
        if (placement.isPlaced || placement.isInTray) return this

        val groupId = groupOf(pieceId)
        val members = groupMembers(groupId)
        val dropped = copy(moves = moves + 1)

        val cornerFit = dropped.findBoardFit(members, snapThreshold)?.takeIf { it.isCorner }
        if (cornerFit != null) {
            return dropped
                .translate(members, cornerFit.dx, cornerFit.dy)
                .lock(members)
                .mergeAlignedNeighbours(groupId)
                .settle(groupId)
        }

        val neighbourFit = dropped.findNeighbourFit(members, snapThreshold)
        if (neighbourFit != null) {
            return dropped
                .translate(members, neighbourFit.dx, neighbourFit.dy)
                .merge(groupId, neighbourFit.groupId)
                .mergeAlignedNeighbours(groupId)
                .settle(groupId)
        }

        val boardFit = dropped.findBoardFit(members, snapThreshold)
        if (boardFit != null) {
            return dropped
                .translate(members, boardFit.dx, boardFit.dy)
                .lock(members)
        }
        return dropped
    }

    private fun translate(members: Set<Int>, dx: Float, dy: Float): PuzzlePlayState {
        val moved = members.associateWith { id ->
            val placement = placements.getValue(id)
            placement.copy(
                position = PieceOffset(placement.position.x + dx, placement.position.y + dy)
            )
        }
        return copy(placements = placements + moved)
    }

    private fun lock(members: Set<Int>): PuzzlePlayState {
        val locked = members.associateWith { placements.getValue(it).copy(isPlaced = true) }
        return copy(placements = placements + locked)
    }

    /** Dat tung manh cua khoi vao dung o cua no - dung khi ca buc anh da lien khoi. */
    private fun snapToTargets(members: Set<Int>): PuzzlePlayState {
        val pieces = puzzle.pieces.associateBy { it.id }
        val fitted = members.associateWith { id ->
            val piece = pieces.getValue(id)
            placements.getValue(id).copy(position = targetOf(piece))
        }
        return copy(placements = placements + fitted)
    }

    private fun merge(keep: Int, absorbed: Int): PuzzlePlayState {
        if (keep == absorbed) return this
        return copy(
            groups = placements.keys.associateWith { id ->
                if (groupOf(id) == absorbed) keep else groupOf(id)
            }
        )
    }

    /**
     * Gop luon nhung khoi da nam dung tuong quan sau khi hut - mot manh tha vao goc co the
     * dong thoi khop voi khoi ben trai va khoi ben tren.
     */
    private fun mergeAlignedNeighbours(groupId: Int): PuzzlePlayState {
        var state = this
        while (true) {
            val members = state.groupMembers(groupId)
            val fit = state.findNeighbourFit(members, ALIGNED_EPSILON) ?: return state
            state = state.merge(groupId, fit.groupId)
        }
    }

    /**
     * Khoa khoi lai neu no da nam dung ban co: hoac vua gop voi mot khoi da khoa, hoac da
     * lien thanh ca buc anh - luc do tu dong fit vao ban co.
     */
    private fun settle(groupId: Int): PuzzlePlayState {
        val members = groupMembers(groupId)
        val isWholePicture = members.size == puzzle.pieces.size
        val touchesBoard = members.any { placements.getValue(it).isPlaced }
        if (!isWholePicture && !touchesBoard) return this

        val fitted = if (isWholePicture) snapToTargets(members) else this
        return fitted.lock(members)
    }

    /**
     * Doan can dich chuyen de khoi khop vao mot manh ke ben, null neu khong co manh nao du gan.
     *
     * [offset] la doan gia dinh khoi da duoc keo them - de hoi truoc ket qua ma khong phai
     * dich that cac manh.
     */
    private fun findNeighbourFit(
        members: Set<Int>,
        threshold: Float,
        offset: PieceOffset = PieceOffset.Zero
    ): Fit? {
        val pieces = puzzle.pieces.associateBy { it.id }
        var best: Fit? = null
        var bestError = Float.MAX_VALUE

        for (id in members) {
            val piece = pieces[id] ?: continue
            val position = placements.getValue(id).position + offset
            for (other in puzzle.pieces) {
                if (other.id in members) continue
                // Manh con trong khay chua co toa do thuc -> khong hut vao duoc.
                if (placements[other.id]?.isInTray != false) continue
                val rowStep = other.row - piece.row
                val colStep = other.col - piece.col
                // Chi xet 4 manh ke ben trong luoi.
                if (abs(rowStep) + abs(colStep) != 1) continue

                val otherPosition = placements.getValue(other.id).position
                val dx = otherPosition.x - colStep.toFloat() / puzzle.difficulty.cols - position.x
                val dy = otherPosition.y - rowStep.toFloat() / puzzle.difficulty.rows - position.y
                val error = hypot(dx, dy)
                if (error > threshold) continue
                if (error < bestError) {
                    bestError = error
                    best = Fit(dx = dx, dy = dy, groupId = groupOf(other.id))
                }
            }
        }
        return best
    }

    /** Doan can dich chuyen de khoi vao dung o tren ban co, null neu con xa. Xem [findNeighbourFit]. */
    private fun findBoardFit(
        members: Set<Int>,
        threshold: Float,
        offset: PieceOffset = PieceOffset.Zero
    ): Fit? {
        val pieces = puzzle.pieces.associateBy { it.id }
        var best: Fit? = null
        var bestError = Float.MAX_VALUE

        for (id in members) {
            val piece = pieces[id] ?: continue
            val position = placements.getValue(id).position + offset
            val target = targetOf(piece)
            val dx = target.x - position.x
            val dy = target.y - position.y
            val error = hypot(dx, dy)
            if (error > boardSnapThresholdOf(id, threshold)) continue
            if (error < bestError) {
                bestError = error
                best = Fit(
                    dx = dx,
                    dy = dy,
                    groupId = groupOf(id),
                    isCorner = isCornerPiece(id)
                )
            }
        }
        return best
    }

    /** [isCorner] = cho hut la o cua mot manh goc tren ban co. */
    private data class Fit(
        val dx: Float,
        val dy: Float,
        val groupId: Int,
        val isCorner: Boolean = false
    )

    companion object {
        /** Nguong hut manh: manh phai dat lech duoi 18% be rong mot manh. Xem [snapThreshold]. */
        private const val SNAP_RATIO = 0.18f

        /**
         * Nguong hut rieng cho 4 manh goc: chi rong hon [SNAP_RATIO] mot chut, gan nhu manh
         * vien thuong. Goc khung chi co mot cho duy nhat nen noi tay hon manh thuong cho de
         * dat, nhung hut tu ca mot manh thi manh tu nhay vao goc khi nguoi choi chi dinh keo
         * ngang qua - mat cam giac tu dat manh. Xem [isCornerPiece].
         */
        private const val CORNER_SNAP_RATIO = 0.25f

        /** Sai so coi nhu da nam dung tuong quan (dung khi gop them khoi ke ben). */
        private const val ALIGNED_EPSILON = 1e-4f

        /** So diem thu tren moi chieu khi tim cho trong cho manh chon tu khay. */
    }
}
