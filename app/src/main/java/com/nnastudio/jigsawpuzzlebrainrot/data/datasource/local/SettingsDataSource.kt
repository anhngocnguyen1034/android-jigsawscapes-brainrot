package com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.AppSettings
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.BoardBackground
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.fromValue(prefs[THEME_MODE]),
            soundEnabled = prefs[SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[VIBRATION_ENABLED] ?: true,
            languageCode = prefs[LANGUAGE_CODE] ?: "en",
            boardBackground = BoardBackground.fromValue(prefs[BOARD_BACKGROUND])
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[THEME_MODE] = mode.value }

    suspend fun setSoundEnabled(enabled: Boolean) = edit { it[SOUND_ENABLED] = enabled }

    suspend fun setVibrationEnabled(enabled: Boolean) = edit { it[VIBRATION_ENABLED] = enabled }

    suspend fun setLanguageCode(code: String) = edit { it[LANGUAGE_CODE] = code }

    suspend fun setBoardBackground(background: BoardBackground) =
        edit { it[BOARD_BACKGROUND] = background.value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val BOARD_BACKGROUND = stringPreferencesKey("board_background")
    }
}
