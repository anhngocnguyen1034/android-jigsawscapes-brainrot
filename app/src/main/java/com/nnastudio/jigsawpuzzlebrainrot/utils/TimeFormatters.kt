package com.nnastudio.jigsawpuzzlebrainrot.utils

import java.util.Locale

/** 125 -> "02:05" */
fun Int.formatAsClock(): String =
    String.format(Locale.US, "%02d:%02d", this / 60, this % 60)
