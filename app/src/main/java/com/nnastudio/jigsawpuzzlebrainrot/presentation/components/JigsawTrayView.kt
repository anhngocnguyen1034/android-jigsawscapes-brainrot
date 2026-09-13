package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Trang thai truot cua hop manh, dung chung giua hop va lop phu lam toi man hinh phia sau.
 *
 * [offset] la doan hop dang bi day xuong (px): 0 la mo het, [heightPx] la nam tron duoi day
 * man hinh. Ca hop lan lop phu chi doc gia tri nay trong lambda cua `graphicsLayer`, nen
 * moi frame cua nhip truot khong recompose gi - do la ly do hop khong con khung khi mo.
 */
@Stable
class TraySheetState(val heightPx: Float) {

    val offset = Animatable(heightPx)

    /** 0 = hop dong han, 1 = mo het. Lop phu lay do toi theo day. */
    val progress: Float get() = if (heightPx <= 0f) 0f else 1f - offset.value / heightPx

    /** Truot hop ve trang thai mong muon. */
    suspend fun animateTo(expanded: Boolean) {
        offset.animateTo(
            targetValue = if (expanded) 0f else heightPx,
            animationSpec = tween(TRAY_SHEET_DURATION, easing = FastOutSlowInEasing)
        )
    }

    /** Keo tay cam: hop di theo ngon tay, khong keo len qua mep tren cua no. */
    suspend fun dragBy(delta: Float) {
        offset.snapTo((offset.value + delta).coerceIn(0f, heightPx))
    }

    /**
     * Nha tay sau khi keo: hat xuong du manh, hay da keo hop di qua [CLOSE_FRACTION] chieu
     * cao cua no, thi hop dong han; con lai thi hop tro ve cho cu.
     */
    fun shouldClose(velocity: Float): Boolean = when {
        velocity > FLING_VELOCITY -> true
        // Dang hat nguoc len tren: giu hop lai du no da bi keo xuong kha xa.
        velocity < -FLING_VELOCITY -> false
        else -> offset.value > heightPx * CLOSE_FRACTION
    }

    private companion object {
        /** Keo hop di qua bao nhieu phan chieu cao cua no thi nha tay la hop dong han. */
        const val CLOSE_FRACTION = 0.3f

        /** Hat tay nhanh hon muc nay (px/s) la dong hop du moi keo duoc mot doan ngan. */
        const val FLING_VELOCITY = 800f
    }
}

/**
 * Trang thai hop manh cho mot man choi. Doi be cao man hinh (xoay may) thi dung lai tu dau:
 * doan truot do theo chieu cao moi.
 */
@Composable
fun rememberTraySheetState(): TraySheetState {
    val heightPx = with(LocalDensity.current) {
        (LocalConfiguration.current.screenHeightDp.dp * TRAY_SHEET_FRACTION).toPx()
    }
    return remember(heightPx) { TraySheetState(heightPx) }
}

/**
 * Khay manh ghep nam ngang phia duoi ban co, cuon ngang.
 *
 * Khay luon hien du da het manh: keo mot manh roi tu tren xuong day la manh tro ve khay,
 * chen dung cho vua tha trong danh sach. Danh sach hien ra do nguoi goi truyen vao ([pieces])
 * nen man hinh loc bot duoc, con thu tu that cua khay van nam trong [playState].
 *
 * Nut mui ten ngay tren khay mo [JigsawTraySheet] - hop manh nhieu hang, de nhin duoc nhieu
 * manh mot luc; bam lan nua thi hop dong lai va con moi hang khay nhu cu.
 *
 * Cho cua manh vua nhac len thu dan ve 0 (khong bien mat ngay) va no lai neu manh duoc tha
 * xuong khay, con cac manh xung quanh truot sang cho moi; nho vay keo ra / them lai khong
 * lam ca danh sach giat mot nhip.
 *
 * Dua manh vao ban co bang cach keo manh len (vuot doc) - [onDragStart]/[onDragMove]/
 * [onDragEnd] bao toa do ngon tay theo goc cua cay layout de man hinh doi sang toa do ban
 * co. Keo den gan dung cho thi manh hut vao ngay giua duong keo, luc do man hinh ket thuc
 * luot keo tu [onDragMove]; con lai thi manh vao cho khi nguoi choi nhac tay ([onDragEnd]).
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
    /** Hinh manh da nuong san; null la chua nuong xong, khay tu ve lay tung manh. */
    pieceAtlas: TrayPieceAtlas? = null,
    /** Nen cua khay - thanh duoi cua man choi, dung chung mau voi thanh cong cu tren. */
    barColor: Color = Color.Transparent,
    /** Mau nut mui ten cua khay, di theo mau icon cua thanh cong cu. */
    iconColor: Color = Color.Unspecified,
    onDragStart: (pieceId: Int, position: Offset) -> Unit,
    onDragMove: (position: Offset) -> Unit,
    onDragEnd: (position: Offset) -> Unit,
    boardSizePx: Float,
    /** Cac manh dang duoc cham chon o che do chon nhieu. */
    selectedPieceIds: Set<Int> = emptySet(),
    /** null = dang tat che do chon nhieu, cham vao manh khong co tac dung gi. */
    onPieceTapped: ((pieceId: Int) -> Unit)? = null,
    onExpandedChange: (Boolean) -> Unit = {},
    /**
     * Goc va kich thuoc cua rieng vung danh sach (khong tinh nut mui ten): man hinh dung no
     * de biet manh trong khay dang nam o dau ma cho manh bay den.
     */
    onListMeasured: (origin: Offset, size: IntSize) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    maxPieceSize: Dp = TRAY_PIECE_SIZE,
    /** Phan duoi cung cua khay: nut dua ca nhom manh len ban choi. */
    footer: @Composable ColumnScope.() -> Unit = {}
) {
    val metrics = trayMetrics(playState, boardSizePx, maxPieceSize)
    // Cao khay khong phu thuoc ti le manh: ban co lay phan con lai bang weight(1f), neu cao
    // khay chay theo ban co thi hai ben do lan nhau.
    val rowHeight = maxPieceSize * (1f + TAB_RATIO * 2)

    Column(modifier = modifier.fillMaxWidth()) {
        TrayToggle(
            expanded = false,
            onExpandedChange = onExpandedChange,
            tint = iconColor
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight + TRAY_PADDING * 2)
                .clip(TRAY_SHAPE)
                .background(barColor)
                .onGloballyPositioned { onListMeasured(it.positionInRoot(), it.size) },
            state = listState,
            contentPadding = TRAY_CONTENT_PADDING,
            horizontalArrangement = Arrangement.spacedBy(TRAY_ITEM_SPACING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(items = pieces, key = { it.id }) { piece ->
                TrayPieceItem(
                    piece = piece,
                    image = image,
                    metrics = metrics,
                    atlas = pieceAtlas,
                    lifted = piece.id == draggedPieceId || piece.id == hintPieceId,
                    selected = piece.id in selectedPieceIds,
                    onPieceTapped = onPieceTapped,
                    // Hop manh dang mo thi hang khay nam khuat hoan toan ben duoi hop nen
                    // khong ai cham toi no: khong can tat viec nhac manh o day, va nho vay
                    // mo / dong hop khong dong den hang khay lay mot nhip nao.
                    draggable = true,
                    onDragStart = onDragStart,
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd,
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(TRAY_SLOT_DURATION),
                        placementSpec = TRAY_PLACEMENT_SPEC,
                        fadeOutSpec = null
                    )
                )
            }
        }

        footer()
    }
}

/**
 * Hop manh: mot bottom sheet that su, truot len tu day man hinh de nhin duoc nhieu manh mot
 * luc. Trong hop khong nhac manh ra duoc (keo doc la cuon luoi): muon dua manh len ban thi
 * cham chon roi bam nut o [footer], nguoi choi khoi phai keo tung manh.
 *
 * Hop nam o mot lop rieng chu khong long trong [JigsawTrayView]: nho vay khay thu gon van
 * cao dung bang mot hang manh, va nhung gi do theo mep tren cua khay (nhu tha manh xuong
 * day de tra ve khay) khong bi lech di khi hop mo.
 *
 * Hop luon duoc dung san, ke ca luc dang dong - luc do no chi bi day xuong duoi mep man
 * hinh. Mo hop vi the chi la truot mot lop da ve xong: doan truot nam trong [sheetState] va
 * chi duoc doc trong lambda cua `graphicsLayer` nen khong recompose gi. Dung lai luoi manh
 * ngay trong nhip mo hop moi la cu khung thay ro.
 *
 * Vi le do hop khong nhan tham so "dang mo hay dang dong": doi mot tham so nhu vay se lam
 * ca hop ve lai dung vao frame bat dau mo, tuc la ve lai ca luoi manh - dung cai khung ma
 * lop rieng nay sinh ra de tranh. Muon dong / mo thi bao qua [sheetState].
 *
 * Keo tay cam xuong cung dong duoc hop, va lop phu phia sau sang lai dung theo doan da keo.
 */
@Composable
fun JigsawTraySheet(
    playState: PuzzlePlayState,
    pieces: List<JigsawPiece>,
    image: ImageBitmap,
    draggedPieceId: Int?,
    hintPieceId: Int?,
    /** Doan truot cua hop, dung chung voi lop phu lam toi man hinh phia sau. */
    sheetState: TraySheetState,
    /** Hinh manh da nuong san; null la chua nuong xong, hop tu ve lay tung manh. */
    pieceAtlas: TrayPieceAtlas? = null,
    onExpandedChange: (Boolean) -> Unit,
    boardSizePx: Float,
    /** Trang thai cuon cua luoi trong hop; tach khoi khay vi hai kieu danh sach khac nhau. */
    gridState: LazyGridState = rememberLazyGridState(),
    selectedPieceIds: Set<Int> = emptySet(),
    onPieceTapped: ((pieceId: Int) -> Unit)? = null,
    /** Goc va kich thuoc cua luoi manh, de man hinh biet cho manh bay ve dau. */
    onListMeasured: (origin: Offset, size: IntSize) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    maxPieceSize: Dp = TRAY_PIECE_SIZE,
    /** Phan duoi cung cua hop: nut dua ca nhom manh len ban choi. */
    footer: @Composable ColumnScope.() -> Unit = {}
) {
    val metrics = trayMetrics(playState, boardSizePx, maxPieceSize)
    val rowHeight = maxPieceSize * (1f + TAB_RATIO * 2)
    val sheetHeight = with(LocalDensity.current) { sheetState.heightPx.toDp() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(sheetHeight)
            // Hop dong thi nam tron duoi mep man hinh nen khong an cham cua ai: ban co phia
            // sau van bam duoc binh thuong, khong can chan gi them.
            .graphicsLayer { translationY = sheetState.offset.value }
            // Tam hop dac va bo tron hai goc tren: nut mui ten nam trong hop nhu tay cam
            // cua bottom sheet, phia sau da co lop phu lam toi ban co.
            .clip(TRAY_SHAPE)
            .background(AnhnnTheme.extraColors.traySheet)
    ) {
        TrayToggle(
            expanded = true,
            onExpandedChange = onExpandedChange,
            // Tay cam keo duoc nhu bottom sheet that: hop di theo ngon tay, tha ra thi dong
            // han hay tro ve cho cu tuy da keo duoc bao xa.
            modifier = Modifier.draggable(
                state = rememberDraggableState { delta ->
                    scope.launch { sheetState.dragBy(delta) }
                },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (sheetState.shouldClose(velocity)) {
                        onExpandedChange(false)
                    } else {
                        sheetState.animateTo(expanded = true)
                    }
                }
            )
        )

        LazyVerticalGrid(
            // O rong bang mot manh: bao nhieu cot la tuy be ngang may.
            columns = GridCells.Adaptive(minSize = rowHeight),
            // Luoi an het phan con lai cua hop: nut mui ten va nut dua manh len ban lay
            // phan cua minh truoc, cao hop van dung bang [TraySheetState.heightPx].
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { onListMeasured(it.positionInRoot(), it.size) },
            state = gridState,
            contentPadding = TRAY_CONTENT_PADDING,
            horizontalArrangement = Arrangement.spacedBy(TRAY_ITEM_SPACING),
            verticalArrangement = Arrangement.spacedBy(TRAY_ITEM_SPACING)
        ) {
            items(items = pieces, key = { it.id }) { piece ->
                // O luoi rong hon manh (cot keo dan cho vua be ngang may): dat manh vao
                // giua o cho ca luoi deu nhau.
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
                    TrayPieceItem(
                        piece = piece,
                        image = image,
                        metrics = metrics,
                        atlas = pieceAtlas,
                        lifted = piece.id == draggedPieceId || piece.id == hintPieceId,
                        selected = piece.id in selectedPieceIds,
                        onPieceTapped = onPieceTapped,
                        // Keo doc trong hop la cuon luoi, khong nhac manh ra.
                        draggable = false,
                        onDragStart = { _, _ -> },
                        onDragMove = {},
                        onDragEnd = {}
                    )
                }
            }
        }

        footer()
    }
}

/**
 * Nuong san hinh cua moi manh o co khay, o luong nen.
 *
 * Hang khay va hop manh dung chung mot tam anh: ca hai ve manh cung mot co nen khong viec
 * gi phai nuong hai lan. Chua nuong xong thi tra ve null va khay ve tung manh nhu truoc.
 */
@Composable
fun rememberTrayPieceAtlas(
    playState: PuzzlePlayState,
    image: ImageBitmap,
    boardSizePx: Float,
    maxPieceSize: Dp = TRAY_PIECE_SIZE
): TrayPieceAtlas? {
    val rows = playState.puzzle.difficulty.rows
    val cols = playState.puzzle.difficulty.cols
    val boardPx = trayBoardSizePx(boardSizePx, rows, cols, maxPieceSize)
    val density = LocalDensity.current
    val pieces = playState.puzzle.pieces
    val atlas by produceState<TrayPieceAtlas?>(null, pieces, image, boardPx, density) {
        // Chua do xong ban co thi khoan nuong: co manh trong khay do theo ban co, nuong
        // bay gio la lat nua do xong lai phai nuong lai tu dau.
        if (boardSizePx <= 0f) return@produceState
        value = withContext(Dispatchers.Default) {
            renderTrayPieceAtlas(
                pieces = pieces,
                image = image,
                boardSizePx = boardPx,
                rows = rows,
                cols = cols,
                density = density
            )
        }
    }
    return atlas
}

/** Kich thuoc de ve mot manh trong khay - hang thu gon va hop manh dung chung. */
private data class TrayMetrics(
    val rows: Int,
    val cols: Int,
    val boardPx: Float,
    val slotWidth: Dp,
    val slotHeight: Dp,
    val margin: Dp
)

@Composable
private fun trayMetrics(
    playState: PuzzlePlayState,
    boardSizePx: Float,
    maxPieceSize: Dp
): TrayMetrics {
    val rows = playState.puzzle.difficulty.rows
    val cols = playState.puzzle.difficulty.cols
    val boardPx = trayBoardSizePx(boardSizePx, rows, cols, maxPieceSize)
    val board = with(LocalDensity.current) { boardPx.toDp() }
    val slotWidth = board / cols
    val slotHeight = board / rows
    return TrayMetrics(
        rows = rows,
        cols = cols,
        boardPx = boardPx,
        slotWidth = slotWidth,
        slotHeight = slotHeight,
        margin = maxOf(slotWidth, slotHeight) * TAB_RATIO
    )
}

/**
 * Nut mui ten mo / thu khay, nam tren cung cua hang khay va cua hop manh. Nut nam giua mot
 * dai tran het be ngang: o hop manh, [modifier] bien ca dai do thanh tay cam keo duoc, nen
 * nguoi choi khong phai trung dung vao mui ten moi keo dong hop.
 */
@Composable
private fun TrayToggle(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.size(TRAY_TOGGLE_SIZE)
        ) {
            Icon(
                painter = painterResource(if (expanded) R.drawable.ic_down else R.drawable.ic_up),
                contentDescription = stringResource(
                    if (expanded) R.string.action_collapse_tray else R.string.action_expand_tray
                ),
                tint = if (tint == Color.Unspecified) LocalContentColor.current else tint
            )
        }
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
    metrics: TrayMetrics,
    /** Hinh manh da nuong san; null thi ve manh tai cho. */
    atlas: TrayPieceAtlas?,
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
        val baked = atlas?.offsetOf(piece.id)
        if (baked != null) {
            // Hinh manh da co san trong tam anh chung: chi con dan mot o cua no len man
            // hinh, khong phai clip lai duong bao moi khung hinh.
            Canvas(
                modifier = Modifier.requiredSize(
                    metrics.slotWidth + metrics.margin * 2,
                    metrics.slotHeight + metrics.margin * 2
                )
            ) {
                drawImage(
                    image = atlas.image,
                    srcOffset = baked,
                    srcSize = atlas.pieceSize,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                )
            }
        } else {
            JigsawPieceView(
                piece = piece,
                image = image,
                rows = metrics.rows,
                cols = metrics.cols,
                boardSizePx = metrics.boardPx,
                slotWidth = metrics.slotWidth,
                slotHeight = metrics.slotHeight,
                margin = metrics.margin,
                isPlaced = false
            )
        }
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

/** Le trong cua danh sach manh: hang khay va luoi trong hop dung chung cho deu nhau. */
private val TRAY_CONTENT_PADDING = PaddingValues(horizontal = 12.dp, vertical = TRAY_PADDING)

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
