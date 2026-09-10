package com.nnastudio.jigsawpuzzlebrainrot.presentation.theme

import androidx.annotation.StringRes
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory

/** Ten hien thi cua tung bo suu tap anh. */
@get:StringRes
val PuzzleCategory.labelRes: Int
    get() = when (this) {
        PuzzleCategory.BRAINROT -> R.string.category_brainrot
        PuzzleCategory.MEME -> R.string.category_meme
        PuzzleCategory.ANIMAL -> R.string.category_animal
        PuzzleCategory.NATURE -> R.string.category_nature
    }
