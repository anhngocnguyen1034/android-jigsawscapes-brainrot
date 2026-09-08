package com.nnastudio.jigsawpuzzlebrainrot.domain.usecases

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleProgress
import com.nnastudio.jigsawpuzzlebrainrot.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveProgressUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    operator fun invoke(): Flow<List<PuzzleProgress>> = progressRepository.observeAllProgress()
}
