package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

/** Nguon anh de cat manh ghep - app offline nen anh chi den tu `assets/`. */
@Immutable
sealed interface PuzzleSource {
    /** Anh nam trong `assets/` cua app. */
    data class Asset(val path: String) : PuzzleSource
}
