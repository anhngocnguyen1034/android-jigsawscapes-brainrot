package com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.AnhnnGradientButton
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.PuzzleCard
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnGradients
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.labelRes
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.HomeUiState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.HomeViewModel
import com.nnastudio.jigsawpuzzlebrainrot.utils.assetUri

/**
 * Do kho dung chung cho moi buc anh: 8x8. Sau nay tung buc anh se tu quy dinh do kho cua no,
 * nen man hinh chinh khong con cho nguoi choi chon ti le nua.
 */
private val PUZZLE_DIFFICULTY = Difficulty.DEFAULT

/** Ba muc cua thanh dieu huong duoi, kem icon va nhan cua tung muc. */
private enum class HomeTab(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    DISCOVER(R.drawable.ic_nav_discover, R.string.home_tab_discover),
    DAILY(R.drawable.ic_nav_daily, R.string.home_tab_daily),
    MINE(R.drawable.ic_nav_mine, R.string.home_tab_mine)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onPuzzleClick: (puzzleId: String, difficulty: Difficulty) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        modifier = Modifier
            // Thanh he thong dang bi an cho ca app, nhung cho status bar co the la notch /
            // camera nen van chua cho no. Phan nav bar duoi thi dung duoc het.
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility),
        uiState = uiState,
        onPuzzleClick = { puzzleId -> onPuzzleClick(puzzleId, PUZZLE_DIFFICULTY) },
        onCategorySelected = viewModel::onCategorySelected,
        onSettingsClick = onSettingsClick
    )
}

/**
 * Phan UI thuan tuy - khong biet gi ve ViewModel, de test va preview.
 *
 * Bo cuc theo kieu cac app xep hinh pho bien: thanh dieu huong duoi chia lam ba muc, tab
 * dau la anh cua ngay cong cac hang ngang theo bo suu tap, tab giua danh rieng cho anh cua
 * ngay, tab cuoi la nhung buc dang choi do.
 */
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onPuzzleClick: (String) -> Unit,
    onCategorySelected: (PuzzleCategory?) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(HomeTab.DISCOVER) }
    // Giu o day chu khong trong MineTab: mo mot bo suu tap la MineTab roi khoi cay giao
    // dien, quay lai phai ve dung muc cu chu khong nhay ve "dang tien hanh".
    var minePage by remember { mutableStateOf(MineTabPage.ONGOING) }

    Column(modifier = modifier.fillMaxSize()) {
        HomeTopBar(
            category = uiState.selectedCategory,
            onClearCategory = { onCategorySelected(null) },
            onSettingsClick = onSettingsClick
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                uiState.errorMessageRes != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(text = stringResource(uiState.errorMessageRes)) }

                // Da bam "xem tat ca" mot bo suu tap: hien luoi day cua rieng bo do.
                uiState.selectedCategory != null -> PuzzleGrid(
                    puzzles = uiState.puzzles,
                    progressPercent = uiState.progressPercent,
                    bestScore = uiState.bestScore,
                    onPuzzleClick = onPuzzleClick
                )

                // Doi tab thi noi dung mo dan vao nhau thay vi thay thang, cung nhip voi
                // thanh dieu huong duoi.
                else -> Crossfade(
                    targetState = tab,
                    animationSpec = tween(NAV_ANIM_MILLIS),
                    label = "homeTab"
                ) { current ->
                    when (current) {
                        HomeTab.DISCOVER -> DiscoverTab(
                            uiState = uiState,
                            onPuzzleClick = onPuzzleClick,
                            onSeeAll = onCategorySelected
                        )

                        HomeTab.DAILY -> DailyTab(uiState = uiState, onPuzzleClick = onPuzzleClick)

                        HomeTab.MINE -> MineTab(
                            uiState = uiState,
                            page = minePage,
                            onPageSelected = { minePage = it },
                            onPuzzleClick = onPuzzleClick
                        )
                    }
                }
            }
        }

        HomeNavigationBar(
            selected = tab,
            onSelect = { selected ->
                // Doi tab thi bo bo loc dang mo, khong thi tab nao cung ra cung mot luoi.
                onCategorySelected(null)
                tab = selected
            }
        )
    }
}

@Composable
private fun HomeTopBar(
    category: PuzzleCategory?,
    onClearCategory: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category != null) {
            TextButton(onClick = onClearCategory) {
                Text(text = stringResource(R.string.action_back))
            }
        }
        Text(
            text = stringResource(category?.labelRes ?: R.string.home_title),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onSettingsClick) {
            Text(text = stringResource(R.string.action_settings))
        }
    }
}

/** Anh cua ngay o tren cung, roi den hang "dang choi" va tung bo suu tap. */
@Composable
private fun DiscoverTab(
    uiState: HomeUiState,
    onPuzzleClick: (String) -> Unit,
    onSeeAll: (PuzzleCategory) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        uiState.dailyPuzzle?.let { daily ->
            DailyPuzzleCard(
                puzzle = daily,
                progressPercent = uiState.progressPercent[daily.id],
                onClick = { onPuzzleClick(daily.id) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (uiState.inProgress.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.home_section_continue))
            PuzzleRow(
                puzzles = uiState.inProgress,
                progressPercent = uiState.progressPercent,
                bestScore = uiState.bestScore,
                onPuzzleClick = onPuzzleClick
            )
        }

        // Giu dung thu tu cua enum de vi tri cac bo suu tap khong nhay moi lan tai lai.
        PuzzleCategory.entries.forEach { category ->
            val puzzles = uiState.allPuzzles.filter { it.category == category }
            if (puzzles.isEmpty()) return@forEach
            SectionHeader(
                title = stringResource(category.labelRes),
                actionText = stringResource(R.string.home_section_see_all),
                onAction = { onSeeAll(category) }
            )
            PuzzleRow(
                puzzles = puzzles,
                progressPercent = uiState.progressPercent,
                bestScore = uiState.bestScore,
                onPuzzleClick = onPuzzleClick
            )
        }
    }
}

/** Rieng buc cua hom nay, de to giua man hinh. */
@Composable
private fun DailyTab(uiState: HomeUiState, onPuzzleClick: (String) -> Unit) {
    val daily = uiState.dailyPuzzle
    if (daily == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.home_empty_mine))
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.home_daily_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DailyPuzzleCard(
            puzzle = daily,
            progressPercent = uiState.progressPercent[daily.id],
            onClick = { onPuzzleClick(daily.id) },
            modifier = Modifier.padding(vertical = 12.dp)
        )
        AnhnnGradientButton(
            text = stringResource(R.string.action_play_now),
            onClick = { onPuzzleClick(daily.id) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Nhung gi thuoc ve nguoi choi, chia lam ba muc: buc dang do dang, buc da ghep xong va bo
 * suu tap (nhung buc da bam tim o man xem truoc).
 */
@Composable
private fun MineTab(
    uiState: HomeUiState,
    page: MineTabPage,
    onPageSelected: (MineTabPage) -> Unit,
    onPuzzleClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = page.ordinal) {
            MineTabPage.entries.forEach { entry ->
                Tab(
                    selected = entry == page,
                    onClick = { onPageSelected(entry) },
                    text = { Text(text = stringResource(entry.labelRes)) }
                )
            }
        }

        when (page) {
            MineTabPage.ONGOING -> PuzzleGridOrEmpty(
                puzzles = uiState.inProgress,
                progressPercent = uiState.progressPercent,
                bestScore = uiState.bestScore,
                emptyText = stringResource(R.string.home_empty_mine),
                onPuzzleClick = onPuzzleClick
            )

            MineTabPage.DONE -> PuzzleGridOrEmpty(
                puzzles = uiState.completed,
                progressPercent = uiState.progressPercent,
                bestScore = uiState.bestScore,
                emptyText = stringResource(R.string.home_empty_done),
                onPuzzleClick = onPuzzleClick
            )

            MineTabPage.COLLECTIONS -> PuzzleGridOrEmpty(
                puzzles = uiState.favorites,
                progressPercent = uiState.progressPercent,
                bestScore = uiState.bestScore,
                emptyText = stringResource(R.string.home_empty_favorites),
                onPuzzleClick = onPuzzleClick
            )
        }
    }
}

/** Ba muc trong tab "cua toi". */
private enum class MineTabPage(@get:StringRes val labelRes: Int) {
    ONGOING(R.string.mine_tab_ongoing),
    DONE(R.string.mine_tab_done),
    COLLECTIONS(R.string.mine_tab_collections)
}

@Composable
private fun PuzzleGridOrEmpty(
    puzzles: List<PuzzleImage>,
    progressPercent: Map<String, Int>,
    bestScore: Map<String, Int>,
    emptyText: String,
    onPuzzleClick: (String) -> Unit
) {
    if (puzzles.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emptyText, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    PuzzleGrid(
        puzzles = puzzles,
        progressPercent = progressPercent,
        bestScore = bestScore,
        onPuzzleClick = onPuzzleClick
    )
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) { Text(text = actionText) }
        }
    }
}

/** Hang ngang cac buc anh trong mot bo suu tap. */
@Composable
private fun PuzzleRow(
    puzzles: List<PuzzleImage>,
    progressPercent: Map<String, Int>,
    bestScore: Map<String, Int>,
    onPuzzleClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = puzzles, key = { it.id }) { puzzle ->
            PuzzleCard(
                puzzle = puzzle,
                onClick = { onPuzzleClick(puzzle.id) },
                progressPercent = progressPercent[puzzle.id],
                score = bestScore[puzzle.id],
                modifier = Modifier.width(ROW_CARD_WIDTH)
            )
        }
    }
}

@Composable
private fun PuzzleGrid(
    puzzles: List<PuzzleImage>,
    progressPercent: Map<String, Int>,
    bestScore: Map<String, Int>,
    onPuzzleClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = puzzles, key = { it.id }) { puzzle ->
            PuzzleCard(
                puzzle = puzzle,
                onClick = { onPuzzleClick(puzzle.id) },
                progressPercent = progressPercent[puzzle.id],
                score = bestScore[puzzle.id]
            )
        }
    }
}

/**
 * The lon cua buc anh trong ngay: anh ngang, nhan "hom nay" o goc tren-trai va % tien do o
 * goc tren-phai giong the thuong.
 */
@Composable
private fun DailyPuzzleCard(
    puzzle: PuzzleImage,
    progressPercent: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(DAILY_ASPECT)
            .clip(RoundedCornerShape(24.dp))
            .background(AnhnnGradients.primaryVertical())
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        AsyncImage(
            model = puzzle.assetUri(),
            contentDescription = puzzle.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = stringResource(R.string.home_daily_badge),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AnhnnGradients.primaryHorizontal())
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        if (progressPercent != null) {
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        // Ten anh doc tren moi buc anh nho mot dai toi vua du o chan the.
        Text(
            text = puzzle.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

/**
 * Thanh dieu huong duoi kieu vien thuoc: mot the trang bo goc tron han, noi len khoi nen va
 * cach le man hinh, thay cho thanh vuong tron chieu ngang cua Material. Muc dang chon hien
 * icon mau nhan kem ten muc, cac muc con lai chi con icon mo.
 */
@Composable
private fun HomeNavigationBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = NAV_BAR_ELEVATION,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NAV_BAR_MARGIN, vertical = NAV_BAR_MARGIN)
            .height(NAV_BAR_HEIGHT)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.entries.forEach { tab ->
                HomeNavigationItem(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    // Chia deu chieu ngang: moi muc giu dung mot phan ba thanh nen doi muc
                    // thi cac icon khong bi day qua lai.
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Mot muc cua thanh dieu huong. Doi muc thi mau icon, co icon va do mo cua ten muc chuyen
 * dan chu khong nhay mot phat. Cho cua ten muc luon duoc chua san (muc khong duoc chon thi
 * ten trong suot), nen icon dung yen mot cho khi bam qua lai.
 */
@Composable
private fun HomeNavigationItem(
    tab: HomeTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(tab.labelRes)
    val transition = updateTransition(targetState = selected, label = "navItem")
    val color by transition.animateColor(
        transitionSpec = { tween(NAV_ANIM_MILLIS) },
        label = "color"
    ) { isSelected ->
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_ICON_IDLE_ALPHA)
        }
    }
    // Icon cua muc dang chon phong to mot chut, nay len theo kieu lo xo cho co suc song.
    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy) },
        label = "scale"
    ) { isSelected -> if (isSelected) NAV_ICON_SELECTED_SCALE else 1f }
    val labelAlpha by transition.animateFloat(
        transitionSpec = { tween(NAV_ANIM_MILLIS) },
        label = "labelAlpha"
    ) { isSelected -> if (isSelected) 1f else 0f }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            )
            // Nhan la ten muc nen khong doc lai ten do o icon va chu ben trong.
            .clearAndSetSemantics { contentDescription = label }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(NAV_ICON_SIZE)
                .scale(scale)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 2.dp)
                .graphicsLayer { alpha = labelAlpha }
        )
    }
}

/** Khoang cach tu the dieu huong duoi den le man hinh - the noi len chu khong dinh vao le. */
private val NAV_BAR_MARGIN = 16.dp

/** Chieu cao cua the dieu huong duoi, du cho icon cong ten muc cua muc dang chon. */
private val NAV_BAR_HEIGHT = 72.dp

/** Do noi cua the dieu huong duoi so voi nen. */
private val NAV_BAR_ELEVATION = 10.dp

/** Co icon trong thanh dieu huong duoi. */
private val NAV_ICON_SIZE = 26.dp

/** Do mo cua icon o cac muc khong duoc chon. */
private const val NAV_ICON_IDLE_ALPHA = 0.55f

/** Co icon cua muc dang chon so voi cac muc khac. */
private const val NAV_ICON_SELECTED_SCALE = 1.1f

/** Thoi luong doi muc o thanh dieu huong (ms), dung chung cho ca noi dung ben tren. */
private const val NAV_ANIM_MILLIS = 220

/** Be ngang cua the anh trong hang ngang. */
private val ROW_CARD_WIDTH = 148.dp

/** Ti le the anh cua ngay. */
private const val DAILY_ASPECT = 16f / 10f

/** Do duc cua lop nen dat duoi chu tren anh. */
private const val SCRIM_ALPHA = 0.45f
