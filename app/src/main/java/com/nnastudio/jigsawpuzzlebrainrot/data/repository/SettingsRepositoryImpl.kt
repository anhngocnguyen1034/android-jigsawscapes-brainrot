package com.nnastudio.jigsawpuzzlebrainrot.data.repository

import com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local.SettingsDataSource
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.AppSettings
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.BoardBackground
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ThemeMode
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataSource: SettingsDataSource
) : SettingsRepository {

    override val settings: Flow<AppSettings> =
        settingsDataSource.settings.catch { emit(AppSettings()) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        runCatching { settingsDataSource.setThemeMode(mode) }
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        runCatching { settingsDataSource.setSoundEnabled(enabled) }
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        runCatching { settingsDataSource.setVibrationEnabled(enabled) }
    }

    override suspend fun setMultiSelectEnabled(enabled: Boolean) {
        runCatching { settingsDataSource.setMultiSelectEnabled(enabled) }
    }

    override suspend fun setLanguageCode(code: String) {
        runCatching { settingsDataSource.setLanguageCode(code) }
    }

    override suspend fun setBoardBackground(background: BoardBackground) {
        runCatching { settingsDataSource.setBoardBackground(background) }
    }
}
