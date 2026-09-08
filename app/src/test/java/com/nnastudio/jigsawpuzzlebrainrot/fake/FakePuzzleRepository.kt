package com.nnastudio.jigsawpuzzlebrainrot.fake

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.PuzzleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePuzzleRepository(
    initial: List<PuzzleImage> = defaultPuzzles
) : PuzzleRepository {

    private val puzzles = MutableStateFlow(initial)

    override fun observePuzzles(category: PuzzleCategory?): Flow<List<PuzzleImage>> =
        puzzles.map { list -> if (category == null) list else list.filter { it.category == category } }

    override suspend fun getPuzzleById(id: String): PuzzleImage? =
        puzzles.value.firstOrNull { it.id == id }

    companion object {
        val defaultPuzzles = listOf(
            PuzzleImage("tralalero", "Tralalero", PuzzleCategory.BRAINROT, "puzzles/tralalero.webp"),
            PuzzleImage("capybara", "Capybara", PuzzleCategory.ANIMAL, "puzzles/capybara.webp")
        )
    }
}
