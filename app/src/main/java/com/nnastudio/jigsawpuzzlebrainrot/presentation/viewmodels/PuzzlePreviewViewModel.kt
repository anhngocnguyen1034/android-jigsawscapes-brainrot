package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleSource
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GenerateJigsawPuzzleUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GetPuzzleDetailUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.LoadPuzzleArtworkUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.LoadSavedGameUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveFavoritesUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveSavedProgressUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ToggleFavoriteUseCase
import com.nnastudio.jigsawpuzzlebrainrot.presentation.navigation.PuzzlePreviewRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class PuzzlePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPuzzleDetail: GetPuzzleDetailUseCase,
    private val loadArtwork: LoadPuzzleArtworkUseCase,
    private val generateJigsawPuzzle: GenerateJigsawPuzzleUseCase,
    private val loadSavedGame: LoadSavedGameUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    observeSavedProgress: ObserveSavedProgressUseCase,
    observeFavorites: ObserveFavoritesUseCase
) : ViewModel() {

    private val route: PuzzlePreviewRoute = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(PuzzlePreviewUiState())
    val uiState: StateFlow<PuzzlePreviewUiState> = _uiState.asStateFlow()

    /** Buc anh trong catalog, doc mot lan roi dung lai cho moi lan doi so manh. */
    private val image = MutableStateFlow<PuzzleImage?>(null)

    /** Moc so manh nguoi choi dang chon o vong quay giua man hinh. */
    private val selectedDifficulty =
        MutableStateFlow(Difficulty.fromId(route.difficultyId))

    init {
        viewModelScope.launch { load() }

        viewModelScope.launch {
            observeFavorites().collect { favorites ->
                _uiState.update { it.copy(isFavorite = route.puzzleId in favorites) }
            }
        }

        // Ban luu la cua tung cap (anh, so manh) nen doi so manh la phai doc lai: nut co
        // the tu "choi tiep" thanh "bat dau". Luong tien do keo theo ca truong hop nguoi
        // choi vao van roi quay ra - man nay nam lai trong back stack.
        viewModelScope.launch {
            combine(
                image.filterNotNull(),
                selectedDifficulty,
                observeSavedProgress()
            ) { image, difficulty, _ -> image to difficulty }
                .collectLatest { (image, difficulty) -> showDifficulty(image, difficulty) }
        }
    }

    /** Doi moc so manh: anh xem truoc cat lai theo so manh moi. */
    fun onDifficultySelected(difficulty: Difficulty) {
        selectedDifficulty.value = difficulty
    }

    /** Them / bo buc anh nay khoi bo suu tap. */
    fun onToggleFavorite() {
        viewModelScope.launch { toggleFavorite(route.puzzleId) }
    }

    private suspend fun load() {
        val puzzleImage = getPuzzleDetail(route.puzzleId)
        val artwork = puzzleImage
            ?.let { runCatching { loadArtwork(PuzzleSource.Asset(it.assetPath)) }.getOrNull() }
        if (puzzleImage == null || artwork == null) {
            _uiState.update {
                it.copy(isLoading = false, errorMessageRes = R.string.error_puzzle_not_found)
            }
            return
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                title = puzzleImage.title,
                artwork = artwork,
                errorMessageRes = null
            )
        }
        image.value = puzzleImage
    }

    private suspend fun showDifficulty(image: PuzzleImage, difficulty: Difficulty) {
        val saved = loadSavedGame(route.puzzleId, difficulty)
        // Dang choi do dang thi cat lai bang dung hat giong cua van do: duong cat trong anh
        // xem truoc giong het cai nguoi choi dang thay tren ban co. Chua choi thi hat giong
        // suy tu id de moi lan mo van thay mot bo duong cat.
        val seed = saved?.seed ?: route.puzzleId.hashCode().toLong()
        // Luoi 400 manh sinh mat vai chuc ms - de tren main thread la ro giat ngay khi
        // nguoi choi vua chon xong moc so manh.
        val puzzle = withContext(Dispatchers.Default) {
            generateJigsawPuzzle(image, difficulty, Random(seed))
        }
        _uiState.update {
            it.copy(
                difficulty = difficulty,
                puzzle = puzzle,
                hasSavedGame = saved != null,
                progressPercent = saved?.progressPercent
            )
        }
    }
}
