package com.nnastudio.jigsawpuzzlebrainrot.domain.repository

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleArtwork
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleSource

interface PuzzleImageRepository {
    /**
     * Giai ma anh, thu nho ve toi da [maxDimension]px va cat vuong o giua de vua ban co.
     * Neu khong doc duoc anh thi tra ve anh gradient thay the (app luon choi duoc).
     */
    suspend fun loadArtwork(source: PuzzleSource, maxDimension: Int = 1080): PuzzleArtwork
}
