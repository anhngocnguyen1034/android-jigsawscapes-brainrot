package com.nnastudio.jigsawpuzzlebrainrot.domain.repository

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.AppSettings
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setVibrationEnabled(enabled: Boolean)
    suspend fun setLanguageCode(code: String)
}
