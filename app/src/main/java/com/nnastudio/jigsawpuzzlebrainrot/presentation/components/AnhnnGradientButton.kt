package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.ThemeMode
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnGradients
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnTheme

/** Nut chinh cua app, dung gradient tim Anhnn. Touch target toi thieu 48dp. */
@Composable
fun AnhnnGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(AnhnnGradients.primaryHorizontal())
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
    }
}

@Preview(name = "Light")
@Composable
private fun AnhnnGradientButtonLightPreview() {
    AnhnnTheme(themeMode = ThemeMode.LIGHT) {
        AnhnnGradientButton(text = "Choi ngay", onClick = {})
    }
}

@Preview(name = "Dark")
@Composable
private fun AnhnnGradientButtonDarkPreview() {
    AnhnnTheme(themeMode = ThemeMode.DARK) {
        AnhnnGradientButton(text = "Choi ngay", onClick = {})
    }
}
