package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceBounds
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnTheme

/** Goc bo tron cua khung ban co. */
private val BOARD_CORNER = 12.dp

/** Zoom ban co toi da: du to de ghep luoi 8x8 tren may nho, chua den muc mat huong nhin chung. */
private const val MAX_BOARD_ZOOM = 3f

/**
 * Zoom la phong to ca vung choi (khung ban co lan cho dau manh roi hai ben ngoai khung), nen
 * keo duoc het phan vung choi tran ra ngoai man hinh - do theo canh vung choi chu khong theo
 * canh ban co: do theo ban co thi cua so nhin bi khoa vao khung, manh roi dau ngoai khung se
 * ra ngoai man hinh va khong the keo tro lai.
 */
private fun panLimit(areaPx: Int, zoom: Float) = (areaPx * (zoom - 1f) / 2f).coerceAtLeast(0f)

/**
 * Doi zoom / vi tri ban co sao cho diem [focus] (toa do trong vung choi) van nam duoi ngon
 * tay sau khi phong to, roi cat [pan] lai trong pham vi cho phep.
 */
private fun PointerInputScope.applyZoom(
    focus: Offset,
    zoomChange: Float,
    panChange: Offset,
    zoom: Float,
    pan: Offset,
    onChange: (zoom: Float, pan: Offset) -> Unit
) {
    val next = (zoom * zoomChange).coerceIn(1f, MAX_BOARD_ZOOM)
    val fromCenter = focus - Offset(size.width / 2f, size.height / 2f)
    val moved = fromCenter - (fromCenter - pan) * (next / zoom) + panChange
    val limitX = panLimit(size.width, next)
    val limitY = panLimit(size.height, next)
    onChange(
        next,
        Offset(
            x = moved.x.coerceIn(-limitX, limitX),
            y = moved.y.coerceIn(-limitY, limitY)
        )
    )
}

/**
 * Vung choi: khung ban co vuong can giua, con manh roi thi di chuyen tu do trong ca vung
 * (ke ca hai ben ngoai khung). Chi ve nhung manh da duoc dua ra khoi khay.
 *
 * Moi manh duoc dat tuyet doi theo toa do "don vi ban co" (1.0 = canh ban co) nen state
 * khong phu thuoc kich thuoc thuc te cua man hinh.
 *
 * Chum hai ngon tay de zoom khung ban co (den [MAX_BOARD_ZOOM] lan) va keo de di chuyen no.
 * Zoom chi doi canh ban co that su ([boardSizePx]) chu khong scale mot lop anh: o, manh, le
 * cua manh va ca doan keo deu do tu canh nay nen tat ca to len dung ti le va toa do "don vi
 * ban co" cua state khong doi.
 *
 * [onBoardMeasured] tra ve goc tren-trai cua khung (toa do goc cua cay layout), canh ban co
 * hien tai va canh luc chua zoom (tinh bang px), cung vung manh roi duoc phep nam - de man
 * hinh doi diem tha ngon tay sang toa do ban co.
 *
 * Trong luc keo, manh chi duoc dich bang graphicsLayer tu mot bien local: doi state qua
 * ViewModel moi frame se recompose ca ban co (den 64 manh, moi manh mot Canvas) nen keo bi
 * khung. Toa do that chi duoc bao ve o [onPieceDragEnd] - kem doan da keo (toa do ban co)
 * va diem ngon tay de biet nguoi choi co tha vao khay ben duoi hay khong.
 *
 * Rieng cac manh vien thi hut vao o ngay trong luc keo: manh ve thang vao o va [onPieceDragSnap]
 * chot no lai luon, luot keo ket thuc tai day du ngon tay con tren man hinh. Manh ben trong di
 * theo ngon tay cho den khi nhac tay moi nhay vao cho. Xem [PuzzlePlayState.dragSnapOffset].
 */
@Composable
fun JigsawBoardView(
    playState: PuzzlePlayState,
    image: ImageBitmap,
    draggingPieceId: Int?,
    lastMovedPieceId: Int?,
    /**
     * Cac manh dang bay (goi y vao o cua no, hay don ve khay): chung duoc ve o lop noi ben
     * tren nen cho cu tren ban co de trong va khong nhan cham.
     */
    flyingPieceIds: Set<Int>,
    onBoardMeasured: (
        origin: Offset,
        sizePx: Float,
        baseSizePx: Float,
        bounds: PieceBounds
    ) -> Unit,
    onPieceDragStart: (Int) -> Unit,
    /** Manh vien vua hut vao o giua luc keo: dat va khoa no ngay, luot keo ket thuc. */
    onPieceDragSnap: (pieceId: Int, dx: Float, dy: Float) -> Unit,
    onPieceDragEnd: (pieceId: Int, dx: Float, dy: Float, finger: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = playState.puzzle.difficulty.rows
    val cols = playState.puzzle.difficulty.cols
    // Keo mot manh la ca khoi di theo, nen ca khoi phai noi len tren cac manh khac.
    val draggingGroup = draggingPieceId?.let(playState::groupOf)
    val lastMovedGroup = lastMovedPieceId?.let(playState::groupOf)
    val draggingGroupSize = draggingGroup?.let { playState.groupMembers(it).size } ?: 0
    // Doan da keo, tinh bang px, chi doc trong lambda cua graphicsLayer.
    val dragOffset = remember { mutableStateOf(Offset.Zero) }
    // Cu keo xong la ViewModel bao lai vi tri that; luc do moi bo doan keo tam.
    LaunchedEffect(draggingPieceId) {
        if (draggingPieceId == null) dragOffset.value = Offset.Zero
    }
    // Gesture khong duoc khoi dong lai moi lan state doi nen phai doc state qua bien nay.
    val latestState = rememberUpdatedState(playState)

    // Zoom va vi tri ban co (pan tinh bang px, tu tam vung choi). Van moi thi ve lai tu dau.
    var zoom by remember(playState.puzzle.image) { mutableFloatStateOf(1f) }
    var pan by remember(playState.puzzle.image) { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // Zoom roi thi ban co tran ra ngoai vung choi: cat bot cho khoi de len dong ho
            // va khay. Luc chua zoom thi khong cat, de manh keo xuong khay con thay duoc.
            .then(if (zoom > 1f) Modifier.clipToBounds() else Modifier)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, panChange, zoomChange, _ ->
                    applyZoom(
                        focus = centroid,
                        zoomChange = zoomChange,
                        panChange = panChange,
                        zoom = zoom,
                        pan = pan
                    ) { nextZoom, nextPan ->
                        zoom = nextZoom
                        pan = nextPan
                    }
                }
            }
    ) {
        val density = LocalDensity.current
        val baseBoard = minOf(maxWidth, maxHeight)
        val boardSize = baseBoard * zoom
        val panDp = with(density) { DpOffset(pan.x.toDp(), pan.y.toDp()) }
        val boardLeft = (maxWidth - boardSize) / 2 + panDp.x
        val boardTop = (maxHeight - boardSize) / 2 + panDp.y
        val boardSizePx = with(density) { boardSize.toPx() }
        val baseBoardPx = with(density) { baseBoard.toPx() }
        val slotWidth = boardSize / cols
        val slotHeight = boardSize / rows
        val margin = maxOf(slotWidth, slotHeight) * TAB_RATIO
        // Vung manh roi: ca vung choi, do theo goc cua khung ban co luc chua zoom. Zoom chi
        // doi cach nhin nen cho dau manh khong duoc co lai theo, khong thi manh roi dang o
        // ngoai khung se bi cat ve trong khung ngay khi nguoi choi phong to.
        val baseLeft = (maxWidth - baseBoard) / 2
        val baseTop = (maxHeight - baseBoard) / 2
        val bounds = with(density) {
            PieceBounds(
                minX = -baseLeft.toPx() / baseBoardPx,
                minY = -baseTop.toPx() / baseBoardPx,
                maxX = (maxWidth - baseLeft).toPx() / baseBoardPx - 1f / cols,
                maxY = (maxHeight - baseTop).toPx() / baseBoardPx - 1f / rows
            )
        }

        // Khung ban co ve thang vao lop day chu khong phai mot Box co kich thuoc: zoom to hon
        // vung choi thi rang buoc layout cua cha se kep kich thuoc cua Box lai, con net ve
        // thi khong - no chi bi cat o mep vung choi dung nhu mong doi.
        val boardColor = AnhnnTheme.extraColors.boardSlot
        val boardLeftPx = with(density) { boardLeft.toPx() }
        val boardTopPx = with(density) { boardTop.toPx() }
        var areaRoot by remember { mutableStateOf(Offset.Zero) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { areaRoot = it.positionInRoot() }
        ) {
            drawRoundRect(
                color = boardColor,
                topLeft = Offset(boardLeftPx, boardTopPx),
                size = Size(boardSizePx, boardSizePx),
                cornerRadius = CornerRadius(BOARD_CORNER.toPx())
            )
        }

        // Goc khung tinh thang tu goc vung choi + doan da zoom/keo: doc positionInRoot cua
        // mot Box da bi cat se cho goc sai khi ban co tran ra ngoai vung choi.
        val measured = rememberUpdatedState(onBoardMeasured)
        LaunchedEffect(areaRoot, boardLeftPx, boardTopPx, boardSizePx, baseBoardPx, bounds) {
            measured.value(
                Offset(areaRoot.x + boardLeftPx, areaRoot.y + boardTopPx),
                boardSizePx,
                baseBoardPx,
                bounds
            )
        }

        playState.puzzle.pieces.forEach { piece ->
            val placement = playState.placements[piece.id] ?: return@forEach
            if (placement.isInTray) return@forEach
            val coordinates = remember(piece.id) { mutableStateOf<LayoutCoordinates?>(null) }
            val group = playState.groupOf(piece.id)
            val isDraggingGroup = draggingGroup != null && group == draggingGroup
            JigsawPieceView(
                piece = piece,
                image = image,
                rows = rows,
                cols = cols,
                boardSizePx = boardSizePx,
                slotWidth = slotWidth,
                slotHeight = slotHeight,
                margin = margin,
                isPlaced = placement.isPlaced,
                modifier = Modifier
                    .zIndex(
                        when {
                            isDraggingGroup -> 3f
                            placement.isPlaced -> 0f
                            // Khoi vua duoc thao tac nam tren cac manh roi khac.
                            lastMovedGroup != null && group == lastMovedGroup -> 2f
                            else -> 1f
                        }
                    )
                    .offset(
                        x = boardLeft + boardSize * placement.position.x - margin,
                        y = boardTop + boardSize * placement.position.y - margin
                    )
                    .graphicsLayer {
                        // Ca khoi di theo ngon tay. Chi manh trong khoi doc dragOffset nen
                        // manh khac khong bi ve lai.
                        if (isDraggingGroup) {
                            translationX = dragOffset.value.x
                            translationY = dragOffset.value.y
                        }
                        // Chi phong to manh don le; phong to tung manh cua mot khoi se lam
                        // cac manh trong khoi bi ho ra.
                        val isLoneDragged = piece.id == draggingPieceId && draggingGroupSize == 1
                        val scale = if (isLoneDragged) 1.06f else 1f
                        scaleX = scale
                        scaleY = scale
                        if (piece.id in flyingPieceIds) alpha = 0f
                    }
                    .onGloballyPositioned { coordinates.value = it }
                    .then(
                        if (placement.isPlaced || piece.id in flyingPieceIds) {
                            Modifier
                        } else {
                            Modifier.pointerInput(piece.id, boardSizePx) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var overSlop = Offset.Zero
                                    val dragged = awaitTouchSlopOrCancellation(down.id) { change, over ->
                                        change.consume()
                                        overSlop = over
                                    } ?: return@awaitEachGesture

                                    fun rootOf(position: Offset) =
                                        coordinates.value?.localToRoot(position) ?: position

                                    // Luc an xuong manh chua dich nen toa do goc con dung.
                                    val startFinger = rootOf(down.position)
                                    onPieceDragStart(piece.id)
                                    // Trong luc keo manh di tu do theo ngon tay, ke ca xuong
                                    // khay - co chan lai o day thi manh dung o mep khay va
                                    // nguoi choi tuong khong tra ve khay duoc. Luc tha moi
                                    // gioi han vao vung choi (movePiece tu cat bot).
                                    // Doan vuot qua touch slop cung phai tinh, khong thi manh bi tre.
                                    var total = overSlop
                                    // Manh vien da hut vao o: luot keo ket thuc tai day,
                                    // khong bao drag-end them mot lan nua.
                                    var snapped = false
                                    /** Ve manh theo ngon tay; tra ve true neu vua hut vao o. */
                                    fun applyDrag(): Boolean {
                                        val dx = total.x / boardSizePx
                                        val dy = total.y / boardSizePx
                                        val snap = latestState.value.dragSnapOffset(
                                            pieceId = piece.id,
                                            dx = dx,
                                            dy = dy
                                        )
                                        if (snap == null) {
                                            dragOffset.value = total
                                            return false
                                        }
                                        // Ve thang vao o roi chot luon: manh vien vao dung
                                        // cho la coi nhu xong, khong keo di duoc nua.
                                        dragOffset.value = total +
                                            Offset(snap.x * boardSizePx, snap.y * boardSizePx)
                                        snapped = true
                                        onPieceDragSnap(piece.id, dx, dy)
                                        return true
                                    }
                                    if (applyDrag()) return@awaitEachGesture
                                    // Ngon tay phai do theo toa do goc: manh di theo ngon tay nen
                                    // toa do cuc bo cua no gan nhu khong doi, positionChange() se
                                    // ra xap xi 0 va manh dung yen.
                                    var local = rootOf(dragged.position)
                                    drag(dragged.id) { change ->
                                        change.consume()
                                        if (snapped) return@drag
                                        val current = rootOf(change.position)
                                        total += current - local
                                        local = current
                                        applyDrag()
                                    }
                                    if (snapped) return@awaitEachGesture
                                    // Diem ngon tay phai suy ra tu diem an xuong: localToRoot
                                    // khong tinh phan graphicsLayer da dich nen toa do doc trong
                                    // luc keo bi lech dung bang doan da keo (hieu hai lan doc
                                    // thi khong bi lech nen van dung de tinh total).
                                    val finger = startFinger + total
                                    onPieceDragEnd(
                                        piece.id,
                                        total.x / boardSizePx,
                                        total.y / boardSizePx,
                                        finger
                                    )
                                }
                            }
                        }
                    )
            )
        }
    }
}
