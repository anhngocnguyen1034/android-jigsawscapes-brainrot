package com.nnastudio.jigsawpuzzlebrainrot.presentation.theme

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.BoardBackground

/**
 * Bang mau cua man choi ung voi mot lua chon nen. Doi nen la doi ca bon thanh phan nay mot
 * luc, nen ca man choi luon la mot bo hoan chinh chu khong phai mot mau nen thay ao.
 *
 * Nen co ca loai sang (kem, mac dinh o theme sang) lan loai toi (go, dem, tim), vi vay
 * khong the lay mot mau co dinh cho thanh cong cu hay khung ghep: mau nao cung se chim vao
 * mot trong hai loai. Moi nen vi the tu mang du mau cua minh.
 */
@Immutable
data class BoardTheme(
    /** Nen cua ca man choi. */
    val background: Color,
    /** Nen thanh cong cu tren va khay manh duoi - hai thanh luon chung mot mau. */
    val bar: Color,
    /** Icon va chu tren hai thanh do. */
    val icon: Color,
    /**
     * Khung ghep. Mau nay trong mot phan (nhin xuyen ra nen), va net vien cua khung lay
     * chinh no dam len - xem [BoardTheme.slotBorder].
     */
    val slot: Color
) {
    /** Net vien khung ghep: dung mau [slot] nhung dam han, de ro dau la cho tha manh. */
    val slotBorder: Color get() = slot.copy(alpha = (slot.alpha * 2.8f).coerceAtMost(1f))
}

/**
 * Bang mau ung voi tung lua chon nen. [BoardBackground.DEFAULT] di theo theme sang / toi
 * cua app, cac lua chon con lai co mau co dinh de nguoi choi thay dung mau da chon.
 */
@Composable
fun BoardBackground.theme(): BoardTheme = when (this) {
    BoardBackground.DEFAULT -> BoardTheme(
        background = MaterialTheme.colorScheme.background,
        bar = AnhnnTheme.extraColors.boardSlot,
        icon = MaterialTheme.colorScheme.onSurface,
        slot = AnhnnTheme.extraColors.slotOnSurface
    )

    BoardBackground.CREAM -> BoardTheme(
        background = Color(0xFFF6EBD4),
        bar = Color(0xFFEADCBB),
        icon = Color(0xFF4A3A22),
        slot = Color.Black.copy(alpha = 0.06f)
    )

    BoardBackground.WOOD -> BoardTheme(
        background = Color(0xFF6E4A2E),
        bar = Color(0xFF573923),
        icon = Color(0xFFF4E8DB),
        slot = Color.White.copy(alpha = 0.08f)
    )

    BoardBackground.GREY -> BoardTheme(
        background = Color(0xFFAEB4BE),
        bar = Color(0xFF98A0AC),
        icon = Color(0xFF1F2430),
        slot = Color.Black.copy(alpha = 0.07f)
    )

    BoardBackground.NIGHT -> BoardTheme(
        background = Color(0xFF16203A),
        bar = Color(0xFF1F2C4E),
        icon = Color(0xFFD6E1FF),
        slot = Color.White.copy(alpha = 0.08f)
    )

    BoardBackground.PURPLE -> BoardTheme(
        background = Color(0xFF2E2555),
        bar = Color(0xFF3B3070),
        icon = Color(0xFFE4DDFF),
        slot = Color.White.copy(alpha = 0.09f)
    )
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
