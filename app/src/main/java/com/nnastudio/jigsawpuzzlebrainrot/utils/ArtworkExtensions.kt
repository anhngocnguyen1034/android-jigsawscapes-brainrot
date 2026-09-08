package com.nnastudio.jigsawpuzzlebrainrot.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleArtwork

/** Domain giu bitmap duoi dang [Any]; lop UI quy doi ve ImageBitmap tai day. */
fun PuzzleArtwork.toImageBitmap(): ImageBitmap = (nativeImage as Bitmap).asImageBitmap()
