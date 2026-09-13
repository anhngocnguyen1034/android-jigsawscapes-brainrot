package com.nnastudio.jigsawpuzzlebrainrot.presentation.theme

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.BoardBackground

/**
 * Mau nen ban choi ung voi tung lua chon. [BoardBackground.DEFAULT] di theo theme sang /
 * toi cua app, cac lua chon con lai co mau co dinh de nguoi choi thay dung mau da chon.
 */
@Composable
fun BoardBackground.color(): Color = when (this) {
    BoardBackground.DEFAULT -> MaterialTheme.colorScheme.background
    BoardBackground.CREAM -> Color(0xFFF6EBD4)
    BoardBackground.WOOD -> Color(0xFF6E4A2E)
    BoardBackground.GREY -> Color(0xFFAEB4BE)
    BoardBackground.NIGHT -> Color(0xFF16203A)
    BoardBackground.PURPLE -> Color(0xFF2E2555)
}

/** Mau ve khung ghep: ruot khung va net vien cua no. */
@Immutable
data class BoardSlotColors(val fill: Color, val border: Color)

/**
 * Mau khung ghep tren tung nen ban choi.
 *
 * Khung khong co mau rieng ma chi lam nen dam hoac sang hon mot chut, vi nen ban choi co
 * ca loai sang (kem, mac dinh o theme sang) lan loai toi (go, dem, tim): mot mau co dinh
 * se chim han vao mot trong hai. Them net vien cho ro dau la cho tha manh, nhat thoi -
 * khung la cho de ghep, khong phai thu de nhin.
 */
@Composable
fun BoardBackground.slotColors(): BoardSlotColors {
    val onLightBackground = color().luminance() > LIGHT_BACKGROUND_LUMINANCE
    val ink = if (onLightBackground) Color.Black else Color.White
    return BoardSlotColors(
        fill = ink.copy(alpha = if (onLightBackground) 0.05f else 0.08f),
        border = ink.copy(alpha = if (onLightBackground) 0.14f else 0.22f)
    )
}

/** Tren muc nay coi la nen sang, khung phai dam hon nen moi noi. */
private const val LIGHT_BACKGROUND_LUMINANCE = 0.4f

@get:StringRes
val BoardBackground.labelRes: Int
    get() = when (this) {
        BoardBackground.DEFAULT -> R.string.background_default
        BoardBackground.CREAM -> R.string.background_cream
        BoardBackground.WOOD -> R.string.background_wood
        BoardBackground.GREY -> R.string.background_grey
        BoardBackground.NIGHT -> R.string.background_night
        BoardBackground.PURPLE -> R.string.background_purple
    }
