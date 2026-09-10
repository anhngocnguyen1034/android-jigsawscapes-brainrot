package com.nnastudio.jigsawpuzzlebrainrot.data.repository

import com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local.SavedGameDataSource
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.SavedGame
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.SavedGameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Doc / ghi khong nem loi ra ngoai: van dang choi la tien nghi, hong ban luu thi nguoi choi
 * choi lai tu dau chu man hinh khong duoc vi the ma sap.
 */
@Singleton
class SavedGameRepositoryImpl @Inject constructor(
    private val savedGameDataSource: SavedGameDataSource
) : SavedGameRepository {

    override fun observeAll(): Flow<List<SavedGame>> =
        savedGameDataSource.observeAll().catch { emit(emptyList()) }

    override suspend fun load(puzzleId: String, difficulty: Difficulty): SavedGame? =
        runCatching { savedGameDataSource.load(puzzleId, difficulty) }.getOrNull()

    override suspend fun save(game: SavedGame) {
        runCatching { savedGameDataSource.save(game) }
    }

    override suspend fun clear(puzzleId: String, difficulty: Difficulty) {
        runCatching { savedGameDataSource.clear(puzzleId, difficulty) }
    }
}
