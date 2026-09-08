package com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ThemeMode
import com.nnastudio.jigsawpuzzlebrainrot.domain.usecases.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Giu theme mode o cap Activity de AnhnnTheme doi mau ngay khi user bat/tat. */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = observeSettings()
        .map { it.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM
        )
}
