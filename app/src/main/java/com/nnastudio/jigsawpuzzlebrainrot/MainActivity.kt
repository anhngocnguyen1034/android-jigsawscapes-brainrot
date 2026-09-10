package com.nnastudio.jigsawpuzzlebrainrot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nnastudio.jigsawpuzzlebrainrot.presentation.navigation.AnhnnNavHost
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnTheme
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        setContent {
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            AnhnnTheme(themeMode = themeMode) {
                // Scaffold khong tu chua cho thanh he thong: thanh dang an nen inset cua no
                // bang 0, moi man hinh tu chua cho theo *IgnoringVisibility de noi dung khong
                // tran len cho status bar (notch / camera) ma van dung duoc phan cua nav bar.
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0)
                ) { innerPadding ->
                    AnhnnNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    /** Bars hien tam thoi khi nguoi dung vuot vao, va co the con lai sau khi mat focus. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /**
     * An ca status bar lan nav bar cho toan app: choi ghep hinh khong can dong ho / pin / nut
     * dieu huong. Vuot tu mep van goi chung ra tam thoi.
     */
    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
