package com.nnastudio.jigsawpuzzlebrainrot.presentation.theme

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
