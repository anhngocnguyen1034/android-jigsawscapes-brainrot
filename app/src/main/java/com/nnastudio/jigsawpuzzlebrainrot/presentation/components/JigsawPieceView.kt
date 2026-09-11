package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.JigsawPiece
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Ve mot manh ghep: clip anh goc theo duong bao jigsaw roi ve vien. Chi ve, khong bat cu
 * chi - nguoi goi tu gan gesture (ban co keo tu do, khay vua keo vua cuon ngang).
 */
@Composable
fun JigsawPieceView(
    piece: JigsawPiece,
    image: ImageBitmap,
    rows: Int,
    cols: Int,
    boardSizePx: Float,
    slotWidth: Dp,
    slotHeight: Dp,
    margin: Dp,
    isPlaced: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val marginPx = with(density) { margin.toPx() }
    val cellWidth = boardSizePx / cols
    val cellHeight = boardSizePx / rows

    val bevelPx = with(density) {
        (minOf(cellWidth, cellHeight) * BEVEL_RATIO).coerceIn(BEVEL_MIN.toPx(), BEVEL_MAX.toPx())
    }
    val cutPx = with(density) { CUT_WIDTH.toPx() }
    val shadowPx = with(density) { SHADOW_OFFSET.toPx() }

    val piecePath = remember(piece.edges, cellWidth, cellHeight, marginPx) {
        jigsawPiecePath(
            edges = piece.edges,
            width = cellWidth,
            height = cellHeight,
            margin = marginPx,
            cornerRadius = boardSizePx * CORNER_RADIUS_RATIO
        )
    }
    val imageSlice = remember(image, piece.row, piece.col, cellWidth, cellHeight, marginPx) {
        imageSliceFor(
            image = image,
            piece = piece,
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            marginPx = marginPx,
            boardSizePx = boardSizePx
        )
    }

    Canvas(
        modifier = modifier.requiredSize(slotWidth + margin * 2, slotHeight + margin * 2)
    ) {
        drawJigsawPiece(
            image = image,
            piecePath = piecePath,
            imageSlice = imageSlice,
            isPlaced = isPlaced,
            bevelPx = bevelPx,
            cutPx = cutPx,
            shadowPx = shadowPx
        )
    }
}

/**
 * Ve mot manh vao goc (0, 0) cua he toa do hien tai.
 * Đã sửa lỗi nguồn sáng và nâng cấp bóng đổ (Drop Shadow) mượt mà.
 */
internal fun DrawScope.drawJigsawPiece(
    image: ImageBitmap,
    piecePath: Path,
    imageSlice: ImageSlice,
    isPlaced: Boolean,
    bevelPx: Float,
    cutPx: Float,
    shadowPx: Float,
    bevelLayers: Int = BEVEL_LAYERS
) {
    // Manh chua vao o van con nam tren ban co nen do bong.
    // Nâng cấp: Dùng BlurMaskFilter thay vì xếp lớp để bóng mềm mại và chân thực hơn.
    if (!isPlaced) {
        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.argb((SHADOW_ALPHA * 255).toInt(), 0, 0, 0)
                maskFilter = android.graphics.BlurMaskFilter(
                    shadowPx * 1.5f,
                    android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.translate(shadowPx, shadowPx) // Lệch chéo bóng xuống dưới-phải
            canvas.nativeCanvas.drawPath(piecePath.asAndroidPath(), paint)
            canvas.nativeCanvas.restore()
        }
    }

    // Manh da vao o thi vien nhat lai, làm bức tranh liền mạch.
    val strength = if (isPlaced) PLACED_DEPTH else 1f
    val layers = bevelLayers.coerceAtLeast(1)

    clipPath(piecePath) {
        translate(left = imageSlice.dstLeft, top = imageSlice.dstTop) {
            drawImage(
                image = image,
                srcOffset = imageSlice.srcOffset,
                srcSize = imageSlice.srcSize,
                dstSize = imageSlice.dstSize
            )
        }

        // Goc vat: Nguồn sáng chiếu từ TRÊN-TRÁI.
        repeat(layers) { layer ->
            val shift = bevelPx * (layer + 1) / layers

            // 1. HIGHLIGHT (Sáng): Dịch XUỐNG DƯỚI, SANG PHẢI để lọt viền Trên-Trái vào trong clip
            translate(left = shift, top = shift) {
                drawPath(
                    path = piecePath,
                    color = Color.White.copy(alpha = HIGHLIGHT_ALPHA * strength / layers),
                    style = Stroke(width = bevelPx * BEVEL_WIDTH_RATIO)
                )
            }

            // 2. SHADOW (Tối): Dịch LÊN TRÊN, SANG TRÁI để lọt viền Dưới-Phải vào trong clip
            translate(left = -shift, top = -shift) {
                drawPath(
                    path = piecePath,
                    color = Color.Black.copy(alpha = EDGE_SHADOW_ALPHA * strength / layers),
                    style = Stroke(width = bevelPx * BEVEL_WIDTH_RATIO)
                )
            }
        }

        // Khe toi sat ria: mảnh ghép thật bao giờ cũng có khe giữa hai mảnh.
        drawPath(
            path = piecePath,
            color = Color.Black.copy(alpha = CUT_ALPHA * strength),
            style = Stroke(width = cutPx * 2f)
        )
    }
}

private const val BEVEL_LAYERS = 2
internal const val BEVEL_LAYERS_BAKED = 5

// Đã loại bỏ RIM_ALPHA vì nó làm bẩn màu của viền bắt sáng (Highlight).
private const val HIGHLIGHT_ALPHA = 0.9f
private const val EDGE_SHADOW_ALPHA = 0.5f
private const val BEVEL_WIDTH_RATIO = 2f

/** Khe toi sat giua hai manh. */
private const val CUT_ALPHA = 0.42f

/**
 * Vung anh goc ung voi khung cua mot manh, kem cho dat trong khung.
 */
internal data class ImageSlice(
    val srcOffset: IntOffset,
    val srcSize: IntSize,
    val dstSize: IntSize,
    val dstLeft: Float,
    val dstTop: Float
)

internal fun imageSliceFor(
    image: ImageBitmap,
    piece: JigsawPiece,
    cellWidth: Float,
    cellHeight: Float,
    marginPx: Float,
    boardSizePx: Float
): ImageSlice {
    if (boardSizePx <= 0f) {
        return ImageSlice(IntOffset.Zero, IntSize.Zero, IntSize.Zero, 0f, 0f)
    }

    val scaleX = image.width / boardSizePx
    val scaleY = image.height / boardSizePx
    val left = piece.col * cellWidth - marginPx
    val top = piece.row * cellHeight - marginPx
    val width = cellWidth + marginPx * 2
    val height = cellHeight + marginPx * 2

    val srcLeft = floor(left * scaleX).toInt().coerceIn(0, image.width) - 1
    val srcTop = floor(top * scaleY).toInt().coerceIn(0, image.height) - 1
    val srcRight = ceil((left + width) * scaleX).toInt().coerceIn(0, image.width) + 1
    val srcBottom = ceil((top + height) * scaleY).toInt().coerceIn(0, image.height) + 1
    val clampedLeft = srcLeft.coerceIn(0, image.width)
    val clampedTop = srcTop.coerceIn(0, image.height)
    val clampedRight = srcRight.coerceIn(clampedLeft, image.width)
    val clampedBottom = srcBottom.coerceIn(clampedTop, image.height)

    return ImageSlice(
        srcOffset = IntOffset(clampedLeft, clampedTop),
        srcSize = IntSize(clampedRight - clampedLeft, clampedBottom - clampedTop),
        dstSize = IntSize(
            width = ((clampedRight - clampedLeft) / scaleX).roundToInt(),
            height = ((clampedBottom - clampedTop) / scaleY).roundToInt()
        ),
        dstLeft = clampedLeft / scaleX - left,
        dstTop = clampedTop / scaleY - top
    )
}

internal const val BEVEL_RATIO = 0.012f
internal val BEVEL_MIN = 0.5.dp
internal val BEVEL_MAX = 1.2.dp

/** Khe cắt đã được tinh chỉnh thanh mảnh hơn. */
internal val CUT_WIDTH = 0.4.dp

/** Bóng đổ được điều chỉnh cho BlurMaskFilter */
private val SHADOW_OFFSET = 3.dp
private const val SHADOW_ALPHA = 0.35f

/** Độ sâu khi đã đặt vào bàn cờ giảm xuống để bức tranh phẳng, liền mạch hơn */
internal const val PLACED_DEPTH = 0.75f