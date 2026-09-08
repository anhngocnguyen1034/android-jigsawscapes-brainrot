package com.nnastudio.jigsawpuzzlebrainrot.presentation.navigation

import android.util.Base64
import kotlinx.serialization.Serializable

/** Cac dich chuyen man hinh - type-safe bang kotlinx.serialization. */
@Serializable
data object HomeRoute

/**
 * [encodedImageUri] khac null khi nguoi choi tu chon anh trong may. URI duoc ma hoa
 * base64 URL-safe de cac ky tu `/` `:` `?` khong lam vo duong dan navigation.
 */
@Serializable
data class GameRoute(
    val puzzleId: String,
    val difficultyId: String,
    val encodedImageUri: String? = null
)

@Serializable
data object SettingsRoute

private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

fun encodeDeviceImageUri(uri: String): String =
    Base64.encodeToString(uri.toByteArray(Charsets.UTF_8), BASE64_FLAGS)

fun decodeDeviceImageUri(encoded: String): String? = runCatching {
    String(Base64.decode(encoded, BASE64_FLAGS), Charsets.UTF_8)
}.getOrNull()
