package com.nnastudio.jigsawpuzzlebrainrot.core

import androidx.compose.runtime.Immutable

/** Trang thai chung cho moi man hinh: Loading -> Success | Error. */
@Immutable
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val messageRes: Int) : UiState<Nothing>
}
