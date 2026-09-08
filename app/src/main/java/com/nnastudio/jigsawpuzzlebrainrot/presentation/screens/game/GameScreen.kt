package com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceBounds
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPiece
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceOffset
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.AnhnnGradientButton
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.JigsawBoardView
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.JigsawPieceView
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.JigsawTrayView
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.TAB_RATIO
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.trayBoardSizePx
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.GameUiState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.GameViewModel
import com.nnastudio.jigsawpuzzlebrainrot.utils.formatAsClock
import com.nnastudio.jigsawpuzzlebrainrot.utils.toImageBitmap

@Composable
fun GameScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GameContent(
        uiState = uiState,
        onPieceDragStart = viewModel::onPieceDragStart,
        onPieceDragEnd = viewModel::onPieceDragEnd,
        onPieceDragSnap = viewModel::onPieceSnappedWhileDragging,
        onPieceReturnedToTray = viewModel::onPieceReturnedToTray,
        onTrayPieceSelected = viewModel::onTrayPieceSelected,
        onTrayPieceMoved = viewModel::onTrayPieceMoved,
        onTrayPieceDropped = viewModel::onTrayPieceDropped,
        onPlayAreaMeasured = viewModel::onPlayAreaMeasured,
        onHintRequested = viewModel::onHintRequested,
        onHintPieceLanded = viewModel::onHintPieceLanded,
        onCleanRequested = viewModel::onCleanRequested,
        onCleanPieceLanded = viewModel::onCleanPieceLanded,
        onTogglePause = viewModel::onTogglePause,
        onRestart = viewModel::startNewGame,
        onBack = onBack
    )
}

@Composable
private fun GameContent(
    uiState: GameUiState,
    onPieceDragStart: (Int) -> Unit,
    onPieceDragEnd: (Int, Float, Float) -> Unit,
    onPieceDragSnap: (Int, Float, Float) -> Unit,
    onPieceReturnedToTray: (pieceId: Int, index: Int) -> Unit,
    onTrayPieceSelected: (Int) -> Unit,
    onTrayPieceMoved: (pieceId: Int, index: Int) -> Unit,
    onTrayPieceDropped: (Int, PieceOffset) -> Unit,
    onPlayAreaMeasured: (PieceBounds) -> Unit,
    onHintRequested: () -> Unit,
    onHintPieceLanded: (Int) -> Unit,
    onCleanRequested: () -> Unit,
    onCleanPieceLanded: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Icon(painter = painterResource(R.drawable.ic_back), contentDescription = null)
            }
            TextButton(onClick = onCleanRequested, enabled = uiState.canClean) {
                Icon(
                    painter = painterResource(R.drawable.ic_clean),
                    contentDescription = stringResource(R.string.action_clean_pieces)
                )
            }
            Text(
                text = uiState.elapsedSeconds.formatAsClock(),
                style = MaterialTheme.typography.titleLarge
            )
            Row {
                TextButton(onClick = onHintRequested, enabled = uiState.canUseHint) {
                    Text(text = stringResource(R.string.action_hint, uiState.hintsLeft))
                }
            }
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

            uiState.playState != null && uiState.artwork != null -> {
                val image = remember(uiState.artwork) { uiState.artwork.toImageBitmap() }

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
                    modifier = Modifier.padding(vertical = 4.dp)
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
                    onPieceDragStart = onPieceDragStart,
                    onPieceDragEnd = onPieceDragEnd,
                    onPieceDragSnap = onPieceDragSnap,
                    onPieceReturnedToTray = onPieceReturnedToTray,
                    onTrayPieceSelected = onTrayPieceSelected,
                    onTrayPieceMoved = onTrayPieceMoved,
                    onTrayPieceDropped = onTrayPieceDropped,
                    onPlayAreaMeasured = onPlayAreaMeasured,
                    modifier = Modifier.weight(1f)
                )

                AnhnnGradientButton(
                    text = stringResource(R.string.action_new_game),
                    onClick = onRestart,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 12.dp)
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
    onPieceDragStart: (Int) -> Unit,
    onPieceDragEnd: (Int, Float, Float) -> Unit,
    onPieceDragSnap: (Int, Float, Float) -> Unit,
    onPieceReturnedToTray: (pieceId: Int, index: Int) -> Unit,
    onTrayPieceSelected: (Int) -> Unit,
    onTrayPieceMoved: (pieceId: Int, index: Int) -> Unit,
    onTrayPieceDropped: (Int, PieceOffset) -> Unit,
    onPlayAreaMeasured: (PieceBounds) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = playState.puzzle.difficulty.rows
    val cols = playState.puzzle.difficulty.cols

    var areaOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardSizePx by remember { mutableFloatStateOf(0f) }
    // null = chua do xong khay, chua biet dau la ranh gioi khay.
    var trayOrigin by remember { mutableStateOf<Offset?>(null) }
    var traySize by remember { mutableStateOf(IntSize.Zero) }
    // Toa do ngon tay khi keo manh tu khay chi duoc doc trong lambda cua offset (pha layout):
    // doc no o pha composition thi moi frame keo se ve lai ca ban co lan khay.
    var trayDragPieceId by remember { mutableStateOf<Int?>(null) }
    val trayDragPosition = remember { mutableStateOf(Offset.Zero) }
    val trayListState = rememberLazyListState()
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

    /** Cho chen trong danh sach khay ung voi diem tha: truoc hay sau manh dang o do. */
    fun trayIndexAt(x: Float): Int {
        val localX = x - (trayOrigin?.x ?: 0f)
        val visible = trayListState.layoutInfo.visibleItemsInfo
        val item = visible.firstOrNull { localX < it.offset + it.size }
            ?: return visible.lastOrNull()?.let { it.index + 1 } ?: 0
        return item.index + if (localX > item.offset + item.size / 2f) 1 else 0
    }

    Box(modifier = modifier.onGloballyPositioned { areaOrigin = it.positionInRoot() }) {
        Column(modifier = Modifier.fillMaxSize()) {
            JigsawBoardView(
                playState = playState,
                image = image,
                draggingPieceId = draggingPieceId,
                lastMovedPieceId = lastMovedPieceId,
                flyingPieceIds = flyingPieceIds,
                onBoardMeasured = { origin, sizePx, bounds ->
                    boardOrigin = origin
                    boardSizePx = sizePx
                    onPlayAreaMeasured(bounds)
                },
                onPieceDragStart = onPieceDragStart,
                onPieceDragSnap = onPieceDragSnap,
                onPieceDragEnd = { pieceId, dx, dy, finger ->
                    // Tha ngon tay xuong khay = tra manh ve danh sach, dung cho vua tha.
                    val overTray = trayOrigin?.let { finger.y >= it.y } == true
                    if (overTray && playState.canReturnToTray(pieceId)) {
                        onPieceReturnedToTray(pieceId, trayIndexAt(finger.x))
                    } else {
                        onPieceDragEnd(pieceId, dx, dy)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    // Manh keo tu ban co chi dich bang graphicsLayer nen no van thuoc lop ban
                    // co; khay ve sau se de len no. Nang ca lop ban co trong luc keo de manh
                    // di qua duoc thanh khay. Het keo thi ha xuong, tra cho khay nhan cham.
                    .zIndex(if (draggingPieceId != null) 1f else 0f)
            )

            // Khay luon hien, ke ca khi trong: no la cho de tha manh tu ban co ve.
            JigsawTrayView(
                playState = playState,
                image = image,
                draggedPieceId = trayDragPieceId,
                hintPieceId = hintPieceId,
                listState = trayListState,
                boardSizePx = boardSizePx,
                onPieceSelected = onTrayPieceSelected,
                onDragStart = { pieceId, position ->
                    trayDragPosition.value = position
                    trayDragPieceId = pieceId
                },
                onDragMove = { position -> trayDragPosition.value = position },
                onDragEnd = { position ->
                    val pieceId = trayDragPieceId
                    trayDragPieceId = null
                    val overBoard = position.y < (trayOrigin?.y ?: Float.MAX_VALUE)
                    if (pieceId != null && boardSizePx > 0f && overBoard) {
                        onTrayPieceDropped(pieceId, boardPositionOf(position))
                    } else if (pieceId != null) {
                        // Tha lai trong khay = doi cho: manh ve dung cho vua tha.
                        onTrayPieceMoved(pieceId, trayIndexAt(position.x))
                    }
                },
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .onGloballyPositioned {
                        trayOrigin = it.positionInRoot()
                        traySize = it.size
                    }
            )
        }

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
            val trayScale = trayBoardSizePx(boardSizePx, rows, cols) / boardSizePx

            /** Goc tren-trai cua khung manh khi manh nam o [position] tren ban co. */
            fun boardFrameOf(position: PieceOffset) = Offset(
                x = boardOrigin.x - areaOrigin.x + position.x * boardSizePx - marginPx,
                y = boardOrigin.y - areaOrigin.y + position.y * boardSizePx - marginPx
            )

            /** Goc tren-trai cua khung manh khi manh nam o [left] trong khay. */
            fun trayFrameAt(left: Float) = Offset(
                x = trayTop.x - areaOrigin.x + left,
                y = trayTop.y - areaOrigin.y + (traySize.height - frameHeight * trayScale) / 2f
            )

            /** Cho cua manh trong khay; manh da cuon ra ngoai thi lay giua khay. */
            fun traySlotOf(pieceId: Int): Offset {
                val slot = trayListState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.key == pieceId }
                return trayFrameAt(
                    slot?.offset?.toFloat() ?: ((traySize.width - frameWidth * trayScale) / 2f)
                )
            }

            /**
             * Cuoi khay - dich cua manh duoc don ve. Khay dai hon man hinh thi cuoi khay nam
             * ngoai vung thay duoc, luc do manh bay den mep phai khay roi bien vao danh sach.
             */
            val trayEnd = run {
                val last = trayListState.layoutInfo.visibleItemsInfo.lastOrNull()
                val left = last?.let { (it.offset + it.size).toFloat() } ?: 0f
                trayFrameAt(left.coerceAtMost(traySize.width - frameWidth * trayScale))
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
                // key() phai o ngoai cung moi vong lap: de trong if thi khi manh dau ha canh
                // (danh sach ngan lai), Compose khong nhan ra cac manh con lai la manh cu nen
                // dung lai chung tu dau - chung nhay ve ban co roi bay lai mot lan nua.
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
                            end = trayEnd,
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
                    // Dich o pha ve (khong phai offset o pha layout) de moi frame keo khong
                    // phai do lai ca cay layout.
                    modifier = Modifier.graphicsLayer {
                        // Manh duoc ve o ti le ban co, tam manh dat duoi ngon tay.
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
