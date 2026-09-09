package com.nnastudio.jigsawpuzzlebrainrot.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.game.GameScreen
import com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.home.HomeScreen
import com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.settings.SettingsScreen

const val DEVICE_IMAGE_PUZZLE_ID = "device_image"

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
                onPuzzleClick = { puzzleId, difficulty ->
                    navController.navigate(GameRoute(puzzleId, difficulty.id))
                },
                onDeviceImagePicked = { uri, difficulty ->
                    navController.navigate(
                        GameRoute(
                            puzzleId = DEVICE_IMAGE_PUZZLE_ID,
                            difficultyId = difficulty.id,
                            encodedImageUri = encodeDeviceImageUri(uri)
                        )
                    )
                },
                onSettingsClick = { navController.navigate(SettingsRoute) }
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
