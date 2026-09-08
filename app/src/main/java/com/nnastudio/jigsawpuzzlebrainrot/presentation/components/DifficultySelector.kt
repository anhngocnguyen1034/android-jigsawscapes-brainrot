package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ThemeMode
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnGradients
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnTheme

@Composable
fun DifficultySelector(
    selected: Difficulty,
    onSelect: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Difficulty.entries.forEach { difficulty ->
            DifficultyChip(
                difficulty = difficulty,
                selected = difficulty == selected,
                onClick = { onSelect(difficulty) }
            )
        }
    }
}

@Composable
private fun DifficultyChip(
    difficulty: Difficulty,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        AnhnnGradients.primaryHorizontal()
    } else {
        SolidColor(MaterialTheme.colorScheme.surfaceVariant)
    }
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${difficulty.rows}x${difficulty.cols}",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(name = "Light")
@Composable
private fun DifficultySelectorLightPreview() {
    AnhnnTheme(themeMode = ThemeMode.LIGHT) {
        DifficultySelector(selected = Difficulty.MEDIUM, onSelect = {})
    }
}

@Preview(name = "Dark")
@Composable
private fun DifficultySelectorDarkPreview() {
    AnhnnTheme(themeMode = ThemeMode.DARK) {
        DifficultySelector(selected = Difficulty.MEDIUM, onSelect = {})
    }
}
