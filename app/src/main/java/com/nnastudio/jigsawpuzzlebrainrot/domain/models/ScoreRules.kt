package com.nnastudio.jigsawpuzzlebrainrot.domain.models

/**
 * Cach cong diem trong mot van: nguoi choi an diem moi lan ghep dung, khong phai moi lan
 * dong manh.
 *
 * Hai viec duoc tinh la ghep dung:
 * - hai manh (hay hai khoi) hut vao nhau dung tuong quan - [POINTS_PER_JOIN]. Day la nuoc
 *   di chinh cua van: manh khop voi manh ke ben, khong can dat dung o tren ban co;
 * - mot manh vao dung o cua no tren ban co va bi khoa lai - [POINTS_PER_PLACED].
 *
 * Goi y khong duoc cong diem: may lam thay nguoi choi. Con [POINTS_SOLVED] la thuong khi
 * xong ca buc anh, cong mot lan du xong bang cach nao.
 */
object ScoreRules {

    const val POINTS_PER_JOIN = 10
    const val POINTS_PER_PLACED = 20
    const val POINTS_SOLVED = 100

    /**
     * Diem an duoc khi van di tu [before] sang [after]. Chi cong chu khong tru: keo manh ra
     * hay tra manh ve khay khong lam mat diem da an.
     */
    fun gain(before: PuzzlePlayState, after: PuzzlePlayState): Int {
        val joins = (after.joinCount - before.joinCount).coerceAtLeast(0)
        val placed = (after.placedCount - before.placedCount).coerceAtLeast(0)
        return joins * POINTS_PER_JOIN + placed * POINTS_PER_PLACED
    }
}
