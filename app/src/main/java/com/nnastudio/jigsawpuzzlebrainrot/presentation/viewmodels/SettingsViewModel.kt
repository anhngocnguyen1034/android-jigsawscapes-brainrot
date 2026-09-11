package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ThemeMode
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveSettingsUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.SetThemeModeUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ToggleMultiSelectUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ToggleSoundUseCase
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ToggleVibrationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val toggleSound: ToggleSoundUseCase,
    private val toggleVibration: ToggleVibrationUseCase,
    private val toggleMultiSelect: ToggleMultiSelectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update { it.copy(isLoading = false, settings = settings) }
            }
        }
    }

    fun onThemeToggled(isDarkNow: Boolean) {
        viewModelScope.launch {
            setThemeMode(if (isDarkNow) ThemeMode.LIGHT else ThemeMode.DARK)
        }
    }

    fun onSoundChanged(enabled: Boolean) {
        viewModelScope.launch { toggleSound(enabled) }
    }

    fun onVibrationChanged(enabled: Boolean) {
        viewModelScope.launch { toggleVibration(enabled) }
    }

    fun onMultiSelectChanged(enabled: Boolean) {
        viewModelScope.launch { toggleMultiSelect(enabled) }
    }
}
