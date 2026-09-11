package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nnastudio.jigsawpuzzlebrainrot.R
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
 * Nut mui ten ngay tren khay mo rong khay thanh mot hop luoi cuon doc ([expanded]) de nhin
 * duoc nhieu manh mot luc, bam lan nua thu ve mot hang nhu cu. Trong hop, keo doc la cuon
 * danh sach chu khong nhac manh: muon dua manh len ban thi cham chon roi bam nut dua ca nhom
 * len - nguoi choi khoi phai keo tung manh.
 *
 * Cho cua manh vua nhac len thu dan ve 0 (khong bien mat ngay) va no lai neu manh duoc tha
 * xuong khay, con cac manh xung quanh truot sang cho moi; nho vay keo ra / them lai khong
 * lam ca danh sach giat mot nhip.
 *
 * Dua manh vao ban co bang cach keo manh len (vuot doc) - [onDragStart]/[onDragMove]/
 * [onDragEnd] bao toa do ngon tay theo goc cua cay layout de man hinh doi sang toa do ban
 * co. Manh thuong chi vao cho khi nguoi choi nhac tay ([onDragEnd]); rieng manh vien co the
 * hut vao o ngay giua duong keo, luc do man hinh ket thuc luot keo tu [onDragMove].
 *
 * Vuot ngang khong bi bat lam keo manh de danh sach con cuon duoc: chi khi vuot doc vuot
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
    /** Cac manh dang duoc cham chon o che do chon nhieu. */
    selectedPieceIds: Set<Int> = emptySet(),
    /** null = dang tat che do chon nhieu, cham vao manh khong co tac dung gi. */
    onPieceTapped: ((pieceId: Int) -> Unit)? = null,
    /** Khay dang mo rong thanh hop nhieu hang hay dang thu ve mot hang. */
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    /** Trang thai cuon cua hop mo rong; tach khoi [listState] vi hai kieu danh sach khac nhau. */
    gridState: LazyGridState = rememberLazyGridState(),
    /**
     * Goc va kich thuoc cua rieng vung danh sach (khong tinh nut mui ten): man hinh dung no
     * de biet manh trong khay dang nam o dau ma cho manh bay den.
     */
    onListMeasured: (origin: Offset, size: IntSize) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    maxPieceSize: Dp = TRAY_PIECE_SIZE,
    /** Phan duoi cung cua khay (nut dua manh len ban): mo hop thi no nam trong tam hop. */
    footer: @Composable ColumnScope.() -> Unit = {}
) {
    val rows = playState.puzzle.difficulty.rows
    val cols = playState.puzzle.difficulty.cols
    val trayBoardPx = trayBoardSizePx(boardSizePx, rows, cols, maxPieceSize)
    val trayBoard = with(LocalDensity.current) { trayBoardPx.toDp() }
    val slotWidth = trayBoard / cols
    val slotHeight = trayBoard / rows
    val margin = maxOf(slotWidth, slotHeight) * TAB_RATIO

    // Cao khay khong phu thuoc ti le manh: ban co lay phan con lai bang weight(1f), neu cao
    // khay chay theo ban co thi hai ben do lan nhau.
    val rowHeight = maxPieceSize * (1f + TAB_RATIO * 2)
    // Hop manh do theo be cao man hinh chu khong theo vung choi: nguoi choi thay dung mot
    // phan man hinh du man to hay nho, va vung choi phia sau khong doi kich thuoc.
    val sheetHeight = LocalConfiguration.current.screenHeightDp.dp * TRAY_SHEET_FRACTION

    // Cho cua manh do theo danh sach dang hien: hop dang mo thi la luoi trong hop, dong roi
    // thi la hang khay. Do ca hai se dap len nhau, manh bay ve mot cho sai.
    val measuredModifier = Modifier.onGloballyPositioned {
        onListMeasured(it.positionInRoot(), it.size)
    }
    val contentPadding = PaddingValues(horizontal = 12.dp, vertical = TRAY_PADDING)

    @Composable
    fun TrayPiece(piece: JigsawPiece, itemModifier: Modifier) {
        TrayPieceItem(
            piece = piece,
            image = image,
            rows = rows,
            cols = cols,
            trayBoardPx = trayBoardPx,
            slotWidth = slotWidth,
            slotHeight = slotHeight,
            margin = margin,
            lifted = piece.id == draggedPieceId || piece.id == hintPieceId,
            selected = piece.id in selectedPieceIds,
            onPieceTapped = onPieceTapped,
            // Hop cuon doc: keo doc la de cuon danh sach nen manh khong con nhac ra duoc,
            // muon dua manh len ban thi cham chon roi bam nut.
            draggable = !expanded,
            onDragStart = onDragStart,
            onDragMove = onDragMove,
            onDragEnd = onDragEnd,
            modifier = itemModifier
        )
    }

    // Hai tam chong nhau: hang khay thu gon nam duoi, hop manh truot len de len no. Doi
    // hop bang cach doi han bo cuc (hang <-> luoi) thi man hinh phai dung 64 manh cua luoi
    // ngay trong mot nhip - do la cu khung luc mo hop. Cach nay dung luoi mot lan roi chi
    // truot no, nen nhip mo / dong khong con phai do lai gi.
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TrayToggle(expanded = false, onExpandedChange = onExpandedChange)

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight + TRAY_PADDING * 2)
                    .clip(TRAY_SHAPE)
                    // Nen khay trong mot phan de mau nen ban choi nhin xuyen qua duoc.
                    .background(AnhnnTheme.extraColors.boardSlot.copy(alpha = TRAY_ALPHA))
                    .then(if (expanded) Modifier else measuredModifier),
                state = listState,
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(TRAY_ITEM_SPACING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(items = pieces, key = { it.id }) { piece ->
                    TrayPiece(
                        piece,
                        Modifier.animateItem(
                            fadeInSpec = tween(TRAY_SLOT_DURATION),
                            placementSpec = TRAY_PLACEMENT_SPEC,
                            fadeOutSpec = null
                        )
                    )
                }
            }

            footer()
        }

        AnimatedVisibility(
            visible = expanded,
            // Truot len / xuong theo dung chieu cao cua chinh no: hop di het ra khoi man hinh
            // roi moi bien mat, khong nhay cai mot.
            enter = slideInVertically(
                animationSpec = tween(TRAY_SHEET_DURATION, easing = FastOutSlowInEasing),
                initialOffsetY = { it }
            ),
            exit = slideOutVertically(
                animationSpec = tween(TRAY_SHEET_DURATION, easing = FastOutSlowInEasing),
                targetOffsetY = { it }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight)
                    // Tam hop dac va bo tron hai goc tren: nut mui ten nam trong hop nhu tay
                    // cam cua bottom sheet, phia sau da co lop phu lam toi ban co.
                    .clip(TRAY_SHAPE)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TrayToggle(expanded = true, onExpandedChange = onExpandedChange)

                LazyVerticalGrid(
                    // O rong bang mot manh: bao nhieu cot la tuy be ngang may.
                    columns = GridCells.Adaptive(minSize = rowHeight),
                    // Luoi an het phan con lai cua hop: nut mui ten va nut dua manh len ban
                    // lay phan cua minh truoc, cao hop van dung bang [sheetHeight].
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(if (expanded) measuredModifier else Modifier),
                    state = gridState,
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(TRAY_ITEM_SPACING),
                    verticalArrangement = Arrangement.spacedBy(TRAY_ITEM_SPACING)
                ) {
                    items(items = pieces, key = { it.id }) { piece ->
                        // O luoi rong hon manh (cot keo dan cho vua be ngang may): dat manh
                        // vao giua o cho ca luoi deu nhau.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    fadeInSpec = tween(TRAY_SLOT_DURATION),
                                    placementSpec = TRAY_PLACEMENT_SPEC,
                                    fadeOutSpec = null
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            TrayPiece(piece, Modifier)
                        }
                    }
                }

                footer()
            }
        }
    }
}

/** Nut mui ten mo / thu khay, nam tren cung cua hang khay va cua hop manh. */
@Composable
private fun ColumnScope.TrayToggle(expanded: Boolean, onExpandedChange: (Boolean) -> Unit) {
    IconButton(
        onClick = { onExpandedChange(!expanded) },
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(TRAY_TOGGLE_SIZE)
    ) {
        Icon(
            painter = painterResource(if (expanded) R.drawable.ic_down else R.drawable.ic_up),
            contentDescription = stringResource(
                if (expanded) R.string.action_collapse_tray else R.string.action_expand_tray
            )
        )
    }
}

/**
 * Mot manh trong khay: ve manh, nhan cham chon va keo manh ra ban co.
 *
 * [modifier] la phan rieng cua tung kieu danh sach (hieu ung doi cho cua o), con moi thu
 * khac hai kieu dung chung.
 */
@Composable
private fun TrayPieceItem(
    piece: JigsawPiece,
    image: ImageBitmap,
    rows: Int,
    cols: Int,
    trayBoardPx: Float,
    slotWidth: Dp,
    slotHeight: Dp,
    margin: Dp,
    /** Manh dang duoc keo / dang bay: lop noi ben tren ve no, cho trong khay thu lai. */
    lifted: Boolean,
    selected: Boolean,
    onPieceTapped: ((pieceId: Int) -> Unit)?,
    /** false = manh nam trong hop cuon doc, keo doc la cuon danh sach chu khong nhac manh. */
    draggable: Boolean,
    onDragStart: (pieceId: Int, position: Offset) -> Unit,
    onDragMove: (position: Offset) -> Unit,
    onDragEnd: (position: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val coordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Manh dang chon nhom len mot chut de nhin ra ngay giua ca hang manh.
    val pick by animateFloatAsState(
        targetValue = if (selected) TRAY_PICKED_SCALE else 1f,
        animationSpec = tween(TRAY_SLOT_DURATION, easing = FastOutSlowInEasing),
        label = "trayPick"
    )
    val slot by animateFloatAsState(
        targetValue = if (lifted) 0f else 1f,
        animationSpec = tween(TRAY_SLOT_DURATION, easing = FastOutSlowInEasing),
        label = "traySlot"
    )

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates.value = it }
            // Cham chon nam o mot pointerInput rieng: no chi doi mot nhip cham roi nha, con
            // khoi ben duoi van lo hoan toan viec keo manh ra khoi khay.
            .then(
                if (onPieceTapped != null) {
                    Modifier.pointerInput(piece.id) {
                        detectTapGestures { onPieceTapped(piece.id) }
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (!draggable) {
                    Modifier
                } else {
                    Modifier.pointerInput(piece.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Cho vuot doc: vuot ngang de danh cho danh sach cuon.
                            val dragged =
                                awaitVerticalTouchSlopOrCancellation(down.id) { change, _ ->
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
                }
            )
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val width = (placeable.width * slot).roundToInt()
                // Ve manh o giua cho da thu lai: thu vao / no ra deu tu tam manh.
                layout(width, placeable.height) {
                    placeable.place((width - placeable.width) / 2, 0)
                }
            }
            .graphicsLayer {
                // Thu ca hinh manh theo cho de khong de len manh ben canh; manh dang keo thi
                // an hoan toan vi lop noi ben tren dang ve no.
                scaleX = slot * pick
                scaleY = slot * pick
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

/** Canh cua "ban co ao" trong khay (px): manh trong khay ve dung ti le ban co de nhin nhu
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

/** Khoang ho giua hai manh trong khay. */
private val TRAY_ITEM_SPACING = 4.dp

/** Canh cua nut mui ten mo / thu khay. */
private val TRAY_TOGGLE_SIZE = 32.dp

/**
 * Cao cua khay luc thu gon (nut mui ten + mot hang manh): man hinh giu san chung nay o cuoi
 * cot de khay mo rong de len ban co ma ban co khong phai do lai.
 */
val TRAY_COLLAPSED_HEIGHT = TRAY_TOGGLE_SIZE + TRAY_PIECE_SIZE * (1f + TAB_RATIO * 2) +
    TRAY_PADDING * 2

/** Hop manh truot len / xuong het trong bao lau (ms). */
private const val TRAY_SHEET_DURATION = 260

/** Hop manh mo rong cao bang bao nhieu phan be cao man hinh. */
private const val TRAY_SHEET_FRACTION = 0.7f

/**
 * Dang cua khay: tran het be ngang va cham day man hinh nen chi bo tron hai goc tren. Hang
 * khay thu gon va hop mo rong dung chung mot dang de mo / thu khong doi net vien.
 */
private val TRAY_SHAPE = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

/** Do duc cua nen khay: du de tach khay khoi ban co, van thay mau nen phia sau. */
private const val TRAY_ALPHA = 0.32f

/** Thoi gian cho trong khay thu lai / no ra khi manh roi khoi khay hay tra ve khay (ms). */
private const val TRAY_SLOT_DURATION = 220

/**
 * Manh doi cho trong khay thi truot theo thay vi nhay: manh moi chen vao day cac manh khac
 * ra tu tu. Manh roi khay khi cho da thu bang 0 nen khong con gi de mo dan - vi the noi goi
 * tat fadeOut, mo dan chi lam manh vua dat len ban co loe lai mot nhip trong khay.
 */
private val TRAY_PLACEMENT_SPEC = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold
)

/** Manh dang duoc cham chon nhom to len bao nhieu lan. */
private const val TRAY_PICKED_SCALE = 1.12f
