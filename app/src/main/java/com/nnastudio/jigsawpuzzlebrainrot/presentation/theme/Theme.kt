package com.nnastudio.jigsawpuzzlebrainrot.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ThemeMode

/** Cac token mau nam ngoai MD3 (dung cho switch sang/toi va vien the manh ghep). */
@Immutable
data class AnhnnExtraColors(
    val border: Color,
    val neonBlue: Color = Color(0xFF00E5FF),
    val neonYellow: Color = Color(0xFFFFE500),
    val boardSlot: Color
)

private val LightExtraColors = AnhnnExtraColors(
    border = Color(0xFFE0E0E0),
    boardSlot = Color(0xFFF1EFFB)
)

private val DarkExtraColors = AnhnnExtraColors(
    border = Color(0xFF444444),
    boardSlot = Color(0xFF232130)
)

val LocalAnhnnColors = staticCompositionLocalOf { LightExtraColors }

object AnhnnTheme {
    val extraColors: AnhnnExtraColors
        @Composable get() = LocalAnhnnColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = AnhnnPurpleLight,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = SurfaceDark,
    surface = SurfaceDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = AnhnnPurpleDark,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = SurfaceLight,
    surface = SurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight
)

/**
 * Theme goc cua app. Khong dung dynamic color de gradient Anhnn luon nhat quan
 * tren moi thiet bi.
 */
@Composable
fun AnhnnTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAnhnnColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
