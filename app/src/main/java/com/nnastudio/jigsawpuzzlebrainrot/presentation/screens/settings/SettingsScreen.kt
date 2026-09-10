package com.nnastudio.jigsawpuzzlebrainrot.presentation.screens.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.AppSettings
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ThemeMode
import com.nnastudio.jigsawpuzzlebrainrot.presentation.components.AnhnnThemeSwitch
import com.nnastudio.jigsawpuzzlebrainrot.presentation.viewmodels.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val isDarkTheme = settings.themeMode == ThemeMode.DARK ||
        (settings.themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())

    SettingsContent(
        modifier = Modifier
            // Thanh he thong dang bi an cho ca app, nhung cho status bar co the la notch /
            // camera nen van chua cho no. Phan nav bar duoi thi dung duoc het.
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility),
        settings = settings,
        isDarkTheme = isDarkTheme,
        onThemeToggle = { viewModel.onThemeToggled(isDarkTheme) },
        onSoundChanged = viewModel::onSoundChanged,
        onVibrationChanged = viewModel::onVibrationChanged,
        onBack = onBack
    )
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text(text = stringResource(R.string.action_back)) }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge
            )
        }

        SettingRow(label = stringResource(R.string.settings_dark_mode)) {
            AnhnnThemeSwitch(isDarkTheme = isDarkTheme, onToggle = onThemeToggle)
        }
        SettingRow(label = stringResource(R.string.settings_sound)) {
            Switch(checked = settings.soundEnabled, onCheckedChange = onSoundChanged)
        }
        SettingRow(label = stringResource(R.string.settings_vibration)) {
            Switch(checked = settings.vibrationEnabled, onCheckedChange = onVibrationChanged)
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    control: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        control()
    }
}
