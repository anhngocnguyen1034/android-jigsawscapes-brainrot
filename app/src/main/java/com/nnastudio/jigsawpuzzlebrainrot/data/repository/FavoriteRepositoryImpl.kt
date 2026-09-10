package com.nnastudio.jigsawpuzzlebrainrot.data.repository

import com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local.FavoriteDataSource
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/** Hong ban ghi thi coi nhu chua yeu thich buc nao: khong lam sap man hinh vi chuyen nay. */
@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDataSource: FavoriteDataSource
) : FavoriteRepository {

    override fun observeFavorites(): Flow<Set<String>> =
        favoriteDataSource.favorites.catch { emit(emptySet()) }

    override suspend fun toggle(puzzleId: String) {
        runCatching { favoriteDataSource.toggle(puzzleId) }
    }
}
