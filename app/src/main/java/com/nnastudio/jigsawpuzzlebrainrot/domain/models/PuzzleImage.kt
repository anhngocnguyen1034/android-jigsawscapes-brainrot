package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

/**
 * Mot buc anh trong catalog. App khong co backend nen [assetPath] tro toi file
 * trong `assets/puzzles/`, con [drawableRes] danh cho anh dong goi san (0 = khong co).
 */
@Immutable
data class PuzzleImage(
    val id: String,
    val title: String,
    val category: PuzzleCategory,
    val assetPath: String,
    val drawableRes: Int = 0,
    val isPremium: Boolean = false
)

@Immutable
enum class PuzzleCategory(val id: String) {
    BRAINROT("brainrot"),
    MEME("meme"),
    ANIMAL("animal"),
    NATURE("nature")
}
