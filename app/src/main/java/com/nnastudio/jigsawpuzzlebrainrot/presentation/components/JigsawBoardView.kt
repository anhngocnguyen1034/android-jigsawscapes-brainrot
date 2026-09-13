package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.BoardBackground
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceBounds
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.BoardSlotColors
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.slotColors

/** Goc bo tron cua khung ban co. */
private val BOARD_CORNER = 12.dp

/** Do day net vien cua khung ghep. */
private val BOARD_BORDER = 1.5.dp

/**
 * Ngon tay phai di qua ngan nay thi moi tinh la keo manh (thay cho touch slop he thong, von
 * lon gap may lan). Cang nho thi manh cang bat dinh ngon tay ngay, chi can du de mot cu cham
 * nhe khong thanh nuoc di va de hai ngon tay kip bat dau zoom ban co.
 */
private val DRAG_SLOP = 2.dp

/** Co cua manh luc bat dau nhip nay len sau khi duoc dua tu khay len ban. */
private const val PIECE_POP_FROM = 0.4f

/**
 * Nhip nay cua manh vua duoc dua len ban: lo qua co that mot chut roi mai ve - manh nhu duoc
 * tha xuong ban chu khong phai hien ra dung cho.
 */
private val PIECE_POP_SPEC = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow
)

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
    /**
     * Cac manh vua duoc dua ca nhom tu khay len ban: chung hien ra kem mot nhip nay len de
     * nguoi choi nhin ra ngay nhom manh moi giua nhung manh da nam san.
     */
    poppingPieceIds: Set<Int>,
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
    /** Mau khung ghep, do theo nen ban choi nguoi choi dang chon. */
    boardColors: BoardSlotColors = BoardBackground.DEFAULT.slotColors(),
    modifier: Modifier = Modifier
) {
    val rows = playState.puzzle.difficulty.rows
    val cols = playState.puzzle.difficulty.cols
    // Keo mot manh la ca khoi di theo, nen ca khoi phai noi len tren cac manh khac.
    val draggingGroup = draggingPieceId?.let(playState::groupOf)
    val lastMovedGroup = lastMovedPieceId?.let(playState::groupOf)
    val draggingGroupSize = draggingGroup?.let { playState.groupMembers(it).size } ?: 0
    // Thu tu de len nhau cua cac manh roi: khoi nao vua duoc dong den thi len tren cung va
    // nam nguyen do cho den khi chinh no bi dong den lai.
    //
    // Chi cho rieng "khoi vua di" noi len thi khong du: buong tay ra, dong sang mot manh
    // thu ba la khoi vua di tut ve thu tu goc (thu tu cat manh), nen hai manh dang de len
    // nhau tu dung doi cho cho nhau du nguoi choi khong cham vao chung.
    val stackOrder = remember(playState.puzzle) { mutableStateListOf<Int>() }
    val bumpedGroup = draggingGroup ?: lastMovedGroup
    LaunchedEffect(bumpedGroup) {
        if (bumpedGroup != null) {
            stackOrder.remove(bumpedGroup)
            stackOrder.add(bumpedGroup)
        }
    }
    // Manh chua duoc dong den nam duoi cung (1f), cac khoi da dong den xep dan len theo
    // dung thu tu tren - tat ca van duoi khoi dang keo (3f).
    val stackZ = stackOrder.withIndex().associate { (index, group) ->
        group to 1f + (index + 1f) / (stackOrder.size + 1f)
    }
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
        // Khung ban co cung trong mot phan: doi mau nen ban choi la thay doi ca o day.
        val boardLeftPx = with(density) { boardLeft.toPx() }
        val boardTopPx = with(density) { boardTop.toPx() }
        var areaRoot by remember { mutableStateOf(Offset.Zero) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { areaRoot = it.positionInRoot() }
        ) {
            val topLeft = Offset(boardLeftPx, boardTopPx)
            val size = Size(boardSizePx, boardSizePx)
            val corner = CornerRadius(BOARD_CORNER.toPx())
            drawRoundRect(
                color = boardColors.fill,
                topLeft = topLeft,
                size = size,
                cornerRadius = corner
            )
            drawRoundRect(
                color = boardColors.border,
                topLeft = topLeft,
                size = size,
                cornerRadius = corner,
                style = Stroke(width = BOARD_BORDER.toPx())
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
            val popping = piece.id in poppingPieceIds
            // Manh vua duoc dua tu khay len: bat dau tu co nho roi nay ve co that. Khoi tao
            // san o co nho chu khong doi LaunchedEffect chay, khong thi manh loe dung co
            // that mot frame truoc khi nhip nay bat dau.
            val pop = remember(piece.id) {
                Animatable(if (popping) PIECE_POP_FROM else 1f)
            }
            LaunchedEffect(piece.id, popping) {
                if (popping) pop.animateTo(1f, PIECE_POP_SPEC) else pop.snapTo(1f)
            }
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
                            else -> stackZ[group] ?: 1f
                        }
                    )
                    .offset(
                        x = boardLeft + boardSize * placement.position.x - margin,
                        y = boardTop + boardSize * placement.position.y - margin
                    )
                    // Gesture nam NGOAI lop graphicsLayer dang dich manh: toa do cua no vi
                    // the dung yen trong suot luot keo. De ben trong lop thi moi frame doc
                    // ra mot he toa do da bi dich, doan keo tinh ra lech nhip va manh rung.
                    .onGloballyPositioned { coordinates.value = it }
                    .then(
                        if (placement.isPlaced || piece.id in flyingPieceIds) {
                            Modifier
                        } else {
                            Modifier.pointerInput(piece.id, boardSizePx) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)

                                    fun rootOf(position: Offset) =
                                        coordinates.value?.localToRoot(position) ?: position

                                    // Luc an xuong manh chua dich nen toa do goc con dung.
                                    val startFinger = rootOf(down.position)
                                    // Trong luc keo manh di tu do theo ngon tay, ke ca xuong
                                    // khay - co chan lai o day thi manh dung o mep khay va
                                    // nguoi choi tuong khong tra ve khay duoc. Luc tha moi
                                    // gioi han vao vung choi (movePiece tu cat bot).
                                    // Doan di truoc khi nhan ra la keo cung duoc tinh vao,
                                    // khong thi manh bi tre lai sau ngon tay.
                                    var total = Offset.Zero
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
                                    // Nguong bat dau keo rieng cua ban co, nho hon touch slop
                                    // he thong nhieu lan: manh dinh ngon tay gan nhu tuc thi
                                    // chu khong tro ra sau mot doan. Van con nguong de cham
                                    // nhe khong thanh mot nuoc di, va de nhip dau con lot cho
                                    // hai ngon tay bat dau zoom ban co.
                                    val slopPx = DRAG_SLOP.toPx()
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: return@awaitEachGesture
                                        // Zoom hai ngon tay da lay su kien nay, hoac ngon tay
                                        // da nhac len: khong phai luot keo manh.
                                        if (change.isConsumed || !change.pressed) {
                                            return@awaitEachGesture
                                        }
                                        total += change.positionChange()
                                        if (total.getDistance() > slopPx) {
                                            // Nhan luot keo ve minh: tu day zoom khong lay nua.
                                            change.consume()
                                            break
                                        }
                                    }

                                    onPieceDragStart(piece.id)
                                    if (applyDrag()) return@awaitEachGesture
                                    drag(down.id) { change ->
                                        // Doc doan dich truoc khi consume: consume roi thi
                                        // positionChange() tra ve 0.
                                        val moved = change.positionChange()
                                        change.consume()
                                        if (snapped) return@drag
                                        total += moved
                                        applyDrag()
                                    }
                                    if (snapped) return@awaitEachGesture
                                    // Node nhan gesture dung yen nen diem ngon tay = diem an
                                    // xuong cong ca doan da keo.
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
                        // Doc pop.value trong lambda nay: doc o pha composition thi moi frame
                        // cua nhip nay se ve lai ca ban co.
                        val scale = (if (isLoneDragged) 1.06f else 1f) * pop.value
                        scaleX = scale
                        scaleY = scale
                        if (piece.id in flyingPieceIds) alpha = 0f
                    }
            )
        }
    }
}
