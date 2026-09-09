package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val languageCode: String = "en",
    /** Nen ban choi nguoi choi da chon, xem [BoardBackground]. */
    val boardBackground: BoardBackground = BoardBackground.DEFAULT
)

enum class ThemeMode(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromValue(value: String?): ThemeMode = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}
