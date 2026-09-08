package com.nnastudio.jigsawpuzzlebrainrot.data.repository

import com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local.PuzzleCatalogDataSource
import com.nnastudio.jigsawpuzzlebrainrot.data.models.toDomain
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.PuzzleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PuzzleRepositoryImpl @Inject constructor(
    private val catalogDataSource: PuzzleCatalogDataSource
) : PuzzleRepository {

    override fun observePuzzles(category: PuzzleCategory?): Flow<List<PuzzleImage>> = flow {
        val puzzles = catalogDataSource.loadCatalog().map { it.toDomain() }
        emit(if (category == null) puzzles else puzzles.filter { it.category == category })
    }.catch { emit(emptyList()) }

    override suspend fun getPuzzleById(id: String): PuzzleImage? =
        runCatching { catalogDataSource.loadCatalog().firstOrNull { it.id == id }?.toDomain() }
            .getOrNull()
}
