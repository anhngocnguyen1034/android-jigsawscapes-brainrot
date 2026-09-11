package com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.preview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPuzzle
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.AnhnnGradientButton
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.renderSolvedBoard
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.PuzzlePreviewUiState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.PuzzlePreviewViewModel
import com.nnastudio.jigsawpuzzlebrainrot.utils.toImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Man xem truoc mot buc anh truoc khi vao choi: anh nam giua man hinh duoi dang bo manh da
 * ghep hoan chinh, ben duoi la nut vao van - "choi tiep" neu con van dang do dang, khong
 * thi "bat dau".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PuzzlePreviewScreen(
    onPlay: (difficulty: Difficulty) -> Unit,
    onBack: () -> Unit,
    viewModel: PuzzlePreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PuzzlePreviewContent(
        modifier = Modifier
            // Thanh he thong dang bi an cho ca app, nhung cho status bar co the la notch /
            // camera nen van chua cho no.
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility),
        uiState = uiState,
        onPlay = onPlay,
        onDifficultySelected = viewModel::onDifficultySelected,
        onToggleFavorite = viewModel::onToggleFavorite,
        onBack = onBack
    )
}

@Composable
private fun PuzzlePreviewContent(
    uiState: PuzzlePreviewUiState,
    onPlay: (Difficulty) -> Unit,
    onDifficultySelected: (Difficulty) -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val image = remember(uiState.artwork) { uiState.artwork?.toImageBitmap() }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text(text = stringResource(R.string.action_back)) }
            Text(
                text = uiState.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Tim day = da nam trong bo suu tap (tab "Cua toi" tren man chinh).
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (uiState.isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Filled.FavoriteBorder
                    },
                    contentDescription = stringResource(
                        if (uiState.isFavorite) {
                            R.string.action_unfavorite
                        } else {
                            R.string.action_favorite
                        }
                    ),
                    tint = if (uiState.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        LocalContentColor.current
                    }
                )
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

            uiState.puzzle != null && image != null -> {
                SolvedBoard(
                    puzzle = uiState.puzzle,
                    image = image,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                // Dong tien do chi co o moc nao dang co ban luu. Luon chua san mot dong
                // o day: de no tu hien ra / mat di thi ca cot dich len xuong, buc anh
                // ben tren nhay theo moi lan doi moc.
                val progressStyle = MaterialTheme.typography.labelSmall
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(
                            with(LocalDensity.current) {
                                progressStyle.lineHeight.takeIf { it.isSp }?.toDp()
                                    ?: PROGRESS_LINE_HEIGHT
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    uiState.progressPercent?.let { percent ->
                        Text(
                            text = stringResource(R.string.preview_progress, percent),
                            style = progressStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                PieceCountPicker(
                    selected = uiState.difficulty,
                    onSelected = onDifficultySelected,
                    modifier = Modifier.padding(top = 8.dp)
                )

                AnhnnGradientButton(
                    text = stringResource(
                        if (uiState.hasSavedGame) {
                            R.string.action_continue_game
                        } else {
                            R.string.action_start_game
                        }
                    ),
                    onClick = { onPlay(uiState.difficulty) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        }
    }
}

/**
 * Chon so manh: cac moc cuon ngang qua mot khung dung giua man hinh. Moc nao truot vao
 * khung thi to len va duoc chon luon - khong phai bam them. Bam thang vao mot moc thi no
 * tu truot vao giua.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PieceCountPicker(
    selected: Difficulty,
    onSelected: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = Difficulty.PIECE_OPTIONS
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = options.indexOf(selected).coerceAtLeast(0)
    )
    val scope = rememberCoroutineScope()

    // Moc dang nam trong khung giua: moc co tam gan tam khung nhat.
    val centered by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            // Chua do xong danh sach thi giu nguyen moc dang chon, dung nhay ve moc dau.
            info.visibleItemsInfo.minByOrNull {
                abs(it.offset + it.size / 2f - center)
            }?.index ?: options.indexOf(selected).coerceAtLeast(0)
        }
    }
    // Cuon den dau la chon den do ngay lap tuc, khong cho ngon tay roi: keo tu 64 manh len
    // 400 manh thi anh xem truoc doi theo tung moc luot qua giua khung. Cat lai anh la viec
    // nang nhung no chay o luong nen (xem SolvedBoard) nen cu vuot van muot, anh chi chay
    // sau mot nhip.
    LaunchedEffect(centered) {
        options.getOrNull(centered)?.let { if (it != selected) onSelected(it) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(PICKER_HEIGHT)) {
        // Moc dau va moc cuoi cung phai vao duoc giua khung.
        val sidePadding = (maxWidth - PICKER_SLOT_WIDTH) / 2

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = PICKER_SLOT_WIDTH, height = PICKER_HEIGHT)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
        )

        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(horizontal = sidePadding),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items = options, key = { _, option -> option.id }) { index, option ->
                val isCentered = index == centered
                val scale by animateFloatAsState(
                    targetValue = if (isCentered) 1f else PICKER_SIDE_SCALE,
                    label = "pieceCountScale"
                )
                Box(
                    modifier = Modifier
                        .width(PICKER_SLOT_WIDTH)
                        .fillMaxHeight()
                        .clickable(role = Role.RadioButton) {
                            scope.launch { listState.animateScrollToItem(index) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.pieceCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isCentered) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                }
            }
        }
    }
}

/** Cao dong tien do khi kieu chu khong khai bao chieu cao dong. */
private val PROGRESS_LINE_HEIGHT = 16.dp

/** Cao cua vong chon so manh va be ngang mot moc (cung la be ngang khung giua). */
private val PICKER_HEIGHT = 64.dp
private val PICKER_SLOT_WIDTH = 88.dp

/** Moc nam ngoai khung giua thu nho lai de moc dang chon noi han len. */
private const val PICKER_SIDE_SCALE = 0.7f

/**
 * Buc anh ve duoi dang bo manh da ghep xong: tung manh duoc ve o dung o cua no nen thay ro
 * duong cat, giong het luc nguoi choi vua hoan thanh van.
 *
 * Ca ban co duoc nuong san thanh mot buc anh o luong nen roi moi dua len man hinh: 400
 * manh, moi manh mot lan clip, ve lai tung ay moi khung hinh thi doi moc so manh la giat.
 */
@Composable
private fun SolvedBoard(
    puzzle: JigsawPuzzle,
    image: ImageBitmap,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val board = minOf(maxWidth, maxHeight)
        val boardPx = with(density) { board.toPx() }

        // produceState giu lai ban co cu cho toi khi ban co moi nuong xong nen khong nhap
        // nhay giua hai lan doi moc.
        val rendered by produceState<RenderedBoard?>(null, puzzle, image, boardPx) {
            if (boardPx <= 0f) return@produceState
            value = withContext(Dispatchers.Default) {
                RenderedBoard(
                    boardPx = boardPx,
                    bitmap = renderSolvedBoard(
                        puzzle = puzzle,
                        image = image,
                        boardSizePx = boardPx,
                        density = density
                    )
                )
            }
        }

        Canvas(modifier = Modifier.size(board)) {
            val ready = rendered ?: return@Canvas
            // Ban co cu do cho khung co kich thuoc khac thi bo qua, cho ban co moi.
            if (ready.boardPx != boardPx) return@Canvas
            drawImage(ready.bitmap)
        }
    }
}

/** Ban co da nuong xong, kem canh da dung de nuong. */
private class RenderedBoard(val boardPx: Float, val bitmap: ImageBitmap)
