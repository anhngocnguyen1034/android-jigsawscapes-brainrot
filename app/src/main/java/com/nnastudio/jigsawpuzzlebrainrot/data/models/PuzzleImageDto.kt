package com.nnastudio.jigsawpuzzlebrainrot.data.models

import kotlinx.serialization.Serializable

/**
 * DTO doc tu file catalog dong goi trong `assets/`. Giu tach biet voi domain model
 * de doi cau truc file khong lam anh huong toi UI.
 */
@Serializable
data class PuzzleImageDto(
    val id: String,
    val title: String,
    val category: String,
    val assetPath: String,
    val premium: Boolean = false
)

@Serializable
data class PuzzleCatalogDto(
    val version: Int = 1,
    val puzzles: List<PuzzleImageDto> = emptyList()
)
