package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceBounds
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnTheme

/**
 * Vung choi: khung ban co vuong can giua, con manh roi thi di chuyen tu do trong ca vung
 * (ke ca hai ben ngoai khung). Chi ve nhung manh da duoc dua ra khoi khay.
 *
 * Moi manh duoc dat tuyet doi theo toa do "don vi ban co" (1.0 = canh ban co) nen state
 * khong phu thuoc kich thuoc thuc te cua man hinh.
 *
 * [onBoardMeasured] tra ve goc tren-trai cua khung (toa do goc cua cay layout), canh ban co
 * tinh bang px va vung manh roi duoc phep nam - de man hinh doi diem tha ngon tay sang toa
 * do ban co.
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
    onBoardMeasured: (origin: Offset, sizePx: Float, bounds: PieceBounds) -> Unit,
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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val boardSize = minOf(maxWidth, maxHeight)
        val boardLeft = (maxWidth - boardSize) / 2
        val boardTop = (maxHeight - boardSize) / 2
        val density = LocalDensity.current
        val boardSizePx = with(density) { boardSize.toPx() }
        val slotWidth = boardSize / cols
        val slotHeight = boardSize / rows
        val margin = maxOf(slotWidth, slotHeight) * TAB_RATIO
        // Vung manh roi: ca vung choi, do theo goc cua khung ban co.
        val bounds = with(density) {
            PieceBounds(
                minX = -boardLeft.toPx() / boardSizePx,
                minY = -boardTop.toPx() / boardSizePx,
                maxX = (maxWidth - boardLeft).toPx() / boardSizePx - 1f / cols,
                maxY = (maxHeight - boardTop).toPx() / boardSizePx - 1f / rows
            )
        }

        Box(
            modifier = Modifier
                .offset(x = boardLeft, y = boardTop)
                .size(boardSize)
                .onGloballyPositioned {
                    onBoardMeasured(it.positionInRoot(), boardSizePx, bounds)
                }
                .clip(RoundedCornerShape(12.dp))
                .background(AnhnnTheme.extraColors.boardSlot)
        )

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
