package com.nnastudio.jigsawpuzzlebrainrot.domain.usecases

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.SavedGameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Tien do cua cac van dang do dang: puzzleId -> % da ghep, lay van cao nhat cua moi anh. */
class ObserveSavedProgressUseCase @Inject constructor(
    private val savedGameRepository: SavedGameRepository
) {
    operator fun invoke(): Flow<Map<String, Int>> =
        savedGameRepository.observeAll().map { games ->
            games.groupBy { it.puzzleId }
                .mapValues { (_, saved) -> saved.maxOf { it.progressPercent } }
        }
}

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
