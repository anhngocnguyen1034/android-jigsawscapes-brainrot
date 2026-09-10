package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPiece
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnTheme
import kotlin.math.roundToInt

/**
 * Khay manh ghep nam ngang phia duoi ban co, cuon ngang.
 *
 * Khay luon hien du da het manh: keo mot manh roi tu tren xuong day la manh tro ve khay,
 * chen dung cho vua tha trong danh sach. Danh sach hien ra do nguoi goi truyen vao ([pieces])
 * nen man hinh loc bot duoc, con thu tu that cua khay van nam trong [playState].
 *
 * Cho cua manh vua nhac len thu dan ve 0 (khong bien mat ngay) va no lai neu manh duoc tha
 * xuong khay, con cac manh xung quanh truot sang cho moi; nho vay keo ra / them lai khong
 * lam ca danh sach giat mot nhip.
 *
 * Dua manh vao ban co bang cach keo manh len (vuot doc) - [onDragStart]/[onDragMove]/
 * [onDragEnd] bao toa do ngon tay theo goc cua cay layout de man hinh doi sang toa do ban
 * co. Manh chi vao cho khi nguoi choi nhac tay ([onDragEnd]).
 *
 * Vuot ngang khong bi bat lam keo manh de LazyRow con cuon duoc: chi khi vuot doc vuot
 * qua touch slop thi manh moi duoc nhac len.
 */
@Composable
fun JigsawTrayView(
    playState: PuzzlePlayState,
    /**
     * Cac manh hien trong khay, theo dung thu tu hien thi. Thuong la
     * [PuzzlePlayState.trayPieces], nhung man hinh co the loc bot (vi du chi hien manh vien).
     */
    pieces: List<JigsawPiece>,
    image: ImageBitmap,
    draggedPieceId: Int?,
    /** Manh dang bay tu khay vao o cua no (goi y): cho cu trong khay de trong. */
    hintPieceId: Int?,
    listState: LazyListState,
    onDragStart: (pieceId: Int, position: Offset) -> Unit,
    onDragMove: (position: Offset) -> Unit,
    onDragEnd: (position: Offset) -> Unit,
    boardSizePx: Float,
    modifier: Modifier = Modifier,
    maxPieceSize: Dp = TRAY_PIECE_SIZE
) {
    val rows = playState.puzzle.difficulty.rows
    val cols = playState.puzzle.difficulty.cols
    val trayBoardPx = trayBoardSizePx(boardSizePx, rows, cols, maxPieceSize)
    val trayBoard = with(LocalDensity.current) { trayBoardPx.toDp() }
    val slotWidth = trayBoard / cols
    val slotHeight = trayBoard / rows
    val margin = maxOf(slotWidth, slotHeight) * TAB_RATIO

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            // Cao khay khong phu thuoc ti le manh: ban co lay phan con lai bang weight(1f),
            // neu cao khay chay theo ban co thi hai ben do lan nhau.
            .height(maxPieceSize * (1f + TAB_RATIO * 2) + TRAY_PADDING * 2)
            .clip(RoundedCornerShape(16.dp))
            // Nen khay trong mot phan de mau nen ban choi nhin xuyen qua duoc.
            .background(AnhnnTheme.extraColors.boardSlot.copy(alpha = TRAY_ALPHA)),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = TRAY_PADDING),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(items = pieces, key = { it.id }) { piece ->
            val coordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
            // Manh dang duoc keo / dang bay hien o lop noi ben tren: cho cu trong khay thu
            // dan ve 0 cho cac manh phia sau don vao, tha lai trong khay thi cho no ra.
            val lifted = piece.id == draggedPieceId || piece.id == hintPieceId
            val slot by animateFloatAsState(
                targetValue = if (lifted) 0f else 1f,
                animationSpec = tween(TRAY_SLOT_DURATION, easing = FastOutSlowInEasing),
                label = "traySlot"
            )

            Box(
                modifier = Modifier
                    // Cho trong khay thu / no lam cac manh sau doi cho: cho chung truot theo
                    // thay vi nhay. Manh moi chen vao khay cung day cac manh khac ra tu tu.
                    .animateItem(
                        fadeInSpec = tween(TRAY_SLOT_DURATION),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                            visibilityThreshold = IntOffset.VisibilityThreshold
                        ),
                        // Manh roi khay khi cho da thu bang 0 nen khong con gi de mo dan;
                        // mo dan chi lam manh vua dat len ban co loe lai mot nhip trong khay.
                        fadeOutSpec = null
                    )
                    .onGloballyPositioned { coordinates.value = it }
                    .pointerInput(piece.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Cho vuot doc: vuot ngang de danh cho LazyRow cuon.
                            val dragged = awaitVerticalTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                            } ?: return@awaitEachGesture

                            fun rootOf(position: Offset) =
                                coordinates.value?.localToRoot(position) ?: position

                            onDragStart(piece.id, rootOf(dragged.position))
                            var last = dragged.position
                            drag(dragged.id) { change ->
                                change.consume()
                                last = change.position
                                onDragMove(rootOf(last))
                            }
                            onDragEnd(rootOf(last))
                        }
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (placeable.width * slot).roundToInt()
                        // Ve manh o giua cho da thu lai: thu vao / no ra deu tu tam manh.
                        layout(width, placeable.height) {
                            placeable.place((width - placeable.width) / 2, 0)
                        }
                    }
                    .graphicsLayer {
                        // Thu ca hinh manh theo cho de khong de len manh ben canh; manh dang
                        // keo thi an hoan toan vi lop noi ben tren dang ve no.
                        scaleX = slot
                        scaleY = slot
                        alpha = if (lifted) 0f else 1f
                    }
            ) {
                JigsawPieceView(
                    piece = piece,
                    image = image,
                    rows = rows,
                    cols = cols,
                    boardSizePx = trayBoardPx,
                    slotWidth = slotWidth,
                    slotHeight = slotHeight,
                    margin = margin,
                    isPlaced = false
                )
            }
        }
    }
}

/**
 * Canh cua "ban co ao" trong khay (px): manh trong khay ve dung ti le ban co de nhin nhu
 * manh se dat vao o, chi bi chan tren boi [maxPieceSize] cho khay khong phai cao them.
 * [boardSizePx] bang 0 la man hinh chua do xong ban co.
 *
 * Lop noi ve manh goi y bay tu khay len cung dung ham nay de biet manh trong khay dang to
 * bang bao nhieu phan cua manh tren ban co.
 */
@Composable
fun trayBoardSizePx(
    boardSizePx: Float,
    rows: Int,
    cols: Int,
    maxPieceSize: Dp = TRAY_PIECE_SIZE
): Float {
    val cap = with(LocalDensity.current) { (maxPieceSize * minOf(rows, cols)).toPx() }
    return if (boardSizePx > 0f) minOf(boardSizePx, cap) else cap
}

/** Chan tren cho canh o manh trong khay. Cao khay = canh nay + 2 * tai + padding doc. */
val TRAY_PIECE_SIZE = 64.dp

private val TRAY_PADDING = 8.dp

/** Do duc cua nen khay: du de tach khay khoi ban co, van thay mau nen phia sau. */
private const val TRAY_ALPHA = 0.32f

/** Thoi gian cho trong khay thu lai / no ra khi manh roi khoi khay hay tra ve khay (ms). */
private const val TRAY_SLOT_DURATION = 220
