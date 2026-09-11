package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Rung va tieng khi choi, bat / tat theo hai cong tac trong man cai dat.
 *
 * Dung rung va tieng cua he thong chu khong phai file am thanh rieng: du de manh vao o co
 * phan hoi, khong phai them tep nhi phan vao ban cai. Khi nao co bo am thanh rieng thi thay
 * ruot hai ham duoi day, cho goi o man choi giu nguyen.
 *
 * Nguoi choi tat tieng cham trong cai dat he thong thi [View.playSoundEffect] tu im - dung
 * nhu moi ung dung khac tren may.
 */
@Immutable
class GameFeedback(
    private val view: View,
    private val soundEnabled: Boolean,
    private val vibrationEnabled: Boolean
) {
    /** Mot manh (hay ca khoi) vua vao dung o. */
    fun onPiecePlaced() {
        if (vibrationEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        if (soundEnabled) view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    /** Vua ghep xong ca buc: an mung dam hon mot nhip manh le. */
    fun onSolved() {
        if (vibrationEnabled) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        if (soundEnabled) view.playSoundEffect(SoundEffectConstants.CLICK)
    }
}

@Composable
fun rememberGameFeedback(soundEnabled: Boolean, vibrationEnabled: Boolean): GameFeedback {
    val view = LocalView.current
    return remember(view, soundEnabled, vibrationEnabled) {
        GameFeedback(view, soundEnabled, vibrationEnabled)
    }
}
