package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.compose.runtime.Immutable
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPuzzle
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleArtwork

/** Man xem truoc mot buc anh: anh hien duoi dang bo manh da ghep xong. */
@Immutable
data class PuzzlePreviewUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val artwork: PuzzleArtwork? = null,
    /** Bo manh chi de ve duong cat; man nay khong choi duoc. */
    val puzzle: JigsawPuzzle? = null,
    /** Moc so manh dang chon - quyet dinh ca ban luu nao duoc choi tiep. */
    val difficulty: Difficulty = Difficulty.DEFAULT,
    /** Co van dang choi do dang: nut la "choi tiep" thay vi "bat dau". */
    val hasSavedGame: Boolean = false,
    /** % da ghep cua van dang do dang, null neu chua choi lan nao. */
    val progressPercent: Int? = null,
    /** Buc nay dang nam trong bo suu tap cua nguoi choi. */
    val isFavorite: Boolean = false,
    val errorMessageRes: Int? = null
)
