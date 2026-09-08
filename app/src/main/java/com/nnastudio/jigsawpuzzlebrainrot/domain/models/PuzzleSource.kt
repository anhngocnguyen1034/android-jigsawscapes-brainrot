package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

/** Nguon anh de cat manh ghep - app offline nen chi co anh dong goi hoac anh cua may. */
@Immutable
sealed interface PuzzleSource {
    /** Anh nam trong `assets/` cua app. */
    data class Asset(val path: String) : PuzzleSource

    /** Anh nguoi dung chon tu may (content:// hoac file://). */
    data class Device(val uri: String) : PuzzleSource
}
