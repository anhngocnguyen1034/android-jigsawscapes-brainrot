package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPuzzle
import kotlin.math.roundToInt

/**
 * "Nuong" ca ban co da ghep xong thanh mot buc anh: tung manh van duoc ve y het cach
 * [JigsawPieceView] ve no - clip theo duong bao, goc vat, khe cat - nhung ve mot lan roi
 * de danh, thay vi ve lai moi khung hinh.
 *
 * Ham nay khong phai composable va khong dung thu gi cua main thread, nen nguoi goi phai
 * chay no o luong nen: luoi 400 manh la 400 lan clip, dat qua de lam giua lung chung mot
 * cu vuot tay.
 */
fun renderSolvedBoard(
    puzzle: JigsawPuzzle,
    image: ImageBitmap,
    boardSizePx: Float,
    density: Density
): ImageBitmap {
    val side = boardSizePx.roundToInt().coerceAtLeast(1)
    val rows = puzzle.difficulty.rows
    val cols = puzzle.difficulty.cols
    val cellWidth = boardSizePx / cols
    val cellHeight = boardSizePx / rows
    // Le cho tai manh tho ra ngoai o - dung cach do cua ban co that.
    val marginPx = maxOf(cellWidth, cellHeight) * TAB_RATIO

    val bevelPx = with(density) {
        (minOf(cellWidth, cellHeight) * BEVEL_RATIO)
            .coerceIn(BEVEL_MIN.toPx(), BEVEL_MAX.toPx())
    }
    val cutPx = with(density) { CUT_WIDTH.toPx() }

    val target = ImageBitmap(side, side)
    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(target),
        size = Size(side.toFloat(), side.toFloat())
    ) {
        puzzle.pieces.forEach { piece ->
            val path = jigsawPiecePath(
                edges = piece.edges,
                width = cellWidth,
                height = cellHeight,
                margin = marginPx,
                cornerRadius = boardSizePx * CORNER_RADIUS_RATIO
            )
            val slice = imageSliceFor(
                image = image,
                piece = piece,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                marginPx = marginPx,
                boardSizePx = boardSizePx
            )
            translate(
                left = cellWidth * piece.col - marginPx,
                top = cellHeight * piece.row - marginPx
            ) {
                drawJigsawPiece(
                    image = image,
                    piecePath = path,
                    imageSlice = slice,
                    isPlaced = true,
                    bevelPx = bevelPx,
                    cutPx = cutPx,
                    // Manh da vao o het nen khong manh nao do bong xuong manh nao.
                    shadowPx = 0f,
                    // Ve mot lan roi de danh nen goc vat duoc lam muot han ban co.
                    bevelLayers = BEVEL_LAYERS_BAKED
                )
            }
        }
    }
    return target
}
