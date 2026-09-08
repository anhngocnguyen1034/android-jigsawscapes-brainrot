package com.nnastudio.jigsawpuzzlebrainrot.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush

/** Gradient tim dac trung cua he sinh thai Anhnn. */
object AnhnnGradients {

    @Composable
    fun primaryVertical(): Brush = remember {
        Brush.verticalGradient(colors = listOf(AnhnnPurpleLight, AnhnnPurpleDark))
    }

    @Composable
    fun primaryHorizontal(): Brush = remember {
        Brush.horizontalGradient(colors = listOf(AnhnnPurpleLight, AnhnnPurpleDark))
    }
}
