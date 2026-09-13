package com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.BoardBackground
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceBounds
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPiece
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceOffset
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.JigsawBoardView
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.JigsawPieceView
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.JigsawTraySheet
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.JigsawTrayView
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.rememberTrayPieceAtlas
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.rememberTraySheetState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.TRAY_COLLAPSED_HEIGHT
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.rememberGameFeedback
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.TAB_RATIO
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.trayBoardSizePx
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnTheme
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.color
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.labelRes
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.slotColors
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.GameUiState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.GameViewModel
import com.nnastudio.jigsawpuzzlebrainrot.utils.toImageBitmap
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Rung / tieng bam theo so manh da vao o thay vi bam vao tung cho goi ViewModel: manh
    // vao o duoc bang nhieu duong - keo tay, tha tu khay, goi y, va ca ghep khoi lam nhieu
    // manh vao mot luc - nhung duong nao roi cung lam so nay tang.
    val feedback = rememberGameFeedback(
        soundEnabled = uiState.soundEnabled,
        vibrationEnabled = uiState.vibrationEnabled
    )
    var lastPlacedCount by remember { mutableIntStateOf(uiState.placedCount) }
    LaunchedEffect(uiState.placedCount) {
        if (uiState.placedCount > lastPlacedCount) feedback.onPiecePlaced()
        lastPlacedCount = uiState.placedCount
    }
    LaunchedEffect(uiState.isSolved) {
        if (uiState.isSolved) feedback.onSolved()
    }

    GameContent(
        uiState = uiState,
        onPieceDragStart = viewModel::onPieceDragStart,
        onPieceDragEnd = viewModel::onPieceDragEnd,
        onPieceDragSnap = viewModel::onPieceSnappedWhileDragging,
        onPieceReturnedToTray = viewModel::onPieceReturnedToTray,
        onTrayPieceMoved = viewModel::onTrayPieceMoved,
        onTrayPieceDropped = viewModel::onTrayPieceDropped,
        onPlayAreaMeasured = viewModel::onPlayAreaMeasured,
        onHintRequested = viewModel::onHintRequested,
        onHintPieceLanded = viewModel::onHintPieceLanded,
        onCleanRequested = viewModel::onCleanRequested,
        onTrayPieceTapped = viewModel::onTrayPieceTapped,
        onSelectedPiecesReleased = viewModel::onSelectedPiecesReleased,
        onTrayExpandedChange = viewModel::onTrayExpandedChange,
        onCleanPieceLanded = viewModel::onCleanPieceLanded,
        onTogglePause = viewModel::onTogglePause,
        onBackgroundSelected = viewModel::onBoardBackgroundSelected,
        onToggleEdgePiecesOnly = viewModel::onToggleEdgePiecesOnly,
        onBack = onBack
    )
}

// statusBarsIgnoringVisibility: chua on dinh nhung la cach duy nhat biet status bar cao bao
// nhieu trong luc no dang bi an (MainActivity an het thanh he thong).
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GameContent(
    uiState: GameUiState,
    onPieceDragStart: (Int) -> Unit,
    onPieceDragEnd: (Int, Float, Float) -> Unit,
    onPieceDragSnap: (Int, Float, Float) -> Unit,
    onPieceReturnedToTray: (pieceId: Int, index: Int) -> Unit,
    onTrayPieceMoved: (pieceId: Int, index: Int) -> Unit,
    onTrayPieceDropped: (Int, PieceOffset) -> Unit,
    onPlayAreaMeasured: (PieceBounds) -> Unit,
    onHintRequested: () -> Unit,
    onHintPieceLanded: (Int) -> Unit,
    onCleanRequested: () -> Unit,
    onTrayPieceTapped: (Int) -> Unit,
    onSelectedPiecesReleased: () -> Unit,
    onTrayExpandedChange: (Boolean) -> Unit,
    onCleanPieceLanded: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onBackgroundSelected: (BoardBackground) -> Unit,
    onToggleEdgePiecesOnly: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPeek by remember { mutableStateOf(false) }
    var showBackgrounds by remember { mutableStateOf(false) }
    // Canh khung ghep, do trong PlayArea: anh mau zoom to nhat bang dung khung nay.
    var boardSizePx by remember { mutableFloatStateOf(0f) }
    val image = remember(uiState.artwork) { uiState.artwork?.toImageBitmap() }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Mau nen phu ca man hinh (ke ca cho status bar): thanh cong cu, khung ban co va
            // khay deu trong suot mot phan nen doi nen la ca man hinh doi theo.
            .background(uiState.boardBackground.color())
            // Status bar dang bi an nhung khong cho noi dung tran len cho cua no: dai do hay
            // co camera / notch. IgnoringVisibility = van chua cho du thanh do dang an. Con
            // cho nav bar thi khay dung luon: nav cung dang an.
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
    ) {
        // Le hai ben nam o tung phan chu khong o ca cot: khay manh phai cham duoc hai mep
        // man hinh, dat le o cot thi no bi thut vao theo.
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SCREEN_PADDING)
                    // Thanh cong cu chi con icon tran: mot lop nen mo de icon khong lan vao
                    // manh ghep phia sau, van du trong de thay mau nen ban choi.
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = TOP_BAR_ALPHA),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.ic_back), contentDescription = null)
                }
                IconButton(onClick = onCleanRequested, enabled = uiState.canClean) {
                    Icon(
                        painter = painterResource(R.drawable.ic_clean),
                        contentDescription = stringResource(R.string.action_clean_pieces)
                    )
                }
                Text(
                    text = stringResource(R.string.game_score, uiState.score),
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    TextButton(onClick = onHintRequested, enabled = uiState.canUseHint) {
                        Text(text = stringResource(R.string.action_hint, uiState.hintsLeft))
                    }
                }
                IconButton(onClick = onToggleEdgePiecesOnly) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edge_pieces),
                        contentDescription = stringResource(
                            if (uiState.edgePiecesOnly) {
                                R.string.action_show_all_pieces
                            } else {
                                R.string.action_show_edge_pieces
                            }
                        ),
                        // Khong con nen nut de bao trang thai bat/tat: doi mau icon.
                        tint = if (uiState.edgePiecesOnly) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current
                        }
                    )
                }
                IconButton(onClick = { showBackgrounds = !showBackgrounds }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_background),
                        contentDescription = stringResource(R.string.action_change_background),
                        tint = if (showBackgrounds) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current
                        }
                    )
                }
                IconButton(onClick = { showPeek = !showPeek }) {
                    Icon(
                        painter = painterResource(
                            if (showPeek) R.drawable.ic_eye_slash else R.drawable.ic_eye
                        ),
                        contentDescription = stringResource(
                            if (showPeek) R.string.action_hide_image else R.string.action_show_image
                        ),
                        tint = if (showPeek) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current
                        }
                    )
                }
            }

            if (showBackgrounds) {
                BackgroundPicker(
                    selected = uiState.boardBackground,
                    onSelected = onBackgroundSelected,
                    modifier = Modifier.padding(top = 4.dp, start = SCREEN_PADDING, end = SCREEN_PADDING)
                )
            }

            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                uiState.errorMessageRes != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(text = stringResource(uiState.errorMessageRes)) }

                uiState.playState != null && image != null -> {
                    Text(
                        text = if (uiState.isSolved) {
                            stringResource(R.string.game_solved)
                        } else {
                            stringResource(
                                R.string.game_progress,
                                uiState.placedCount,
                                uiState.totalPieces
                            )
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isSolved) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(
                            vertical = 4.dp,
                            horizontal = SCREEN_PADDING
                        )
                    )

                    PlayArea(
                        playState = uiState.playState,
                        image = image,
                        draggingPieceId = uiState.draggingPieceId,
                        lastMovedPieceId = uiState.lastMovedPieceId,
                        hintPieceId = uiState.hintPieceId,
                        onHintPieceLanded = onHintPieceLanded,
                        cleaningPieceIds = uiState.cleaningPieceIds,
                        onCleanPieceLanded = onCleanPieceLanded,
                        droppedPieceIds = uiState.droppedPieceIds,
                        onPieceDragStart = onPieceDragStart,
                        onPieceDragEnd = onPieceDragEnd,
                        onPieceDragSnap = onPieceDragSnap,
                        onPieceReturnedToTray = onPieceReturnedToTray,
                        onTrayPieceMoved = onTrayPieceMoved,
                        onTrayPieceDropped = onTrayPieceDropped,
                        onPlayAreaMeasured = onPlayAreaMeasured,
                        onBoardSizeMeasured = { boardSizePx = it },
                        edgePiecesOnly = uiState.edgePiecesOnly,
                        selectedTrayPieceIds = uiState.selectedTrayPieceIds,
                        onTrayPieceTapped = onTrayPieceTapped,
                        onSelectedPiecesReleased = onSelectedPiecesReleased,
                        trayExpanded = uiState.trayExpanded,
                        onTrayExpandedChange = onTrayExpandedChange,
                        boardBackground = uiState.boardBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Anh mau noi tren cung: phai o ngoai Column de keo duoc ra khap man hinh.
        if (showPeek && image != null) {
            PeekWindow(
                image = image,
                maxSize = with(LocalDensity.current) { boardSizePx.toDp() },
                onClose = { showPeek = false }
            )
        }
    }
}

/**
 * Le hai ben cua thanh cong cu, bang chon nen va dong tien do. Khay manh khong dung le nay:
 * no tran het be ngang man hinh.
 */
private val SCREEN_PADDING = 12.dp

/** Do toi cua lop phu sau hop manh: du de hop noi len, van con thay ban co mo mo. */
private const val TRAY_SCRIM_ALPHA = 0.5f

/** Do duc cua lop nen thanh cong cu: du de doc icon, van nhin xuyen thay mau nen ban choi. */
private const val TOP_BAR_ALPHA = 0.32f

/** Canh mot o mau trong bang chon nen. */
private val SWATCH_SIZE = 36.dp

/**
 * Bang chon nen ban choi: moi lua chon la mot o mau, o dang dung co vien day. Chon la doi
 * ngay va duoc ghi vao cai dat nen van sau van giu nen do.
 */
@Composable
private fun BackgroundPicker(
    selected: BoardBackground,
    onSelected: (BoardBackground) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoardBackground.entries.forEach { background ->
            val isSelected = background == selected
            Box(
                modifier = Modifier
                    .size(SWATCH_SIZE)
                    .clip(CircleShape)
                    .background(background.color())
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            AnhnnTheme.extraColors.border
                        },
                        shape = CircleShape
                    )
                    .clickable(onClickLabel = stringResource(background.labelRes)) {
                        onSelected(background)
                    }
            )
        }
    }
}

/** Canh cua so anh mau luc moi mo: nho de khong che cho ghep, van du to de nhan ra chi tiet. */
private val PEEK_SIZE = 132.dp

/** Goc bo tron va co nut dong cua cua so anh mau. */
private val PEEK_CORNER = 12.dp
private val PEEK_CLOSE_SIZE = 28.dp

/** Cua so anh mau cung trong mot phan de khong che han manh ghep / mau nen phia sau. */
private const val PEEK_ALPHA = 0.85f

/**
 * Cua so anh mau: keo di duoc va chum hai ngon tay de zoom. To nhat bang [maxSize] (canh
 * khung ghep) de nguoi choi so anh mau voi ban co o cung mot co.
 */
@Composable
private fun PeekWindow(
    image: ImageBitmap,
    maxSize: Dp,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        // Chua do xong ban co (maxSize = 0) thi chi cho zoom trong pham vi man hinh.
        val largest = maxSize.coerceIn(PEEK_SIZE, minOf(maxWidth, maxHeight))
        var size by remember(largest) { mutableStateOf(PEEK_SIZE.coerceAtMost(largest)) }

        /** Goc tren-trai xa nhat de cua so canh [side] con nam trong man hinh. */
        fun limitOf(side: Dp) = with(density) {
            Offset(
                x = (maxWidth - side).toPx().coerceAtLeast(0f),
                y = (maxHeight - side).toPx().coerceAtLeast(0f)
            )
        }

        var offset by remember(maxWidth, maxHeight) {
            val limit = limitOf(PEEK_SIZE)
            mutableStateOf(Offset(limit.x, limit.y * 0.12f))
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .size(size)
                // alpha dat truoc cac lop ve de mo ca cua so (bong, nen lan anh mau).
                .alpha(PEEK_ALPHA)
                .shadow(8.dp, RoundedCornerShape(PEEK_CORNER))
                .clip(RoundedCornerShape(PEEK_CORNER))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, AnhnnTheme.extraColors.border, RoundedCornerShape(PEEK_CORNER))
                .pointerInput(largest, maxWidth, maxHeight) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val next = (size * zoom).coerceIn(PEEK_SIZE, largest)
                        // Zoom quanh tam cua so: goc tren-trai lui ra nua phan vua no them.
                        val grown = (next - size).toPx() / 2f
                        size = next
                        val limit = limitOf(next)
                        offset = Offset(
                            x = (offset.x + pan.x - grown).coerceIn(0f, limit.x),
                            y = (offset.y + pan.y - grown).coerceIn(0f, limit.y)
                        )
                    }
                }
        ) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(PEEK_CLOSE_SIZE)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.action_close_image),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Ban co o tren, khay cuon ngang o duoi, cong mot lop noi ve manh dang duoc keo tu khay
 * len ban co (khong the ve trong LazyRow vi manh phai di ra ngoai khay).
 */
@Composable
private fun PlayArea(
    playState: PuzzlePlayState,
    image: ImageBitmap,
    draggingPieceId: Int?,
    lastMovedPieceId: Int?,
    hintPieceId: Int?,
    onHintPieceLanded: (Int) -> Unit,
    cleaningPieceIds: List<Int>,
    onCleanPieceLanded: (Int) -> Unit,
    /** Cac manh vua duoc dua ca nhom tu khay len ban: chung hien ra kem mot nhip nay len. */
    droppedPieceIds: Set<Int>,
    onPieceDragStart: (Int) -> Unit,
    onPieceDragEnd: (Int, Float, Float) -> Unit,
    onPieceDragSnap: (Int, Float, Float) -> Unit,
    onPieceReturnedToTray: (pieceId: Int, index: Int) -> Unit,
    onTrayPieceMoved: (pieceId: Int, index: Int) -> Unit,
    onTrayPieceDropped: (Int, PieceOffset) -> Unit,
    onPlayAreaMeasured: (PieceBounds) -> Unit,
    onBoardSizeMeasured: (Float) -> Unit,
    /** Chi hien manh cua 4 canh trong khay. */
    edgePiecesOnly: Boolean,
    /** Cac manh dang cham chon trong khay, rong neu dang tat che do chon nhieu. */
    selectedTrayPieceIds: Set<Int>,
    /**
     * Cham vao manh trong khay. Trong hop manh luc nao cung cham chon duoc, con o hang khay
     * thu gon thi ViewModel bo qua neu nguoi choi dang tat che do chon nhieu.
     */
    onTrayPieceTapped: (Int) -> Unit,
    onSelectedPiecesReleased: () -> Unit,
    /** Khay dang mo thanh hop luoi cuon doc. */
    trayExpanded: Boolean,
    onTrayExpandedChange: (Boolean) -> Unit,
    /** Nen ban choi dang chon: khung ghep do mau theo no cho khoi chim vao nen. */
    boardBackground: BoardBackground,
    modifier: Modifier = Modifier
) {
    val rows = playState.puzzle.difficulty.rows
    val cols = playState.puzzle.difficulty.cols

    var areaOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardSizePx by remember { mutableFloatStateOf(0f) }
    // Canh ban co luc chua zoom: co manh trong khay do theo day nen zoom ban co khong lam
    // khay phong to theo.
    var baseBoardSizePx by remember { mutableFloatStateOf(0f) }
    // null = chua do xong khay, chua biet dau la ranh gioi khay. Day la ca cum khay (tinh ca
    // nut mui ten), dung de biet ngon tay dang o ban co hay da xuong khay.
    var trayOrigin by remember { mutableStateOf<Offset?>(null) }
    // Rieng vung danh sach manh: cho cua tung manh do theo goc nay. Hang khay va luoi trong
    // hop do rieng ra hai cho - do chung mot cho thi moi lan mo / dong hop lai phai gan lai
    // phep do cho ca hai danh sach, va chinh cu do lai do lam nhip mo hop khung mot cai.
    var trayRowOrigin by remember { mutableStateOf(Offset.Zero) }
    var trayRowSize by remember { mutableStateOf(IntSize.Zero) }
    var traySheetOrigin by remember { mutableStateOf(Offset.Zero) }
    var traySheetSize by remember { mutableStateOf(IntSize.Zero) }
    // Manh bay ve "khay" nghia la ve danh sach dang hien: hop dang mo thi la luoi trong hop.
    val trayListOrigin = if (trayExpanded) traySheetOrigin else trayRowOrigin
    val trayListSize = if (trayExpanded) traySheetSize else trayRowSize
    // Toa do ngon tay khi keo manh tu khay chi duoc doc trong lambda cua offset (pha layout):
    // doc no o pha composition thi moi frame keo se ve lai ca ban co lan khay.
    var trayDragPieceId by remember { mutableStateOf<Int?>(null) }
    val trayDragPosition = remember { mutableStateOf(Offset.Zero) }
    // Gesture cua khay giu nguyen lambda tu luc bat dau keo, nen van choi phai doc qua day
    // moi la van hien tai (xem onDragMove ben duoi).
    val latestPlayState = rememberUpdatedState(playState)
    val trayListState = rememberLazyListState()
    val trayGridState = rememberLazyGridState()
    // Doan truot cua hop manh: hop va lop phu cung doc no trong lambda cua graphicsLayer
    // nen ca nhip mo / dong khong recompose man choi lay mot frame nao.
    val traySheetState = rememberTraySheetState()
    // Hinh manh o co khay duoc nuong san mot lan: mo hop manh la ca luoi manh trong hop
    // hien ra cung luc, ve lai tung duong bao moi khung hinh thi nhip mo hop khung mot cai.
    val trayPieceAtlas = rememberTrayPieceAtlas(playState, image, baseBoardSizePx)
    LaunchedEffect(trayExpanded, traySheetState) { traySheetState.animateTo(trayExpanded) }
    // Manh dang bay (goi y hoac dang duoc don ve khay): ban co de trong cho cua no, lop noi
    // ben tren ve no.
    val flyingPieceIds = remember(hintPieceId, cleaningPieceIds) {
        cleaningPieceIds.toSet() + setOfNotNull(hintPieceId)
    }

    /** Toa do ban co cua o manh sao cho tam manh nam duoi ngon tay. */
    fun boardPositionOf(position: Offset) = PieceOffset(
        x = (position.x - boardOrigin.x - boardSizePx / cols / 2) / boardSizePx,
        y = (position.y - boardOrigin.y - boardSizePx / rows / 2) / boardSizePx
    )

    // Khay co the dang loc (chi manh vien): danh sach hien ra khac thu tu that cua khay nen
    // moi cho chen doc tu layout phai doi lai sang thu tu that truoc khi bao ve ViewModel.
    val trayPieces = remember(playState.trayOrder, playState.puzzle, edgePiecesOnly) {
        val all = playState.trayPieces
        if (edgePiecesOnly) all.filter { playState.isEdgePiece(it.id) } else all
    }

    /** Cho chen trong khay that, tinh tu cho chen [visibleIndex] trong danh sach dang hien. */
    fun trayOrderIndexOf(visibleIndex: Int): Int {
        if (trayPieces.size == playState.trayOrder.size) return visibleIndex
        val pieceId = trayPieces.getOrNull(visibleIndex)?.id ?: return playState.trayOrder.size
        return playState.trayOrder.indexOf(pieceId).coerceAtLeast(0)
    }

    /** Cho chen trong danh sach dang hien ung voi diem tha: truoc hay sau manh dang o do. */
    fun trayIndexAt(position: Offset): Int {
        val local = position - trayListOrigin
        if (trayExpanded) {
            // Hop mo rong xep theo cot nen khong chi nhin toa do ngang duoc: lay manh co tam
            // gan diem tha nhat roi xem diem tha nam truoc hay sau tam no.
            val visible = trayGridState.layoutInfo.visibleItemsInfo
            val item = visible.minByOrNull { info ->
                val cx = info.offset.x + info.size.width / 2f
                val cy = info.offset.y + info.size.height / 2f
                (local.x - cx) * (local.x - cx) + (local.y - cy) * (local.y - cy)
            } ?: return 0
            return item.index + if (local.x > item.offset.x + item.size.width / 2f) 1 else 0
        }
        val visible = trayListState.layoutInfo.visibleItemsInfo
        val item = visible.firstOrNull { local.x < it.offset + it.size }
            ?: return visible.lastOrNull()?.let { it.index + 1 } ?: 0
        return item.index + if (local.x > item.offset + item.size / 2f) 1 else 0
    }

    Box(modifier = modifier.onGloballyPositioned { areaOrigin = it.positionInRoot() }) {
        Column(modifier = Modifier.fillMaxSize()) {
            JigsawBoardView(
                playState = playState,
                image = image,
                draggingPieceId = draggingPieceId,
                lastMovedPieceId = lastMovedPieceId,
                flyingPieceIds = flyingPieceIds,
                poppingPieceIds = droppedPieceIds,
                boardColors = boardBackground.slotColors(),
                onBoardMeasured = { origin, sizePx, baseSizePx, bounds ->
                    boardOrigin = origin
                    boardSizePx = sizePx
                    baseBoardSizePx = baseSizePx
                    onBoardSizeMeasured(sizePx)
                    onPlayAreaMeasured(bounds)
                },
                onPieceDragStart = onPieceDragStart,
                onPieceDragSnap = onPieceDragSnap,
                onPieceDragEnd = { pieceId, dx, dy, finger ->
                    // Tha ngon tay xuong khay = tra manh ve danh sach, dung cho vua tha.
                    val overTray = trayOrigin?.let { finger.y >= it.y } == true
                    if (overTray && playState.canReturnToTray(pieceId)) {
                        onPieceReturnedToTray(pieceId, trayOrderIndexOf(trayIndexAt(finger)))
                    } else {
                        onPieceDragEnd(pieceId, dx, dy)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .zIndex(if (draggingPieceId != null) 1f else 0f)
            )

            // Khay chi giu cho san o day; no duoc ve o lop noi ben tren de khi mo rong thi
            // de len ban co chu khong day ban co nho lai.
            Spacer(modifier = Modifier.height(TRAY_COLLAPSED_HEIGHT + 12.dp))
        }

        // Hop manh dang mo: lam toi ca man hinh cho nguoi choi nhin vao hop, cham ra ngoai
        // hop la dong hop lai. Lop phu toi dan dung theo doan hop da truot - keo hop xuong
        // nua chung thi man hinh cung sang lai nua chung.
        //
        // Lop phu khong dung o mep tren cua vung choi ma dang len het ca thanh cong cu: mo
        // hop manh la ca man hinh toi lai, dung kieu mot bottom sheet that. Vung choi duoc
        // ve sau thanh cong cu trong cung mot cot nen no phu duoc len tren.
        val areaTop = with(LocalDensity.current) { areaOrigin.y.toDp() }
        Box(
            modifier = Modifier
                .zIndex(1f)
                .offset(y = -areaTop)
                .fillMaxWidth()
                .height(LocalConfiguration.current.screenHeightDp.dp + areaTop)
                .graphicsLayer { alpha = traySheetState.progress }
                .background(Color.Black.copy(alpha = TRAY_SCRIM_ALPHA))
                // Hop dong roi thi lop phu trong suot va khong nhan cham nua: ban co phia
                // sau lai bam duoc binh thuong.
                .then(
                    if (trayExpanded) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTrayExpandedChange(false) }
                    } else {
                        Modifier
                    }
                )
        )

        // Nut dua ca nhom manh len ban choi, nam duoi cung cho ngon tay khong phai di xa
        // manh vua cham. Trong hop nut luon hien (con mo hop la con dang chon manh); o khay
        // thu gon thi chi hien khi da cham chon manh nao do.
        val releaseButton: @Composable ColumnScope.() -> Unit = {
            Button(
                onClick = onSelectedPiecesReleased,
                enabled = selectedTrayPieceIds.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp, bottom = 12.dp)
            ) {
                Text(
                    text = if (selectedTrayPieceIds.isEmpty()) {
                        stringResource(R.string.action_release_none)
                    } else {
                        stringResource(
                            R.string.action_release_selected,
                            selectedTrayPieceIds.size
                        )
                    }
                )
            }
        }

        // Lop khay: neo duoi day man hinh va nam tren ban co, nen khay mo rong de len ban
        // co kieu bottom sheet chu khong lam ban co doi kich thuoc.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f)
        ) {
            // Khay luon hien, ke ca khi trong: no la cho de tha manh tu ban co ve.
            JigsawTrayView(
                playState = playState,
                pieces = trayPieces,
                image = image,
                draggedPieceId = trayDragPieceId,
                hintPieceId = hintPieceId,
                listState = trayListState,
                pieceAtlas = trayPieceAtlas,
                boardSizePx = baseBoardSizePx,
                onExpandedChange = onTrayExpandedChange,
                onListMeasured = { origin, size ->
                    trayRowOrigin = origin
                    trayRowSize = size
                },
                selectedPieceIds = selectedTrayPieceIds,
                onPieceTapped = onTrayPieceTapped,
                onDragStart = { pieceId, position ->
                    trayDragPosition.value = position
                    trayDragPieceId = pieceId
                },
                onDragMove = { position ->
                    trayDragPosition.value = position
                    // Keo manh tu khay den gan dung o (hay gan mot manh ke ben) thi no vao
                    // cho ngay giua duong, khong doi nhac tay.
                    val pieceId = trayDragPieceId
                    val overBoard = position.y < (trayOrigin?.y ?: Float.MAX_VALUE)
                    if (pieceId != null && boardSizePx > 0f && overBoard) {
                        val boardPosition = boardPositionOf(position)
                        val snap = latestPlayState.value
                            .trayDragSnapOffset(pieceId, boardPosition)
                        if (snap != null) {
                            // Luot keo ket thuc tai day: onDragEnd sau do thay khong con
                            // manh nao dang keo nen khong dat them mot lan nua.
                            trayDragPieceId = null
                            onTrayPieceDropped(pieceId, boardPosition + snap)
                        }
                    }
                },
                onDragEnd = { position ->
                    val pieceId = trayDragPieceId
                    trayDragPieceId = null
                    val overBoard = position.y < (trayOrigin?.y ?: Float.MAX_VALUE)
                    if (pieceId != null && boardSizePx > 0f && overBoard) {
                        onTrayPieceDropped(pieceId, boardPositionOf(position))
                    } else if (pieceId != null) {
                        // Tha lai trong khay = doi cho: manh ve dung cho vua tha.
                        onTrayPieceMoved(pieceId, trayOrderIndexOf(trayIndexAt(position)))
                    }
                },
                modifier = Modifier
                    // Khay cham han day man hinh va hai mep hai ben: thu gon hay mo hop thi
                    // no van la mot tam lien mach voi canh man hinh.
                    .padding(top = 8.dp)
                    .onGloballyPositioned { trayOrigin = it.positionInRoot() },
                footer = { if (selectedTrayPieceIds.isNotEmpty()) releaseButton() }
            )
        }

        // Hop manh nam tren cung: no de len ca khay lan lop phu lam toi man hinh.
        JigsawTraySheet(
            playState = playState,
            pieces = trayPieces,
            image = image,
            draggedPieceId = trayDragPieceId,
            hintPieceId = hintPieceId,
            sheetState = traySheetState,
            pieceAtlas = trayPieceAtlas,
            onExpandedChange = onTrayExpandedChange,
            boardSizePx = baseBoardSizePx,
            gridState = trayGridState,
            selectedPieceIds = selectedTrayPieceIds,
            onPieceTapped = onTrayPieceTapped,
            onListMeasured = { origin, size ->
                traySheetOrigin = origin
                traySheetSize = size
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(3f),
            footer = releaseButton
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Manh dang bay / dang keo phai o tren cung: hop manh khong duoc che no.
                .zIndex(4f)
        ) {
            val trayTop = trayOrigin
            if (trayTop != null && boardSizePx > 0f) {
                val density = LocalDensity.current
                val boardSize = with(density) { boardSizePx.toDp() }
                val slotWidth = boardSize / cols
                val slotHeight = boardSize / rows
                val margin = maxOf(slotWidth, slotHeight) * TAB_RATIO
                val marginPx = with(density) { margin.toPx() }
                val frameWidth = with(density) { (slotWidth + margin * 2).toPx() }
                val frameHeight = with(density) { (slotHeight + margin * 2).toPx() }
                // Manh trong khay ve nho hon manh tren ban co, nen manh bay giua hai noi vua bay
                // vua doi kich thuoc.
                // Manh trong khay do theo canh ban co chua zoom, con manh bay do theo canh dang
                // hien thi, nen ti le nay phai lay ca hai.
                val trayScale = trayBoardSizePx(baseBoardSizePx, rows, cols) / boardSizePx

                /** Goc tren-trai cua khung manh khi manh nam o [position] tren ban co. */
                fun boardFrameOf(position: PieceOffset) = Offset(
                    x = boardOrigin.x - areaOrigin.x + position.x * boardSizePx - marginPx,
                    y = boardOrigin.y - areaOrigin.y + position.y * boardSizePx - marginPx
                )

                /**
                 * Goc tren-trai cua khung manh khi manh nam o [left] / [top] trong vung danh
                 * sach cua khay. [top] de trong = dat giua chieu cao khay (khay mot hang).
                 */
                fun trayFrameAt(left: Float, top: Float? = null) = Offset(
                    x = trayListOrigin.x - areaOrigin.x + left,
                    y = trayListOrigin.y - areaOrigin.y +
                        (top ?: ((trayListSize.height - frameHeight * trayScale) / 2f))
                )

                /** Cho cua manh trong khay; manh da cuon ra ngoai thi lay giua khay. */
                fun traySlotOf(pieceId: Int): Offset {
                    val middle = (trayListSize.width - frameWidth * trayScale) / 2f
                    if (trayExpanded) {
                        val slot = trayGridState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.key == pieceId } ?: return trayFrameAt(middle)
                        // O trong hop rong hon khung manh mot chut: dat manh vao giua o cua no.
                        return trayFrameAt(
                            left = slot.offset.x + (slot.size.width - frameWidth * trayScale) / 2f,
                            top = slot.offset.y + (slot.size.height - frameHeight * trayScale) / 2f
                        )
                    }
                    val slot = trayListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.key == pieceId }
                    return trayFrameAt(slot?.offset?.toFloat() ?: middle)
                }

                /**
                 * Cuoi khay - dich cua manh duoc don ve. Khay dai hon man hinh thi cuoi khay nam
                 * ngoai vung thay duoc, luc do manh bay den mep phai khay roi bien vao danh sach.
                 */
                fun trayEnd(): Offset {
                    val maxLeft = trayListSize.width - frameWidth * trayScale
                    if (trayExpanded) {
                        val last = trayGridState.layoutInfo.visibleItemsInfo.lastOrNull()
                        return trayFrameAt(
                            left = (last?.let { (it.offset.x + it.size.width).toFloat() } ?: 0f)
                                .coerceAtMost(maxLeft),
                            top = last?.let {
                                it.offset.y + (it.size.height - frameHeight * trayScale) / 2f
                            }
                        )
                    }
                    val last = trayListState.layoutInfo.visibleItemsInfo.lastOrNull()
                    val left = last?.let { (it.offset + it.size).toFloat() } ?: 0f
                    return trayFrameAt(left.coerceAtMost(maxLeft))
                }

                hintPieceId?.let { flyingId ->
                    val piece = playState.puzzle.pieces.firstOrNull { it.id == flyingId }
                    if (piece != null) {
                        val inTray = playState.isInTray(flyingId)
                        FlyingPiece(
                            piece = piece,
                            image = image,
                            rows = rows,
                            cols = cols,
                            boardSizePx = boardSizePx,
                            slotWidth = slotWidth,
                            slotHeight = slotHeight,
                            margin = margin,
                            start = if (inTray) {
                                traySlotOf(flyingId)
                            } else {
                                boardFrameOf(playState.placements.getValue(flyingId).position)
                            },
                            startScale = if (inTray) trayScale else 1f,
                            end = boardFrameOf(playState.targetOf(piece)),
                            endScale = 1f,
                            onLanded = { onHintPieceLanded(flyingId) }
                        )
                    }
                }

                // Nut don: cac manh roi le cung bay ve cuoi khay, lech nhip nhau cho de nhin.
                cleaningPieceIds.forEachIndexed { index, cleanedId ->
                    key(cleanedId) {
                        val piece = playState.puzzle.pieces.firstOrNull { it.id == cleanedId }
                        val placement = playState.placements[cleanedId]
                        if (piece != null && placement != null && !placement.isInTray) {
                            FlyingPiece(
                                piece = piece,
                                image = image,
                                rows = rows,
                                cols = cols,
                                boardSizePx = boardSizePx,
                                slotWidth = slotWidth,
                                slotHeight = slotHeight,
                                margin = margin,
                                start = boardFrameOf(placement.position),
                                startScale = 1f,
                                end = trayEnd(),
                                endScale = trayScale,
                                durationMillis = CLEAN_FLIGHT_MILLIS,
                                delayMillis = index * CLEAN_STAGGER_MILLIS,
                                endAlpha = CLEAN_LANDING_ALPHA,
                                onLanded = { onCleanPieceLanded(cleanedId) }
                            )
                        }
                    }
                }
            }

            trayDragPieceId?.let { draggedId ->
                val piece = playState.puzzle.pieces.firstOrNull { it.id == draggedId }
                if (piece != null && boardSizePx > 0f) {
                    val boardSize = with(LocalDensity.current) { boardSizePx.toDp() }
                    val slotWidth = boardSize / cols
                    val slotHeight = boardSize / rows
                    val margin = maxOf(slotWidth, slotHeight) * TAB_RATIO
                    JigsawPieceView(
                        piece = piece,
                        image = image,
                        rows = rows,
                        cols = cols,
                        boardSizePx = boardSizePx,
                        slotWidth = slotWidth,
                        slotHeight = slotHeight,
                        margin = margin,
                        isPlaced = false,
                        modifier = Modifier.graphicsLayer {
                            val position = trayDragPosition.value
                            translationX = position.x - areaOrigin.x -
                                    boardSizePx / cols / 2 - margin.toPx()
                            translationY = position.y - areaOrigin.y -
                                    boardSizePx / rows / 2 - margin.toPx()
                        }
                    )
                }
        }
        }
    }
}

/**
 * Mot manh bay tu [start] den [end]: manh goi y bay vao dung o cua no, hay manh roi le duoc
 * don ve khay. Bay xong moi bao [onLanded] - luc do man hinh moi thuc su doi cho manh.
 *
 * Manh luon duoc ve o ti le ban co roi phong bang [startScale] / [endScale], nen dau bay tu
 * khay hay ve khay thi no cung khop voi kich thuoc manh trong khay.
 */
@Composable
private fun FlyingPiece(
    piece: JigsawPiece,
    image: ImageBitmap,
    rows: Int,
    cols: Int,
    boardSizePx: Float,
    slotWidth: Dp,
    slotHeight: Dp,
    margin: Dp,
    start: Offset,
    startScale: Float,
    end: Offset,
    endScale: Float,
    onLanded: () -> Unit,
    durationMillis: Int = HINT_FLIGHT_MILLIS,
    delayMillis: Int = 0,
    endAlpha: Float = 1f
) {
    val progress = remember(piece.id) { Animatable(0f) }
    // Duong bay duoc chot luc cat canh: cho den trong khay xe dich khi cac manh khac ha
    // canh, doc lai moi frame thi manh dang bay bi giat sang cho moi.
    val from = remember(piece.id) { start }
    val to = remember(piece.id) { end }
    val landed by rememberUpdatedState(onLanded)
    LaunchedEffect(piece.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis, delayMillis, FastOutSlowInEasing)
        )
        landed()
    }

    JigsawPieceView(
        piece = piece,
        image = image,
        rows = rows,
        cols = cols,
        boardSizePx = boardSizePx,
        slotWidth = slotWidth,
        slotHeight = slotHeight,
        margin = margin,
        isPlaced = false,
        // Dich o pha ve nhu manh dang duoc keo: bay khong phai do lai cay layout.
        modifier = Modifier.graphicsLayer {
            val moved = progress.value
            transformOrigin = TransformOrigin(0f, 0f)
            val scale = startScale + (endScale - startScale) * moved
            scaleX = scale
            scaleY = scale
            // Chi mo dan o doan cuoi: mo ngay tu dau thi manh bien mat giua duong bay,
            // nguoi choi khong theo duoc no ve dau.
            val fade = ((moved - FADE_START) / (1f - FADE_START)).coerceIn(0f, 1f)
            alpha = 1f + (endAlpha - 1f) * fade
            translationX = from.x + (to.x - from.x) * moved
            translationY = from.y + (to.y - from.y) * moved
        }
    )
}

/** Thoi gian mot manh goi y bay vao o cua no. */
private const val HINT_FLIGHT_MILLIS = 450

/** Thoi gian mot manh bay tu ban co ve khay khi bam nut don. */
private const val CLEAN_FLIGHT_MILLIS = 320

/** Do lech nhip giua hai manh lien nhau trong luot don, de chung khong bay dinh nhau. */
private const val CLEAN_STAGGER_MILLIS = 45

/** Phan duong bay da di qua truoc khi manh bat dau mo dan. */
private const val FADE_START = 0.55f

/**
 * Do dam cua manh don luc cham khay. Khong ve 0 han: cho cua no trong khay hien len ngay
 * sau do (fade cua LazyRow), tat han truoc thi giua hai buoc co mot nhip trong.
 */
private const val CLEAN_LANDING_ALPHA = 0.25f
