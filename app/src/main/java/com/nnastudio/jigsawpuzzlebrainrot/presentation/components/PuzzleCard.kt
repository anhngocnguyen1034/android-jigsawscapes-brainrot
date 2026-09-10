package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.nnastudio.jigsawpuzzlebrainrot.R
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import com.nnastudio.jigsawpuzzlebrainrot.presentation.theme.AnhnnGradients
import com.nnastudio.jigsawpuzzlebrainrot.utils.assetUri

/** The anh trong danh sach. Anh load bang Coil tu assets (khong co CDN/backend). */
@Composable
fun PuzzleCard(
    puzzle: PuzzleImage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** % da ghep cua van dang do dang; null (chua choi lan nao) thi khong hien gi. */
    progressPercent: Int? = null,
    /** Diem da an duoc o buc nay; co diem thi o goc tren-phai hien diem thay cho %. */
    score: Int? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(AnhnnGradients.primaryVertical())
        ) {
            AsyncImage(
                model = puzzle.assetUri(),
                contentDescription = puzzle.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (puzzle.isPremium) {
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        // Goc tren-phai danh cho % tien do.
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AnhnnGradients.primaryHorizontal())
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            val badge = score?.let { stringResource(R.string.card_score, it) }
                ?: progressPercent?.let { "$it%" }
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        // Nen mo de doc duoc so tren moi buc anh, van thay anh ben duoi.
                        .background(Color.Black.copy(alpha = BADGE_ALPHA))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            text = puzzle.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/** Do duc cua o dung % tien do tren anh. */
private const val BADGE_ALPHA = 0.45f

private val previewPuzzle = PuzzleImage(
    id = "tralalero",
    title = "Tralalero Tralala",
    category = PuzzleCategory.BRAINROT,
    assetPath = "puzzles/tralalero.webp"
)
