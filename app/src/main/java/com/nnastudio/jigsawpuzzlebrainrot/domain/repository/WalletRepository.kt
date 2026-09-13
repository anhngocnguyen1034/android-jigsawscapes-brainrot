package com.nnastudio.jigsawpuzzlebrainrot.domain.repository

import kotlinx.coroutines.flow.Flow

/** Vi diem: so du hien co va nhung buc khoa da mua bang diem. */
interface WalletRepository {
    fun observePoints(): Flow<Int>
    fun observeUnlockedPuzzles(): Flow<Set<String>>

    /** Cong diem an duoc sau mot van. */
    suspend fun earn(points: Int)

    /** Mua mot buc khoa; false neu khong du diem hay buc da mo tu truoc. */
    suspend fun unlock(puzzleId: String, price: Int): Boolean
}
