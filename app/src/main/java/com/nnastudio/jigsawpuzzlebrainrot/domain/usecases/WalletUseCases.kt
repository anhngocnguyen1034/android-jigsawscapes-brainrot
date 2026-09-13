package com.nnastudio.jigsawpuzzlebrainrot.domain.usecases

import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePointsUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    operator fun invoke(): Flow<Int> = walletRepository.observePoints()
}

class ObserveUnlockedPuzzlesUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    operator fun invoke(): Flow<Set<String>> = walletRepository.observeUnlockedPuzzles()
}

/** Cong diem vua an duoc sau mot van vao vi. */
class EarnPointsUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(points: Int) = walletRepository.earn(points)
}

/** Mua mot buc khoa bang diem; false neu khong du diem. */
class UnlockPuzzleUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(puzzleId: String, price: Int): Boolean =
        walletRepository.unlock(puzzleId, price)
}
