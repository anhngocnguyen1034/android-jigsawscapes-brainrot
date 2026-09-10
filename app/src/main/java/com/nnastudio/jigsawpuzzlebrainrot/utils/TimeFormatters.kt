package com.nnastudio.jigsawpuzzlebrainrot.utils

import java.util.Locale

/** 125 -> "02:05" */
fun Int.formatAsClock(): String =
    String.format(Locale.US, "%02d:%02d", this / 60, this % 60)

/** So ngay tinh tu epoch. minSdk 24 nen khong dung java.time. */
fun currentEpochDay(): Long = System.currentTimeMillis() / MILLIS_PER_DAY

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
