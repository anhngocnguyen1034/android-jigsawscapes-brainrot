package com.nnastudio.jigsawpuzzlebrainrot.domain.repository

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame
import kotlinx.coroutines.flow.Flow

interface SavedGameRepository {
    /** Moi van dang choi do dang - man hinh chinh dung de hien % tien do tren the anh. */
    fun observeAll(): Flow<List<SavedGame>>

    suspend fun load(puzzleId: String, difficulty: Difficulty): SavedGame?
    suspend fun save(game: SavedGame)
    suspend fun clear(puzzleId: String, difficulty: Difficulty)
}
