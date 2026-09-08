package com.nnastudio.jigsawpuzzlebrainrot.domain.usecases

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleCategory
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.PuzzleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPuzzleListUseCase @Inject constructor(
    private val puzzleRepository: PuzzleRepository
) {
    operator fun invoke(category: PuzzleCategory? = null): Flow<List<PuzzleImage>> =
        puzzleRepository.observePuzzles(category)
}
