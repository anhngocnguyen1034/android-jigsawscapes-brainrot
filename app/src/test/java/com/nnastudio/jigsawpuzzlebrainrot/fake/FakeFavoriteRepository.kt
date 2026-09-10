package com.nnastudio.jigsawpuzzlebrainrot.fake

import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFavoriteRepository : FavoriteRepository {

    private val favorites = MutableStateFlow<Set<String>>(emptySet())

    override fun observeFavorites(): Flow<Set<String>> = favorites

    override suspend fun toggle(puzzleId: String) {
        favorites.value = if (puzzleId in favorites.value) {
            favorites.value - puzzleId
        } else {
            favorites.value + puzzleId
        }
    }
}
