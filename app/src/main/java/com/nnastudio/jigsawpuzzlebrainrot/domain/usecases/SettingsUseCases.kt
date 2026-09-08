package com.nnastudio.jigsawpuzzlebrainrot.domain.usecases

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.AppSettings
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ThemeMode
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.settings
}

class SetThemeModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = settingsRepository.setThemeMode(mode)
}

class ToggleSoundUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = settingsRepository.setSoundEnabled(enabled)
}

class ToggleVibrationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = settingsRepository.setVibrationEnabled(enabled)
}
