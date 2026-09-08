package com.nnastudio.jigsawpuzzlebrainrot.domain.usecases

import com.nnastudio.jigsawpuzzlebrainrot.domain.models.Difficulty
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.EdgeShape
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.EdgeType
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPiece
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPuzzle
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceEdges
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleImage
import javax.inject.Inject
import kotlin.random.Random

/** Do meo cua duong cat so voi canh o (jitter 4%). */
private const val JITTER = 0.04f

/**
 * Cat anh thanh luoi manh ghep: chon ngau nhien loi/lom cho tung duong cat ben trong,
 * canh ngoai cua anh luon phang. Hai manh ke nhau luon co canh nguoc nhau nen chi khop
 * dung mot cach duy nhat.
 *
 * Duong cat khong sinh theo tung canh roi rac ma theo tung duong cat chay het ban co:
 * moi canh nhan lai do nghieng o dau mut cua canh lien truoc, nho vay cho hai canh noi
 * nhau duong cat di muot chu khong gay goc.
 */
class GenerateJigsawPuzzleUseCase @Inject constructor() {

    operator fun invoke(
        image: PuzzleImage,
        difficulty: Difficulty,
        random: Random = Random.Default
    ): JigsawPuzzle {
        val rows = difficulty.rows
        val cols = difficulty.cols

        // Canh duoi cua manh (r, c) - dung chung voi canh tren cua manh (r+1, c).
        val bottomEdges = Array(rows - 1) { random.cutLine(cols) }
        // Canh phai cua manh (r, c) - dung chung voi canh trai cua manh (r, c+1).
        val rightEdges = Array(cols - 1) { random.cutLine(rows) }

        val pieces = buildList {
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    add(
                        JigsawPiece(
                            id = row * cols + col,
                            row = row,
                            col = col,
                            edges = PieceEdges(
                                top = if (row == 0) EdgeShape.Flat else bottomEdges[row - 1][col].reversed(),
                                right = if (col == cols - 1) EdgeShape.Flat else rightEdges[col][row],
                                bottom = if (row == rows - 1) EdgeShape.Flat else bottomEdges[row][col],
                                left = if (col == 0) EdgeShape.Flat else rightEdges[col - 1][row].reversed()
                            )
                        )
                    )
                }
            }
        }

        return JigsawPuzzle(image = image, difficulty = difficulty, pieces = pieces)
    }

    /**
     * Mot duong cat chay het ban co, cat thanh [count] canh lien tiep.
     *
     * Canh moi lay do nghieng dau vao ([a]) tu do nghieng dau ra cua canh truoc: cung ben
     * loi/lom thi doi dau, khac ben thi giu nguyen - hai cach deu cho hai doan cong noi
     * nhau thanh mot duong thang tai diem noi.
     */
    private fun Random.cutLine(count: Int): List<EdgeShape> {
        var tab = nextBoolean()
        var slope = jitter()
        return List(count) {
            val previousTab = tab
            tab = nextBoolean()
            val a = if (tab == previousTab) -slope else slope
            val b = jitter()
            val c = jitter()
            val d = jitter()
            slope = jitter()
            EdgeShape(
                type = if (tab) EdgeType.TAB else EdgeType.BLANK,
                a = a,
                b = b,
                c = c,
                d = d,
                e = slope
            )
        }
    }

    /** Mot so ngau nhien trong khoang +/- [JITTER]. */
    private fun Random.jitter(): Float = nextFloat() * 2f * JITTER - JITTER
}
