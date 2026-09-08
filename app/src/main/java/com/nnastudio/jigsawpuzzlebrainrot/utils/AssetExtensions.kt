package com.nnastudio.jigsawpuzzlebrainrot.utils

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage

/** Coil doc file trong assets qua scheme `file:///android_asset/`. */
fun PuzzleImage.assetUri(): String = "file:///android_asset/$assetPath"
