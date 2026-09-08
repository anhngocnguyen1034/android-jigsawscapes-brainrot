package com.nnastudio.jigsawpuzzlebrainrot.domain.usecases

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.SavedGameRepository
import javax.inject.Inject

class LoadSavedGameUseCase @Inject constructor(
    private val savedGameRepository: SavedGameRepository
) {
    suspend operator fun invoke(puzzleId: String, difficulty: Difficulty): SavedGame? =
        savedGameRepository.load(puzzleId, difficulty)
}

class SaveGameUseCase @Inject constructor(
    private val savedGameRepository: SavedGameRepository
) {
    suspend operator fun invoke(game: SavedGame) = savedGameRepository.save(game)
}

class ClearSavedGameUseCase @Inject constructor(
    private val savedGameRepository: SavedGameRepository
) {
    suspend operator fun invoke(puzzleId: String, difficulty: Difficulty) =
        savedGameRepository.clear(puzzleId, difficulty)
}
