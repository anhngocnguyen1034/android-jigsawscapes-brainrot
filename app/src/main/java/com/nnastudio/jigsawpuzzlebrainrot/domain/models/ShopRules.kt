package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import kotlin.math.ceil

/**
 * Gia mo khoa mot buc anh, tinh theo chinh so diem nguoi choi kiem duoc trong van - khong
 * phai mot con so dat tay. Doi cach cong diem trong [ScoreRules] thi gia tu chay theo, nen
 * hai ben khong bao gio lech nhau.
 *
 * Cach dat gia:
 * - [perfectRunScore] la so diem mot van ghep xong tra ve: moi manh vao dung o ([ScoreRules.
 *   POINTS_PER_PLACED]), moi moi noi giua hai khoi ([ScoreRules.POINTS_PER_JOIN] - ghep xong
 *   ca buc thi co dung `so manh - 1` moi noi) va thuong ket van ([ScoreRules.solvedBonus]);
 * - gia mot buc khoa = [RUNS_PER_UNLOCK] van nhu vay, lam tron len boi cua [PRICE_STEP] cho
 *   de nho.
 *
 * O moc manh mac dinh (8x8): mot van xong duoc 2330 diem, nen gia la 5000 diem - ghep xong
 * hai buc la mo duoc mot buc moi. Do la nhip ma nguoi choi luon thay dich den gan, nhung
 * van phai choi het mot buc chu khong nhan duoc buc khoa ngay tu van dau.
 */
object ShopRules {

    /** So van ghep xong (o moc manh mac dinh) de du diem mo mot buc khoa. */
    const val RUNS_PER_UNLOCK = 2

    /** Gia luon la boi cua so nay: gia tron nghin nhin de nho hon 4660. */
    const val PRICE_STEP = 500

    /** Diem cua mot van ghep xong tron ven o [difficulty]. */
    fun perfectRunScore(difficulty: Difficulty): Int {
        val pieces = difficulty.pieceCount
        return pieces * ScoreRules.POINTS_PER_PLACED +
            (pieces - 1) * ScoreRules.POINTS_PER_JOIN +
            ScoreRules.solvedBonus(difficulty)
    }

    /**
     * Gia mo khoa mot buc anh. Moi buc khoa deu cung gia: nguoi choi biet truoc con phai
     * ghep bao nhieu van nua, khong phai do gia tung buc mot.
     */
    fun unlockPrice(difficulty: Difficulty = Difficulty.DEFAULT): Int {
        val raw = perfectRunScore(difficulty) * RUNS_PER_UNLOCK
        return ceil(raw.toFloat() / PRICE_STEP).toInt() * PRICE_STEP
    }
}
