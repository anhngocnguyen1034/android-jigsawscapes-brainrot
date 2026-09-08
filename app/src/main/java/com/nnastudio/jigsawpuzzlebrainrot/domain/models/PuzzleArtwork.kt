package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

/**
 * Anh da duoc giai ma va cat vuong, san sang de ve len ban co.
 *
 * [nativeImage] la bitmap cua nen tang (android.graphics.Bitmap). Domain khong tham chieu
 * truc tiep kieu Android; lop presentation ep kieu qua `toImageBitmap()`.
 */
@Immutable
class PuzzleArtwork(
    val width: Int,
    val height: Int,
    val nativeImage: Any
)
