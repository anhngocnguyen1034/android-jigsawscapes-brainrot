package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

/** Do kho quyet dinh kich thuoc luoi ghep (grid = rows x cols). */
@Immutable
enum class Difficulty(val id: String, val rows: Int, val cols: Int) {
    EASY("easy", 3, 3),
    MEDIUM("medium", 4, 4),
    HARD("hard", 5, 5),
    EXPERT("expert", 6, 6),

    /** 64 manh - muc mac dinh, moi buc anh duoc cat thanh 8x8. */
    MASTER("master", 8, 8),

    /** 100 manh. */
    GRAND("grand", 10, 10),

    /** 225 manh. */
    EPIC("epic", 15, 15),

    /** 400 manh. */
    LEGEND("legend", 20, 20);

    val pieceCount: Int get() = rows * cols

    companion object {
        fun fromId(id: String?): Difficulty = entries.firstOrNull { it.id == id } ?: DEFAULT

        val DEFAULT = MASTER

        /**
         * Cac moc so manh nguoi choi duoc chon o man xem truoc. Cac muc con lai giu lai de
         * doc duoc ban luu / thanh tich cu.
         */
        val PIECE_OPTIONS = listOf(MASTER, GRAND, EPIC, LEGEND)
    }
}
