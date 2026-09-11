package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    /**
     * Cho phep cham chon nhieu manh trong khay roi dua ca nhom len ban choi mot luot.
     * Mac dinh tat: nguoi choi moi quen keo tung manh truoc da.
     */
    val multiSelectEnabled: Boolean = false,
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
