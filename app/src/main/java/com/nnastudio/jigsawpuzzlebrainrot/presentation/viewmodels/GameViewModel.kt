package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceBounds
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceOffset
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleArtwork
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleSource
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ClearSavedGameUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GenerateJigsawPuzzleUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GetPuzzleDetailUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPuzzle
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.LoadPuzzleArtworkUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.LoadSavedGameUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.PrepareTrayUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.SaveGameUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.SavePuzzleResultUseCase
import com.nnastudio.jigsawpuzzlebrainrot.presentation.navigation.GameRoute
import com.nnastudio.jigsawpuzzlebrainrot.presentation.navigation.decodeDeviceImageUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPuzzleDetail: GetPuzzleDetailUseCase,
    private val loadArtwork: LoadPuzzleArtworkUseCase,
    private val generateJigsawPuzzle: GenerateJigsawPuzzleUseCase,
    private val prepareTray: PrepareTrayUseCase,
    private val savePuzzleResult: SavePuzzleResultUseCase,
    private val loadSavedGame: LoadSavedGameUseCase,
    private val saveGame: SaveGameUseCase,
    private val clearSavedGame: ClearSavedGameUseCase
) : ViewModel() {

    private val route: GameRoute = savedStateHandle.toRoute()
    private val difficulty: Difficulty = Difficulty.fromId(route.difficultyId)
    private val deviceImageUri: String? = route.encodedImageUri?.let(::decodeDeviceImageUri)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Hat giong da cat bo manh cua van dang choi: luu cung ban luu de van duoc mo lai co
     * dung nhung duong cat cu (xem [SavedGame]).
     */
    private var seed: Long = 0L

    /**
     * Anh nguoi choi tu chon trong may khong luu duoc: quyen doc URI het khi app dong, mo
     * lai ban luu se khong doc noi anh nua.
     */
    private val canSaveGame: Boolean = deviceImageUri == null

    init {
        viewModelScope.launch {
            val saved = if (canSaveGame) loadSavedGame(route.puzzleId, difficulty) else null
            if (saved == null || !resume(saved)) newGame()
        }
        autoSave()
    }

    /** Bo van dang luu di, chia lai bo manh moi. */
    fun startNewGame() {
        viewModelScope.launch {
            if (canSaveGame) clearSavedGame(route.puzzleId, difficulty)
            newGame()
        }
    }

    private suspend fun newGame() {
        val newSeed = Random.nextLong()
        val loaded = loadPuzzle(newSeed) ?: return
        seed = newSeed
        _uiState.update {
            GameUiState(
                isLoading = false,
                artwork = loaded.first,
                playState = prepareTray(loaded.second)
            )
        }
        restartTimer()
    }

    /**
     * Choi tiep van da luu. Tra ve false khi ban luu khong dung vao bo manh nua (doi do kho,
     * doi so manh) - luc do man hinh chia van moi.
     */
    private suspend fun resume(saved: SavedGame): Boolean {
        val loaded = loadPuzzle(saved.seed) ?: return true
        val playState = saved.toPlayState(loaded.second) ?: return false
        seed = saved.seed
        _uiState.update {
            GameUiState(
                isLoading = false,
                artwork = loaded.first,
                playState = playState,
                elapsedSeconds = saved.elapsedSeconds,
                hintsLeft = saved.hintsLeft
            )
        }
        restartTimer()
        return true
    }

    /**
     * Anh + bo manh cat theo [seed]. Tra ve null khi khong tim thay anh: luc do state da
     * mang thong bao loi, khong con gi de choi.
     */
    private suspend fun loadPuzzle(seed: Long): Pair<PuzzleArtwork, JigsawPuzzle>? {
        timerJob?.cancel()
        _uiState.update { GameUiState(isLoading = true) }

        val image = resolveImage()
        if (image == null) {
            _uiState.update {
                it.copy(isLoading = false, errorMessageRes = R.string.error_puzzle_not_found)
            }
            return null
        }

        val source = deviceImageUri?.let { PuzzleSource.Device(it) }
            ?: PuzzleSource.Asset(image.assetPath)
        return loadArtwork(source) to generateJigsawPuzzle(image, difficulty, Random(seed))
    }

    /**
     * Ghi lai van sau moi nuoc di. Chi theo doi [GameUiState.playState] chu khong theo doi
     * dong ho: dong ho chay moi giay, ghi theo no la ghi lien tuc ca van. Van da xong thi
     * khong con gi de choi tiep nen khong ghi ([onSolved] xoa ban luu).
     */
    private fun autoSave() {
        if (!canSaveGame) return
        viewModelScope.launch {
            _uiState
                .map { it.playState?.takeIf { play -> !play.isSolved } }
                // So sanh dung nhung gi duoc ghi: bounds doi khi man hinh do lai / xoay may,
                // ghi lai luc do la ghi y nguyen ban cu.
                .distinctUntilChangedBy { play ->
                    play?.let { listOf(it.placements, it.groups, it.trayOrder, it.moves) }
                }
                .collect { playState -> if (playState != null) persist(playState) }
        }
    }

    private suspend fun persist(playState: PuzzlePlayState) {
        val state = _uiState.value
        saveGame(
            SavedGame.of(
                puzzleId = route.puzzleId,
                seed = seed,
                playState = playState,
                elapsedSeconds = state.elapsedSeconds,
                hintsLeft = state.hintsLeft
            )
        )
    }

    /** Man hinh do xong vung choi: manh roi duoc di chuyen trong ca vung nay. */
    fun onPlayAreaMeasured(bounds: PieceBounds) {
        _uiState.update { it.copy(playState = it.playState?.withBounds(bounds)) }
    }

    /** Keo mot manh tu ban co tra ve khay, chen vao vi tri [index] trong danh sach. */
    fun onPieceReturnedToTray(pieceId: Int, index: Int) {
        val returned = _uiState.value.playState?.returnToTray(pieceId, index) ?: return
        // Manh ve khay thi khong con nam tren ban co de ma de len ai.
        _uiState.update {
            it.copy(playState = returned, draggingPieceId = null, lastMovedPieceId = null)
        }
    }

    /** Keo mot manh tu khay len ban co tai [position] (toa do ban co). */
    fun onTrayPieceDropped(pieceId: Int, position: PieceOffset) {
        releaseFromTray(pieceId, position)
    }

    /** Keo mot manh trong khay tha lai trong khay: doi no sang cho [index] vua tha. */
    fun onTrayPieceMoved(pieceId: Int, index: Int) {
        val moved = _uiState.value.playState?.moveInTray(pieceId, index) ?: return
        _uiState.update { it.copy(playState = moved) }
    }

    /** Chon mot manh trong khay: manh tu tim cho trong tren ban co. */
    fun onTrayPieceSelected(pieceId: Int) {
        val playState = _uiState.value.playState ?: return
        releaseFromTray(pieceId, playState.freeBoardSpot(pieceId))
    }

    private fun releaseFromTray(pieceId: Int, position: PieceOffset) {
        val released = _uiState.value.playState?.releaseFromTray(pieceId, position) ?: return
        _uiState.update {
            it.copy(playState = released, draggingPieceId = null, lastMovedPieceId = pieceId)
        }
        if (released.isSolved) onSolved(released.moves)
    }

    fun onPieceDragStart(pieceId: Int) {
        _uiState.update { it.copy(draggingPieceId = pieceId, lastMovedPieceId = pieceId) }
    }

    /**
     * Manh vien da vao dung o giua luc keo: dat va khoa no ngay, khong cho keo tiep. Man
     * hinh goi ham nay trong luc ngon tay con tren manh nen phai ket thuc luon luot keo.
     */
    fun onPieceSnappedWhileDragging(pieceId: Int, dx: Float, dy: Float) {
        val playState = _uiState.value.playState ?: return
        val snapped = playState.snapWhileDragging(pieceId, dx, dy) ?: return
        _uiState.update {
            it.copy(playState = snapped, draggingPieceId = null, lastMovedPieceId = pieceId)
        }
        if (snapped.isSolved) onSolved(snapped.moves)
    }

    /**
     * Ket thuc keo: man hinh keo manh ngay trong lop ve nen chi bao lai doan da keo mot lan
     * ([dx], [dy] theo toa do ban co). Di chuyen va tha gop vao mot lan doi state de manh
     * khong nhay mot frame giua hai lan.
     */
    fun onPieceDragEnd(pieceId: Int, dx: Float, dy: Float) {
        val playState = _uiState.value.playState ?: return
        val dropped = playState.movePiece(pieceId, dx, dy).dropPiece(pieceId)
        _uiState.update {
            it.copy(playState = dropped, draggingPieceId = null, lastMovedPieceId = pieceId)
        }
        if (dropped.isSolved) onSolved(dropped.moves)
    }

    /**
     * Bam goi y: ba manh o cac o thieu dau tien lan luot bay vao dung cho. Chi xep hang o
     * day, moi manh chi thuc su duoc dat khi no bay den noi ([onHintPieceLanded]).
     */
    fun onHintRequested() {
        val state = _uiState.value
        val playState = state.playState ?: return
        if (!state.canUseHint) return
        val pieceIds = playState.missingPieces(GameUiState.HINT_COUNT).map { it.id }
        if (pieceIds.isEmpty()) return
        _uiState.update { it.copy(hintPieceIds = pieceIds, hintsLeft = it.hintsLeft - 1) }
    }

    /** Manh goi y da bay den o cua no: dat vao va cho manh tiep theo trong hang bay len. */
    fun onHintPieceLanded(pieceId: Int) {
        val placed = _uiState.value.playState?.placeHint(pieceId) ?: return
        _uiState.update {
            it.copy(
                playState = placed,
                hintPieceIds = it.hintPieceIds - pieceId,
                lastMovedPieceId = pieceId
            )
        }
        if (placed.isSolved) onSolved(placed.moves)
    }

    /**
     * Bam don: moi manh dang roi le bay ve cuoi khay. Cac manh bay cung luc (man hinh lech
     * nhip mot chut cho de nhin), manh nao den khay thi [onCleanPieceLanded] moi dat vao.
     */
    fun onCleanRequested() {
        val state = _uiState.value
        if (!state.canClean) return
        val pieceIds = state.playState?.loosePieces?.map { it.id } ?: return
        _uiState.update {
            it.copy(
                cleaningPieceIds = pieceIds,
                draggingPieceId = null,
                lastMovedPieceId = null
            )
        }
    }

    /** Manh vua bay den khay: chen vao cuoi danh sach khay. */
    fun onCleanPieceLanded(pieceId: Int) {
        val playState = _uiState.value.playState ?: return
        val returned = playState.returnToTray(pieceId, playState.trayOrder.size)
        _uiState.update {
            it.copy(playState = returned, cleaningPieceIds = it.cleaningPieceIds - pieceId)
        }
    }

    fun onTogglePause() {
        val paused = !_uiState.value.isPaused
        _uiState.update { it.copy(isPaused = paused) }
        if (paused) timerJob?.cancel() else restartTimer()
    }

    private suspend fun resolveImage(): PuzzleImage? = when {
        deviceImageUri != null -> PuzzleImage(
            id = route.puzzleId,
            title = "",
            category = PuzzleCategory.BRAINROT,
            assetPath = ""
        )

        else -> getPuzzleDetail(route.puzzleId)
    }

    private fun onSolved(moves: Int) {
        timerJob?.cancel()
        val seconds = _uiState.value.elapsedSeconds
        _uiState.update { it.copy(isSolved = true, draggingPieceId = null) }

        // Anh nguoi dung tu chon khong luu vao bang thanh tich.
        if (deviceImageUri != null) return
        viewModelScope.launch {
            // Van da xong: khong con gi de choi tiep, lan sau vao la van moi.
            clearSavedGame(route.puzzleId, difficulty)
            savePuzzleResult(route.puzzleId, difficulty, seconds, moves)
        }
    }

    private fun restartTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MILLIS)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                // Ngoi nghi khong dong manh nao thi van khong duoc ghi lai, thoi gian da
                // choi se bi lui ve nuoc di cuoi: cu it giay ghi lai gio mot lan.
                val state = _uiState.value
                val playState = state.playState
                if (canSaveGame && playState != null && !state.isSolved &&
                    state.elapsedSeconds % SAVE_CLOCK_SECONDS == 0
                ) {
                    persist(playState)
                }
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TICK_MILLIS = 1_000L

        /** Moi bao nhieu giay thi ghi lai gio cua van dang choi. */
        const val SAVE_CLOCK_SECONDS = 15
    }
}
