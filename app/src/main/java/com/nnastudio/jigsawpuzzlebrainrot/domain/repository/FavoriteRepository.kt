package com.nnastudio.jigsawpuzzlebrainrot.domain.repository

import kotlinx.coroutines.flow.Flow

/** Bo suu tap cua nguoi choi: cac buc anh duoc danh dau yeu thich. */
interface FavoriteRepository {
    fun observeFavorites(): Flow<Set<String>>
    suspend fun toggle(puzzleId: String)
}
