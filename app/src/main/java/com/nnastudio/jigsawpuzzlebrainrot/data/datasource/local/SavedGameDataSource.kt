package com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nnastudio.jigsawpuzzlebrainrot.data.models.SavedGameDto
import com.nnastudio.jigsawpuzzlebrainrot.data.models.toDomain
import com.nnastudio.jigsawpuzzlebrainrot.data.models.toDto
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Luu van dang choi vao DataStore theo key "<puzzleId>|<difficulty>" - mot ban luu cho moi
 * cap (anh, do kho), nen doi anh hay doi do kho khong xoa van dang choi do.
 */
@Singleton
class SavedGameDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {

    /** Ban luu cua van nay, null neu chua co hoac ban luu doc khong duoc. */
    suspend fun load(puzzleId: String, difficulty: Difficulty): SavedGame? {
        val raw = dataStore.data.first()[gameKey(puzzleId, difficulty)] ?: return null
        return runCatching { json.decodeFromString<SavedGameDto>(raw).toDomain() }.getOrNull()
    }

    suspend fun save(game: SavedGame) {
        val raw = json.encodeToString(SavedGameDto.serializer(), game.toDto())
        dataStore.edit { it[gameKey(game.puzzleId, game.difficulty)] = raw }
    }

    suspend fun clear(puzzleId: String, difficulty: Difficulty) {
        dataStore.edit { it.remove(gameKey(puzzleId, difficulty)) }
    }

    private companion object {
        const val PREFIX = "saved_game_"

        fun gameKey(puzzleId: String, difficulty: Difficulty) =
            stringPreferencesKey("$PREFIX$puzzleId|${difficulty.id}")
    }
}
