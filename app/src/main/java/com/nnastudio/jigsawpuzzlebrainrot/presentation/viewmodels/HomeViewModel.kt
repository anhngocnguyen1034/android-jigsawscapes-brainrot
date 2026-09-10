package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.puzzleOfDay
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GetPuzzleListUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveFavoritesUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveProgressUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveSavedProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.nnastudio.jigsawpuzzlebrainrot.utils.currentEpochDay
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    getPuzzleList: GetPuzzleListUseCase,
    observeProgress: ObserveProgressUseCase,
    observeSavedProgress: ObserveSavedProgressUseCase,
    observeFavorites: ObserveFavoritesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val selectedCategory = MutableStateFlow<PuzzleCategory?>(null)

    init {
        viewModelScope.launch {
            combine(
                selectedCategory.flatMapLatest { category -> getPuzzleList(category) },
                getPuzzleList(),
                observeProgress(),
                observeSavedProgress(),
                observeFavorites()
            ) { filtered, all, progressList, saved, favoriteIds ->
                // Mot buc co the da choi o nhieu moc so manh: lay ket qua tot nhat.
                val progress = progressList.groupBy { it.puzzleId }
                    .mapValues { (_, entries) -> entries.maxBy { it.bestScore } }
                // Anh da choi xong hien 100% du ban luu da bi xoa khi ket thuc van.
                val percent = progress.filterValues { it.completed }.mapValues { 100 } + saved
                HomeUiState(
                    isLoading = false,
                    puzzles = filtered,
                    allPuzzles = all,
                    dailyPuzzle = all.puzzleOfDay(currentEpochDay()),
                    inProgress = all.filter { percent[it.id] in 1..99 }
                        .sortedByDescending { percent[it.id] },
                    completed = all.filter { percent[it.id] == 100 },
                    favorites = all.filter { it.id in favoriteIds },
                    progress = progress,
                    progressPercent = percent,
                    bestScore = progressList.filter { it.completed && it.bestScore > 0 }
                        .groupBy { it.puzzleId }
                        .mapValues { (_, entries) -> entries.maxOf { it.bestScore } },
                    selectedCategory = selectedCategory.value
                )
            }
                .catch { _uiState.update { it.copy(isLoading = false, errorMessageRes = R.string.error_load_puzzles) } }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onCategorySelected(category: PuzzleCategory?) {
        // Chon lai dung bo dang mo thi khong co gi doi: dat isLoading luc do se treo man
        // hinh o vong quay vi luong danh sach khong phat lai.
        if (selectedCategory.value == category) return
        selectedCategory.value = category
        _uiState.update { it.copy(isLoading = true, selectedCategory = category) }
    }
}
