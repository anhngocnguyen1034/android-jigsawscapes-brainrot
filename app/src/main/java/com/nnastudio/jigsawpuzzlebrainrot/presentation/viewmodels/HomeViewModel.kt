package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.GetPuzzleListUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    getPuzzleList: GetPuzzleListUseCase,
    observeProgress: ObserveProgressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val selectedCategory = MutableStateFlow<PuzzleCategory?>(null)

    init {
        viewModelScope.launch {
            selectedCategory
                .flatMapLatest { category -> getPuzzleList(category) }
                .combine(observeProgress()) { puzzles, progressList ->
                    puzzles to progressList.associateBy { it.puzzleId }
                }
                .catch { _uiState.update { it.copy(isLoading = false, errorMessageRes = R.string.error_load_puzzles) } }
                .collect { (puzzles, progress) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            puzzles = puzzles,
                            progress = progress,
                            errorMessageRes = null
                        )
                    }
                }
        }
    }

    fun onCategorySelected(category: PuzzleCategory?) {
        selectedCategory.value = category
        _uiState.update { it.copy(isLoading = true, selectedCategory = category) }
    }

    fun onDifficultySelected(difficulty: Difficulty) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
    }
}
