package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.compose.runtime.Immutable
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.BoardBackground
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleArtwork
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzlePlayState
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ScoreRules

@Immutable
data class GameUiState(
    val isLoading: Boolean = true,
    val artwork: PuzzleArtwork? = null,
    val playState: PuzzlePlayState? = null,
    val draggingPieceId: Int? = null,
    /** Manh (hay khoi cua no) duoc thao tac gan nhat: ve tren cac manh roi khac. */
    val lastMovedPieceId: Int? = null,
    /** So luot goi y con lai trong van nay. */
    val hintsLeft: Int = HINT_COUNT,
    /**
     * Hang doi manh dang duoc goi y dua vao o. Ba manh bay lan luot, moi luc mot manh:
     * manh dau hang la manh dang bay, bay xong thi no roi khoi hang.
     */
    val hintPieceIds: List<Int> = emptyList(),
    /**
     * Cac manh dang bay tu ban co ve cuoi khay (nut don). Manh chi thuc su vao khay khi no
     * bay den, nen trong luc bay no van con o cho cu trong state.
     */
    val cleaningPieceIds: List<Int> = emptyList(),
    val elapsedSeconds: Int = 0,
    /** Diem da an trong van nay, xem [ScoreRules]. */
    val score: Int = 0,
    val isPaused: Boolean = false,
    /** Chi hien manh cua 4 canh trong khay: loc de nguoi choi dung khung truoc. */
    val edgePiecesOnly: Boolean = false,
    /** Nen ban choi dang dung, nguoi choi doi duoc ngay trong van. */
    val boardBackground: BoardBackground = BoardBackground.DEFAULT,
    /** Ba cong tac tu man cai dat, doc theo thoi gian thuc. */
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val multiSelectEnabled: Boolean = false,
    /**
     * Khay dang mo thanh hop luoi cuon doc. Trong hop luon cham chon duoc manh, ke ca khi
     * [multiSelectEnabled] dang tat.
     */
    val trayExpanded: Boolean = false,
    /**
     * Cac manh dang duoc cham chon trong khay (chi khi [multiSelectEnabled]). Chon xong thi
     * bam nut dua ca nhom len ban choi - xem GameViewModel.onSelectedPiecesReleased.
     */
    val selectedTrayPieceIds: Set<Int> = emptySet(),
    /**
     * Cac manh vua duoc dua tu khay len ban choi ca nhom: chung nay len mot nhip khi hien ra
     * de nguoi choi nhin ra ngay nhom manh moi giua nhung manh da nam san. Danh sach tu rong
     * lai sau khi nhip nay chay xong - xem GameViewModel.onSelectedPiecesReleased.
     */
    val droppedPieceIds: Set<Int> = emptySet(),
    val isSolved: Boolean = false,
    val errorMessageRes: Int? = null
) {
    val placedCount: Int get() = playState?.placedCount ?: 0
    val totalPieces: Int get() = playState?.puzzle?.pieces?.size ?: 0

    /** Manh dang bay vao o cua no, null neu khong co goi y nao dang chay. */
    val hintPieceId: Int? get() = hintPieceIds.firstOrNull()

    /** Het luot, dang bay do goi y truoc, hay da xong van thi khong bam goi y duoc nua. */
    val canUseHint: Boolean
        get() = playState != null && hintsLeft > 0 && !isSolved &&
            hintPieceIds.isEmpty() && cleaningPieceIds.isEmpty()

    /** Dang cham chon do: thanh cong cu nhuong cho nut dua manh len ban choi. */
    val hasTraySelection: Boolean get() = selectedTrayPieceIds.isNotEmpty()

    /** Chi don duoc khi con manh roi le va khong co manh nao dang bay. */
    val canClean: Boolean
        get() = playState?.loosePieces?.isNotEmpty() == true &&
            hintPieceIds.isEmpty() && cleaningPieceIds.isEmpty()

    companion object {
        /** So luot goi y moi van, moi luot dien 3 manh. */
        const val HINT_COUNT = 3
    }
}
