package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
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
 *
 * [boardSizePx] la canh ban co tinh bang px - anh duoc ve o dung ti le do roi dich chuyen
 * sao cho dung phan anh thuoc ve manh nay nam trong duong bao. Khay dung mot "ban co ao"
 * nho hon nen cung composable ve duoc manh o moi ti le.
 *
 * Hai thu duoc lam san ngoai pha ve vi ban co co toi 64 manh: duong bao (nhieu doan cong,
 * dung lai moi frame thi ton CPU) va vung anh can lay (chi vai phan tram buc anh, ve ca
 * buc roi clip thi moi manh phai lay mau lai toan bo anh).
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
    // Be day dai vat thay duoc, tinh theo canh o - manh nho trong khay van phai thay
    // duoc goc vat, manh to thi vat khong duoc day qua che mat anh.
    val bevelPx = with(density) {
        (minOf(cellWidth, cellHeight) * BEVEL_RATIO).coerceIn(BEVEL_MIN.toPx(), BEVEL_MAX.toPx())
    }
    val cutPx = with(density) { CUT_WIDTH.toPx() }
    val shadowPx = with(density) { SHADOW_OFFSET.toPx() }
    val frameWidth = with(density) { (slotWidth + margin * 2).toPx() }
    val frameHeight = with(density) { (slotHeight + margin * 2).toPx() }
    val depth = remember(frameWidth, frameHeight, isPlaced) {
        depthBrush(frameWidth, frameHeight, if (isPlaced) PLACED_DEPTH else 1f)
    }

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

    // requiredSize chu khong phai size: ban co zoom to hon vung choi thi rang buoc tu cha
    // se cat bot khung manh, manh se bi bop lai thay vi to len.
    Canvas(
        modifier = modifier.requiredSize(slotWidth + margin * 2, slotHeight + margin * 2)
    ) {
        // Manh chua vao o van con nam tren ban co nen do bong xuong duoi-phai cho no noi
        // len. Bong la vai ban sao cua chinh duong bao lech dan roi to nhat: canvas tang
        // toc GPU khong ve duoc bong nhoe san cho duong bao lom nhu manh ghep.
        if (!isPlaced) {
            repeat(SHADOW_LAYERS) { layer ->
                val shift = shadowPx * (layer + 1) / SHADOW_LAYERS
                translate(left = shift, top = shift) {
                    drawPath(piecePath, Color.Black.copy(alpha = SHADOW_ALPHA))
                }
            }
        }
        clipPath(piecePath) {
            translate(left = imageSlice.dstLeft, top = imageSlice.dstTop) {
                drawImage(
                    image = image,
                    srcOffset = imageSlice.srcOffset,
                    srcSize = imageSlice.srcSize,
                    dstSize = imageSlice.dstSize
                )
            }
            // Net ve o day deu bi clip cat mat nua ngoai, chi con dai nam trong long manh:
            // nho vay manh khong lan ra ngoai duong bao khi de len manh khac.
            //
            // Goc vat: dai sang chay doc ria tren-trai, dai toi doc ria duoi-phai - keo theo
            // ca duong num ghep nen num loi ra, hoc lom vao dung nhu manh that.
            drawPath(path = piecePath, brush = depth, style = Stroke(width = bevelPx * 2f))
            // Net cat sat ria: manh ghep that bao gio cung co khe toi giua hai manh.
            drawPath(
                path = piecePath,
                color = Color.Black.copy(alpha = if (isPlaced) 0.24f else 0.32f),
                style = Stroke(width = cutPx * 2f)
            )
        }
    }
}

/**
 * Vien vat cua manh: anh sang den tu tren-trai nen ria tren-trai bat sang, ria duoi-phai
 * chim vao toi. Chuyen dan giua hai ben de mat vat cong deu chu khong thanh hai vach.
 *
 * [strength] ha do dam cho manh da vao o - buc anh la chinh, khong phai cai luoi cat.
 */
private fun depthBrush(width: Float, height: Float, strength: Float) = Brush.linearGradient(
    colorStops = arrayOf(
        0f to Color.White.copy(alpha = 0.62f * strength),
        0.42f to Color.White.copy(alpha = 0.12f * strength),
        0.58f to Color.Black.copy(alpha = 0.12f * strength),
        1f to Color.Black.copy(alpha = 0.5f * strength)
    ),
    start = Offset.Zero,
    end = Offset(width, height)
)

/**
 * Vung anh goc ung voi khung cua mot manh, kem cho dat trong khung.
 *
 * [dstLeft]/[dstTop] la phan le duoi mot pixel anh goc: lam tron o day se lech noi dung so
 * voi duong bao, nen phan le duoc giu lai bang translate.
 */
private data class ImageSlice(
    val srcOffset: IntOffset,
    val srcSize: IntSize,
    val dstSize: IntSize,
    val dstLeft: Float,
    val dstTop: Float
)

private fun imageSliceFor(
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
    // Ti le anh goc / ban co: 1 px ban co = scaleX px anh.
    val scaleX = image.width / boardSizePx
    val scaleY = image.height / boardSizePx
    // Goc (0, 0) cua khung manh nam tai diem nay tren ban co.
    val left = piece.col * cellWidth - marginPx
    val top = piece.row * cellHeight - marginPx
    val width = cellWidth + marginPx * 2
    val height = cellHeight + marginPx * 2

    // Lay du mot pixel anh o moi phia cho vien khung khong bi ho khi lam tron.
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

/** Be day goc vat, tinh theo canh o (bi chan tren/duoi cho moi do kho deu thay duoc). */
private const val BEVEL_RATIO = 0.04f
private val BEVEL_MIN = 1.2.dp
private val BEVEL_MAX = 5.dp

/** Khe toi giua hai manh ke nhau. */
private val CUT_WIDTH = 0.7.dp

/** Bong cua manh chua vao o: lech [SHADOW_OFFSET] xuong duoi-phai, xep tu nhieu lop. */
private val SHADOW_OFFSET = 2.dp
private const val SHADOW_LAYERS = 4
private const val SHADOW_ALPHA = 0.055f

/** Manh da vao o thi vien vat nhat lai. */
private const val PLACED_DEPTH = 0.8f
