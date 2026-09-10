package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.compose.runtime.Immutable
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleProgress

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    /** Danh sach dang hien, da loc theo [selectedCategory]. */
    val puzzles: List<PuzzleImage> = emptyList(),
    /** Ca catalog, khong loc - dung cho anh cua ngay va hang "dang choi". */
    val allPuzzles: List<PuzzleImage> = emptyList(),
    /** Buc anh cua hom nay, doi moi nua dem. */
    val dailyPuzzle: PuzzleImage? = null,
    /** Cac buc con do dang (1..99%), buc gan xong len truoc. */
    val inProgress: List<PuzzleImage> = emptyList(),
    /** Cac buc da ghep xong. */
    val completed: List<PuzzleImage> = emptyList(),
    /** Bo suu tap: cac buc nguoi choi da danh dau yeu thich. */
    val favorites: List<PuzzleImage> = emptyList(),
    val progress: Map<String, PuzzleProgress> = emptyMap(),
    /** puzzleId -> % da ghep cua van dang do dang. Anh chua choi thi khong co trong map. */
    val progressPercent: Map<String, Int> = emptyMap(),
    /** puzzleId -> diem cao nhat da dat, chi co o nhung buc da ghep xong. */
    val bestScore: Map<String, Int> = emptyMap(),
    val selectedCategory: PuzzleCategory? = null,
    val errorMessageRes: Int? = null
)
