package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPiece
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Tam anh chung chua san hinh cua moi manh o co khay.
 *
 * Ve mot manh la clip anh theo duong bao jigsaw roi ve them may net vien - dat, va man choi
 * phai ve lai tat ca nhung gi dang hien moi khung hinh. Mo hop manh la luc dat nhat: ban co
 * vai chuc manh van dang ve, gio them ca luoi manh trong hop truot len, nen nhip mo hop
 * khung thay ro du khong recompose gi.
 *
 * Nen hinh tung manh o co khay duoc nuong san mot lan vao day; luc ve chi con dan mot o
 * cua tam anh nay len man hinh. Tat ca nam chung mot tam (khong phai moi manh mot anh) de
 * ca luoi manh ve ra bang mot texture duy nhat.
 */
@Immutable
class TrayPieceAtlas(
    val image: ImageBitmap,
    /** Kich thuoc mot o trong tam anh, dung bang kich thuoc ve mot manh o khay. */
    val pieceSize: IntSize,
    private val slots: Map<Int, IntOffset>
) {
    /** Goc cua manh [pieceId] trong tam anh; null la manh khong co trong tam anh nay. */
    fun offsetOf(pieceId: Int): IntOffset? = slots[pieceId]
}

/**
 * Nuong hinh cua [pieces] o co khay thanh mot tam anh chung.
 *
 * Ham nay khong phai composable va khong dung gi cua main thread: nguoi goi phai chay o
 * luong nen. Tra ve null khi chua do xong khay hay khi tam anh se to qua muc GPU nhan
 * duoc - luc do khay cu ve tung manh nhu truoc.
 */
suspend fun renderTrayPieceAtlas(
    pieces: List<JigsawPiece>,
    image: ImageBitmap,
    /** Canh cua "ban co ao" trong khay: manh trong khay do theo day. */
    boardSizePx: Float,
    rows: Int,
    cols: Int,
    density: Density
): TrayPieceAtlas? {
    if (pieces.isEmpty() || boardSizePx <= 0f) return null

    val cellWidth = boardSizePx / cols
    val cellHeight = boardSizePx / rows
    val marginPx = maxOf(cellWidth, cellHeight) * TAB_RATIO
    val pieceWidth = ceil(cellWidth + marginPx * 2).toInt().coerceAtLeast(1)
    val pieceHeight = ceil(cellHeight + marginPx * 2).toInt().coerceAtLeast(1)

    // Xep o vuong vuc cho tam anh khong dai ra mot chieu.
    val columns = ceil(sqrt(pieces.size.toFloat())).toInt().coerceAtLeast(1)
    val atlasRows = ceil(pieces.size.toFloat() / columns).toInt()
    val atlasWidth = pieceWidth * columns
    val atlasHeight = pieceHeight * atlasRows
    if (atlasWidth > MAX_ATLAS_SIDE || atlasHeight > MAX_ATLAS_SIDE) return null

    val bevelPx = with(density) {
        (minOf(cellWidth, cellHeight) * BEVEL_RATIO)
            .coerceIn(BEVEL_MIN.toPx(), BEVEL_MAX.toPx())
    }
    val cutPx = with(density) { CUT_WIDTH.toPx() }

    val target = ImageBitmap(atlasWidth, atlasHeight)
    val canvas = Canvas(target)
    val drawScope = CanvasDrawScope()
    val canvasSize = Size(atlasWidth.toFloat(), atlasHeight.toFloat())
    val slots = HashMap<Int, IntOffset>(pieces.size)

    // Nuong tung cum nhu ban co da ghep xong: doi kich thuoc khay giua chung thi lan nuong
    // cu dung han thay vi ve not ca 400 manh roi mang di vut.
    pieces.withIndex().chunked(BAKE_CHUNK).forEach { chunk ->
        coroutineContext.ensureActive()
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = canvasSize
        ) {
            chunk.forEach { (index, piece) ->
                val left = (index % columns) * pieceWidth
                val top = (index / columns) * pieceHeight
                slots[piece.id] = IntOffset(left, top)
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
                translate(left = left.toFloat(), top = top.toFloat()) {
                    drawJigsawPiece(
                        image = image,
                        piecePath = path,
                        imageSlice = slice,
                        isPlaced = false,
                        bevelPx = bevelPx,
                        cutPx = cutPx,
                        // Ve mot lan roi de danh nen goc vat duoc lam muot han ban co.
                        bevelLayers = BEVEL_LAYERS_BAKED
                    )
                }
            }
        }
    }

    return TrayPieceAtlas(
        image = target,
        pieceSize = IntSize(pieceWidth, pieceHeight),
        slots = slots
    )
}

/** So manh moi lan mo mot lan ve - du nho de huy nhanh, du to de khong ton cong mo lai. */
private const val BAKE_CHUNK = 32

/** Tam anh to hon muc nay thi thoi khong nuong: GPU pho thong chi nhan den co nay. */
private const val MAX_ATLAS_SIDE = 4096
