package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.compose.runtime.Immutable
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleProgress

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val puzzles: List<PuzzleImage> = emptyList(),
    val progress: Map<String, PuzzleProgress> = emptyMap(),
    val selectedCategory: PuzzleCategory? = null,
    val selectedDifficulty: Difficulty = Difficulty.DEFAULT,
    val errorMessageRes: Int? = null
)
