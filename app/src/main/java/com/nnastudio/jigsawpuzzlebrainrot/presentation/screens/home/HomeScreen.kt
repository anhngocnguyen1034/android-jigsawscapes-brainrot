package com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ThemeMode
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.AnhnnGradientButton
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.DifficultySelector
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.PuzzleCard
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnTheme
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.HomeUiState
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.HomeViewModel
import com.nnastudio.jigsawpuzzlebrainrot.utils.formatAsClock

@Composable
fun HomeScreen(
    onPuzzleClick: (puzzleId: String, difficulty: Difficulty) -> Unit,
    onDeviceImagePicked: (uri: String, difficulty: Difficulty) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onDeviceImagePicked(uri.toString(), uiState.selectedDifficulty)
    }

    HomeContent(
        uiState = uiState,
        onDifficultySelected = viewModel::onDifficultySelected,
        onPuzzleClick = { puzzleId -> onPuzzleClick(puzzleId, uiState.selectedDifficulty) },
        onPickImageClick = {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onSettingsClick = onSettingsClick
    )
}

/** Phan UI thuan tuy - khong biet gi ve ViewModel, de test va preview. */
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onDifficultySelected: (Difficulty) -> Unit,
    onPuzzleClick: (String) -> Unit,
    onPickImageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = onSettingsClick) {
                Text(text = stringResource(R.string.action_settings))
            }
        }

        DifficultySelector(
            selected = uiState.selectedDifficulty,
            onSelect = onDifficultySelected,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        AnhnnGradientButton(
            text = stringResource(R.string.action_pick_image),
            onClick = onPickImageClick,
            modifier = Modifier.fillMaxWidth()
        )

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.errorMessageRes != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text(text = stringResource(uiState.errorMessageRes)) }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = uiState.puzzles, key = { it.id }) { puzzle ->
                    val best = uiState.progress[puzzle.id]
                    PuzzleCard(
                        puzzle = puzzle,
                        onClick = { onPuzzleClick(puzzle.id) },
                        bestTimeLabel = best?.bestTimeSeconds?.formatAsClock()
                    )
                }
            }
        }
    }
}

@Preview(name = "Light")
@Composable
private fun HomeContentLightPreview() {
    AnhnnTheme(themeMode = ThemeMode.LIGHT) {
        HomeContent(
            uiState = HomeUiState(isLoading = false),
            onDifficultySelected = {},
            onPuzzleClick = {},
            onPickImageClick = {},
            onSettingsClick = {}
        )
    }
}

@Preview(name = "Dark")
@Composable
private fun HomeContentDarkPreview() {
    AnhnnTheme(themeMode = ThemeMode.DARK) {
        HomeContent(
            uiState = HomeUiState(isLoading = false),
            onDifficultySelected = {},
            onPuzzleClick = {},
            onPickImageClick = {},
            onSettingsClick = {}
        )
    }
}
