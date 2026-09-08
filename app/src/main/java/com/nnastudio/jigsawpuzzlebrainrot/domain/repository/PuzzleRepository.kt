package com.nnastudio.jigsawpuzzlebrainrot.domain.repository

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import kotlinx.coroutines.flow.Flow

/** Catalog anh ghep hinh - toan bo du lieu nam trong app (khong goi API). */
interface PuzzleRepository {
    fun observePuzzles(category: PuzzleCategory? = null): Flow<List<PuzzleImage>>
    suspend fun getPuzzleById(id: String): PuzzleImage?
}
