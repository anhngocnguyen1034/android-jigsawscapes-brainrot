package com.nnastudio.jigsawpuzzlebrainrot.domain.repository

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame

interface SavedGameRepository {
    suspend fun load(puzzleId: String, difficulty: Difficulty): SavedGame?
    suspend fun save(game: SavedGame)
    suspend fun clear(puzzleId: String, difficulty: Difficulty)
}
