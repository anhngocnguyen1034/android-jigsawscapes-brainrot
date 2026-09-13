package com.nnastudio.jigsawpuzzlebrainrot.fake

import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeWalletRepository(points: Int = 0) : WalletRepository {

    private val balance = MutableStateFlow(points)
    private val unlocked = MutableStateFlow<Set<String>>(emptySet())

    override fun observePoints(): Flow<Int> = balance

    override fun observeUnlockedPuzzles(): Flow<Set<String>> = unlocked

    override suspend fun earn(points: Int) {
        balance.value += points
    }

    override suspend fun unlock(puzzleId: String, price: Int): Boolean {
        if (puzzleId in unlocked.value || balance.value < price) return false
        balance.value -= price
        unlocked.value = unlocked.value + puzzleId
        return true
    }
}
