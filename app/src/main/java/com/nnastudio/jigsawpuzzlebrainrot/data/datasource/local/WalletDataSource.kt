package com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vi diem cua nguoi choi: so diem con lai va nhung buc khoa da mua.
 *
 * Diem luu o day la so du (da cong diem an duoc, da tru tien mua) chu khong phai tong diem
 * kiem duoc - diem cao nhat cua tung buc van nam trong ban ghi tien do rieng.
 */
@Singleton
class WalletDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val points: Flow<Int> = dataStore.data.map { it[POINTS] ?: 0 }

    val unlockedPuzzles: Flow<Set<String>> = dataStore.data.map { it[UNLOCKED] ?: emptySet() }

    /** Cong diem vua an duoc sau mot van. */
    suspend fun earn(points: Int) {
        if (points <= 0) return
        dataStore.edit { prefs -> prefs[POINTS] = (prefs[POINTS] ?: 0) + points }
    }

    /**
     * Mua mot buc khoa. Tra ve false neu khong du diem hay buc da mo tu truoc - viec doc so
     * du, tru diem va ghi buc da mo nam gon trong mot lan `edit` nen hai cu bam lien tiep
     * khong the mua hai lan bang mot lan tra diem.
     */
    suspend fun unlock(puzzleId: String, price: Int): Boolean {
        var bought = false
        dataStore.edit { prefs ->
            val unlocked = prefs[UNLOCKED] ?: emptySet()
            val balance = prefs[POINTS] ?: 0
            if (puzzleId in unlocked || balance < price) return@edit
            prefs[POINTS] = balance - price
            prefs[UNLOCKED] = unlocked + puzzleId
            bought = true
        }
        return bought
    }

    private companion object {
        val POINTS = intPreferencesKey("wallet_points")
        val UNLOCKED = stringSetPreferencesKey("unlocked_puzzles")
    }
}
