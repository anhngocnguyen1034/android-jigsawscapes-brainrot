package com.nnastudio.jigsawpuzzlebrainrot.data.repository

import com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local.WalletDataSource
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/** Hong ban ghi thi coi nhu vi rong: khong lam sap man hinh chinh vi chuyen nay. */
@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val walletDataSource: WalletDataSource
) : WalletRepository {

    override fun observePoints(): Flow<Int> = walletDataSource.points.catch { emit(0) }

    override fun observeUnlockedPuzzles(): Flow<Set<String>> =
        walletDataSource.unlockedPuzzles.catch { emit(emptySet()) }

    override suspend fun earn(points: Int) {
        runCatching { walletDataSource.earn(points) }
    }

    // Ghi hong thi coi nhu chua mua: khong tru diem ma cung khong mo buc anh.
    override suspend fun unlock(puzzleId: String, price: Int): Boolean =
        runCatching { walletDataSource.unlock(puzzleId, price) }.getOrDefault(false)
}
