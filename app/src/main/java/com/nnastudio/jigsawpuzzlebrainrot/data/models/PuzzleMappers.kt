package com.nnastudio.jigsawpuzzlebrainrot.data.models

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage

fun PuzzleImageDto.toDomain(): PuzzleImage = PuzzleImage(
    id = id,
    title = title,
    category = PuzzleCategory.entries.firstOrNull { it.id == category } ?: PuzzleCategory.BRAINROT,
    assetPath = assetPath,
    isPremium = premium
)
