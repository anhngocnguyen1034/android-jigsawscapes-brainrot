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
    MASTER("master", 8, 8);

    val pieceCount: Int get() = rows * cols

    companion object {
        fun fromId(id: String?): Difficulty = entries.firstOrNull { it.id == id } ?: DEFAULT

        val DEFAULT = MASTER
    }
}
