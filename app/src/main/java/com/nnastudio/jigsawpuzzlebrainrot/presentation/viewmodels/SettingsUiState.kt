package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.compose.runtime.Immutable
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.AppSettings

@Immutable
data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings()
)
