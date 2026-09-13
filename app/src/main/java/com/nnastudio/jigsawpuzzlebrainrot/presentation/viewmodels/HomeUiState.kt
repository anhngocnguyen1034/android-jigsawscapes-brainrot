package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.compose.runtime.Immutable
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleProgress
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ShopRules

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
    /** Diem con lai trong vi, hien o goc tren-phai man hinh chinh. */
    val points: Int = 0,
    /** Cac buc con khoa: buc PRO chua duoc mua bang diem. */
    val lockedPuzzleIds: Set<String> = emptySet(),
    /** Gia mo mot buc khoa - moi buc cung gia, xem [ShopRules.unlockPrice]. */
    val unlockPrice: Int = ShopRules.unlockPrice(),
    val errorMessageRes: Int? = null
) {
    /** Con thieu bao nhieu diem nua moi mua duoc mot buc khoa; 0 la da du. */
    val missingPoints: Int get() = (unlockPrice - points).coerceAtLeast(0)
}
