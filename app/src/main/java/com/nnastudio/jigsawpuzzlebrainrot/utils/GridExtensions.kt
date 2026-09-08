package com.nnastudio.jigsawpuzzlebrainrot.utils

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty

fun Difficulty.rowOf(slot: Int): Int = slot / cols

fun Difficulty.colOf(slot: Int): Int = slot % cols
