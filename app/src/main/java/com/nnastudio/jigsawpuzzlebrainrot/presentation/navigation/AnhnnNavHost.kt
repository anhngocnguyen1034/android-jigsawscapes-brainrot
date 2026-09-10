package com.nnastudio.jigsawpuzzlebrainrot.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.game.GameScreen
import com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.home.HomeScreen
import com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.preview.PuzzlePreviewScreen
import com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.settings.SettingsScreen

@Composable
fun AnhnnNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                // The anh mo man xem truoc; chi anh nguoi choi tu chon trong may moi vao
                // thang van (khong co gi de xem truoc ngoai chinh anh vua chon).
                onPuzzleClick = { puzzleId, difficulty ->
                    navController.navigate(PuzzlePreviewRoute(puzzleId, difficulty.id))
                },
                onSettingsClick = { navController.navigate(SettingsRoute) }
            )
        }
        composable<PuzzlePreviewRoute> { entry ->
            val route = entry.toRoute<PuzzlePreviewRoute>()
            PuzzlePreviewScreen(
                onPlay = { difficulty ->
                    navController.navigate(GameRoute(route.puzzleId, difficulty.id))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<GameRoute> {
            GameScreen(onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
