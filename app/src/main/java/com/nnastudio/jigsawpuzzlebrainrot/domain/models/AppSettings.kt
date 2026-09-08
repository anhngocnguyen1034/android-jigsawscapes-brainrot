package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val languageCode: String = "en"
)

enum class ThemeMode(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromValue(value: String?): ThemeMode = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}
