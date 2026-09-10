package com.nnastudio.jigsawpuzzlebrainrot.presentation.navigation

import kotlinx.serialization.Serializable

/** Cac dich chuyen man hinh - type-safe bang kotlinx.serialization. */
@Serializable
data object HomeRoute

@Serializable
data class GameRoute(
    val puzzleId: String,
    val difficultyId: String
)

/** Man xem truoc anh (bo manh da ghep xong) truoc khi vao van. */
@Serializable
data class PuzzlePreviewRoute(
    val puzzleId: String,
    val difficultyId: String
)

@Serializable
data object SettingsRoute
