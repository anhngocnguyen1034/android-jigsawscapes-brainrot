package com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Danh sach anh nguoi choi da danh dau yeu thich, luu thanh mot tap id trong DataStore. */
@Singleton
class FavoriteDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val favorites: Flow<Set<String>> = dataStore.data.map { it[FAVORITES] ?: emptySet() }

    /** Dao trang thai yeu thich cua mot buc anh. */
    suspend fun toggle(puzzleId: String) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITES] ?: emptySet()
            prefs[FAVORITES] = if (puzzleId in current) {
                current - puzzleId
            } else {
                current + puzzleId
            }
        }
    }

    private companion object {
        val FAVORITES = stringSetPreferencesKey("favorite_puzzles")
    }
}
