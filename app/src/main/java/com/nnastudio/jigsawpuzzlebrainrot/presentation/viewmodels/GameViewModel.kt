package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.BoardBackground
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceBounds
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceOffset
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleArtwork
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleSource
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ScoreRules
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ClearSavedGameUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.EarnPointsUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GenerateJigsawPuzzleUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GetPuzzleDetailUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPuzzle
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.LoadPuzzleArtworkUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.LoadSavedGameUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveSettingsUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.PrepareTrayUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.SaveGameUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.SavePuzzleResultUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.SetBoardBackgroundUseCase
import com.nnastudio.jigsawpuzzlebrainrot.presentation.navigation.GameRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.random.Random

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPuzzleDetail: GetPuzzleDetailUseCase,
    private val loadArtwork: LoadPuzzleArtworkUseCase,
    private val generateJigsawPuzzle: GenerateJigsawPuzzleUseCase,
    private val prepareTray: PrepareTrayUseCase,
    private val savePuzzleResult: SavePuzzleResultUseCase,
    private val earnPoints: EarnPointsUseCase,
    private val loadSavedGame: LoadSavedGameUseCase,
    private val saveGame: SaveGameUseCase,
    private val clearSavedGame: ClearSavedGameUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    private val setBoardBackground: SetBoardBackgroundUseCase
) : ViewModel() {

    private val route: GameRoute = savedStateHandle.toRoute()
    private val difficulty: Difficulty = Difficulty.fromId(route.difficultyId)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Hat giong da cat bo manh cua van dang choi: luu cung ban luu de van duoc mo lai co
     * dung nhung duong cat cu (xem [SavedGame]).
     */
    private var seed: Long = 0L

    init {
        viewModelScope.launch {
            val saved = loadSavedGame(route.puzzleId, difficulty)
            if (saved == null || !resume(saved)) newGame()
        }
        autoSave()
        observeSettingsFlags()
    }

    /**
     * Nen ban choi, am thanh, rung va che do chon nhieu deu la cai dat chung nen doc tu
     * settings, khong nam trong ban luu cua van. Nguoi choi doi cai dat roi quay lai van
     * dang do la thay co hieu luc ngay.
     */
    private fun observeSettingsFlags() {
        viewModelScope.launch {
            observeSettings().distinctUntilChanged().collect { settings ->
                _uiState.update {
                    it.copy(
                        boardBackground = settings.boardBackground,
                        soundEnabled = settings.soundEnabled,
                        vibrationEnabled = settings.vibrationEnabled,
                        multiSelectEnabled = settings.multiSelectEnabled,
                        // Tat che do chon nhieu giua chung thi bo luon nhung manh dang cham -
                        // tru khi hop manh dang mo, vi trong hop luon cham chon duoc.
                        selectedTrayPieceIds = if (settings.multiSelectEnabled || it.trayExpanded) {
                            it.selectedTrayPieceIds
                        } else {
                            emptySet()
                        }
                    )
                }
            }
        }
    }

    /**
     * Mo / dong hop manh. Dong hop khi dang tat che do chon nhieu thi bo luon nhung manh
     * dang cham: ngoai hop, hang khay khong cho cham chon nua.
     */
    fun onTrayExpandedChange(expanded: Boolean) {
        _uiState.update { state ->
            state.copy(
                trayExpanded = expanded,
                selectedTrayPieceIds = if (expanded || state.multiSelectEnabled) {
                    state.selectedTrayPieceIds
                } else {
                    emptySet()
                }
            )
        }
    }

    /**
     * Cham mot manh de chon / bo chon. Cham chon duoc khi hop manh dang mo (hop sinh ra de
     * chon nhieu manh mot luc), hoac khi nguoi choi bat che do chon nhieu cho ca hang khay.
     * Manh da roi khoi khay (duoc keo ra, hay do goi y) thi khong con trong danh sach nay.
     */
    fun onTrayPieceTapped(pieceId: Int) {
        val state = _uiState.value
        if (!state.multiSelectEnabled && !state.trayExpanded) return
        _uiState.update { state ->
            val selected = state.selectedTrayPieceIds
            state.copy(
                selectedTrayPieceIds = if (pieceId in selected) {
                    selected - pieceId
                } else {
                    selected + pieceId
                }
            )
        }
    }

    /**
     * Dua cac manh dang cham len ban choi mot luot. Chung duoc rai deu trong vung choi chu
     * khong chong len nhau: nguoi choi dang muon nhin thay ca nhom cung luc de so voi anh.
     *
     * Hop manh dong lai luon: nguoi choi vua bao "dua len ban" thi viec tiep theo cua ho la
     * nhin ban choi, de hop mo thi no che mat dung nhung manh vua dua len.
     *
     * Manh nao roi vao dung o cua no thi [PuzzlePlayState.releaseFromTray] tu khoa lai, y
     * het nhu khi keo tay tha vao dung cho.
     */
    fun onSelectedPiecesReleased() {
        val state = _uiState.value
        val playState = state.playState ?: return
        val ids = state.selectedTrayPieceIds.filter { playState.isInTray(it) }
        if (ids.isEmpty()) {
            _uiState.update { it.copy(selectedTrayPieceIds = emptySet(), trayExpanded = false) }
            return
        }

        val positions = scatterPositions(playState, ids.size)
        var updated = playState
        ids.forEachIndexed { index, pieceId ->
            updated = updated.releaseFromTray(pieceId, positions[index])
        }
        _uiState.update {
            it.copy(
                playState = updated,
                selectedTrayPieceIds = emptySet(),
                trayExpanded = false,
                droppedPieceIds = ids.toSet(),
                lastMovedPieceId = ids.lastOrNull(),
                isSolved = updated.isSolved
            )
        }
        clearDroppedPieces(ids.toSet())
        if (updated.isSolved) onSolved(updated.moves)
    }

    /**
     * Tra danh sach manh vua dua len ban ve rong sau khi nhip nay len chay xong. Phai xoa
     * that: con ten trong danh sach thi lan sau dua dung manh do len se khong con nhip nay
     * nua (voi man hinh, danh sach khong doi la khong co gi moi).
     *
     * Chi xoa dung nhung manh cua luot nay, phong khi nguoi choi kip bam them mot luot khac
     * trong luc cho.
     */
    private fun clearDroppedPieces(ids: Set<Int>) {
        viewModelScope.launch {
            delay(DROP_POP_DURATION)
            _uiState.update { it.copy(droppedPieceIds = it.droppedPieceIds - ids) }
        }
    }

    /**
     * Cho dat cho [count] manh sap duoc dua len ban choi.
     *
     * Vung choi duoc chia thanh luoi o, moi o rong hon manh [SCATTER_GAP] lan nen hai manh
     * o hai o canh nhau van con khe ho - khong manh nao dinh sat manh nao. Nhung o dang co
     * manh roi nam san (cac lan them truoc, hay manh nguoi choi tu keo ra) bi loai truoc khi
     * chia, nho vay lan them sau khong dat de len lan truoc.
     *
     * Het o trong that su thi danh dung lai ca luoi - luc do ban choi da chat kin manh roi.
     */
    private fun scatterPositions(playState: PuzzlePlayState, count: Int): List<PieceOffset> {
        if (count <= 0) return emptyList()
        val bounds = playState.bounds
        val difficulty = playState.puzzle.difficulty
        val stepX = SCATTER_GAP / difficulty.cols
        val stepY = SCATTER_GAP / difficulty.rows
        val width = (bounds.maxX - bounds.minX).coerceAtLeast(0f)
        val height = (bounds.maxY - bounds.minY).coerceAtLeast(0f)
        val cols = (width / stepX).toInt() + 1
        val rows = (height / stepY).toInt() + 1
        // Luoi can giua vung choi: phan du chia deu cho hai ben.
        val startX = bounds.minX + (width - stepX * (cols - 1)) / 2f
        val startY = bounds.minY + (height - stepY * (rows - 1)) / 2f
        val slots = List(rows * cols) { i ->
            PieceOffset(x = startX + stepX * (i % cols), y = startY + stepY * (i / cols))
        }

        val taken = playState.loosePieces.mapNotNull { playState.placements[it.id]?.position }
        val free = slots.filter { slot ->
            taken.none { abs(it.x - slot.x) < stepX / 2f && abs(it.y - slot.y) < stepY / 2f }
        }
        val usable = free.ifEmpty { slots }
        // Nhay cach deu tren danh sach o trong de ca nhom trai rong ca vung choi, thay vi
        // don het ve goc tren-trai.
        val jump = (usable.size / count).coerceAtLeast(1)
        return List(count) { i -> usable[(i * jump) % usable.size] }
    }

    /** Loc khay: chi hien manh vien, hay hien lai tat ca. */
    fun onToggleEdgePiecesOnly() {
        _uiState.update { it.copy(edgePiecesOnly = !it.edgePiecesOnly) }
    }

    fun onBoardBackgroundSelected(background: BoardBackground) {
        viewModelScope.launch { setBoardBackground(background) }
    }

    /** Bo van dang luu di, chia lai bo manh moi. */
    fun startNewGame() {
        viewModelScope.launch {
            clearSavedGame(route.puzzleId, difficulty)
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
                playState = prepareTray(loaded.second),
                boardBackground = it.boardBackground
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
                hintsLeft = saved.hintsLeft,
                score = saved.score,
                boardBackground = it.boardBackground
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
        _uiState.update { GameUiState(isLoading = true, boardBackground = it.boardBackground) }

        val image = getPuzzleDetail(route.puzzleId)
        if (image == null) {
            _uiState.update {
                it.copy(isLoading = false, errorMessageRes = R.string.error_puzzle_not_found)
            }
            return null
        }

        return loadArtwork(PuzzleSource.Asset(image.assetPath)) to
            generateJigsawPuzzle(image, difficulty, Random(seed))
    }

    /**
     * Ghi lai van sau moi nuoc di. Chi theo doi [GameUiState.playState] chu khong theo doi
     * dong ho: dong ho chay moi giay, ghi theo no la ghi lien tuc ca van. Van da xong thi
     * khong con gi de choi tiep nen khong ghi ([onSolved] xoa ban luu).
     */
    private fun autoSave() {
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
                hintsLeft = state.hintsLeft,
                score = state.score
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

    private fun releaseFromTray(pieceId: Int, position: PieceOffset) {
        val playState = _uiState.value.playState ?: return
        val released = playState.releaseFromTray(pieceId, position)
        _uiState.update {
            it.copy(
                playState = released,
                score = it.score + ScoreRules.gain(playState, released),
                draggingPieceId = null,
                lastMovedPieceId = pieceId
            )
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
            it.copy(
                playState = snapped,
                score = it.score + ScoreRules.gain(playState, snapped),
                draggingPieceId = null,
                lastMovedPieceId = pieceId
            )
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
            it.copy(
                playState = dropped,
                score = it.score + ScoreRules.gain(playState, dropped),
                draggingPieceId = null,
                lastMovedPieceId = pieceId
            )
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

    /**
     * Manh goi y da bay den o cua no: dat vao va cho manh tiep theo trong hang bay len.
     * Khong cong diem - manh nay do may dat chu khong phai nguoi choi ghep.
     */
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

    private fun onSolved(moves: Int) {
        timerJob?.cancel()
        val seconds = _uiState.value.elapsedSeconds
        val finalScore = _uiState.value.score + ScoreRules.solvedBonus(difficulty)
        _uiState.update {
            it.copy(isSolved = true, draggingPieceId = null, score = finalScore)
        }

        viewModelScope.launch {
            // Van da xong: khong con gi de choi tiep, lan sau vao la van moi.
            clearSavedGame(route.puzzleId, difficulty)
            savePuzzleResult(route.puzzleId, difficulty, seconds, moves, finalScore)
            // Diem chi vao vi khi ghep xong ca buc: bo do giua chung thi khong an diem,
            // nen gia mot buc khoa dem duoc bang so van tron ven (xem ShopRules).
            earnPoints(finalScore)
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
                if (playState != null && !state.isSolved &&
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

        /**
         * O rai manh rong gap nay lan canh mot manh: lon hon 1 nen giua hai manh canh nhau
         * luon con khe ho, nhin ra ngay tung manh mot.
         */
        const val SCATTER_GAP = 1.35f

        /** Moi bao nhieu giay thi ghi lai gio cua van dang choi. */
        const val SAVE_CLOCK_SECONDS = 15

        /**
         * Nhip manh moi nay len tren ban choi keo dai bao lau (ms). Dai hon nhip ve mot chut
         * de danh sach chi rong sau khi manh da dung han.
         */
        const val DROP_POP_DURATION = 600L
    }
}
