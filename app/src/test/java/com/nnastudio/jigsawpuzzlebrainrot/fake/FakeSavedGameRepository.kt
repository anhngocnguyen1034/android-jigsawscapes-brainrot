package com.nnastudio.jigsawpuzzlebrainrot.fake

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.SavedGameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSavedGameRepository : SavedGameRepository {

    private val games = MutableStateFlow<List<SavedGame>>(emptyList())

    override fun observeAll(): Flow<List<SavedGame>> = games

    override suspend fun load(puzzleId: String, difficulty: Difficulty): SavedGame? =
        games.value.firstOrNull { it.puzzleId == puzzleId && it.difficulty == difficulty }

    override suspend fun save(game: SavedGame) {
        games.value = games.value.filterNot {
            it.puzzleId == game.puzzleId && it.difficulty == game.difficulty
        } + game
    }

    override suspend fun clear(puzzleId: String, difficulty: Difficulty) {
        games.value = games.value.filterNot {
            it.puzzleId == puzzleId && it.difficulty == difficulty
        }
    }
}
