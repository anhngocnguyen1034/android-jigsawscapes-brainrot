package com.nnastudio.jigsawpuzzlebrainrot.data.datasource.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PuzzleSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Doc anh tu assets hoac tu may nguoi dung. Anh duoc subsample luc giai ma de khong
 * ngon RAM, sau do cat vuong o giua vi ban co la hinh vuong.
 */
@Singleton
class ArtworkDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun decodeSquare(source: PuzzleSource, maxDimension: Int): Bitmap? =
        withContext(ioDispatcher) {
            runCatching {
                val bounds = openStream(source)?.use { stream ->
                    BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        .also { BitmapFactory.decodeStream(stream, null, it) }
                } ?: return@runCatching null

                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = openStream(source)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return@runCatching null

                cropCenterSquare(decoded, maxDimension)
            }.getOrNull()
        }

    /** Anh thay the khi thieu file trong assets, de van choi thu duoc. */
    fun placeholder(size: Int, seedText: String): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val hue = (seedText.hashCode().toFloat() % 360f + 360f) % 360f
        val start = Color.HSVToColor(floatArrayOf(hue, 0.55f, 0.95f))
        val end = Color.HSVToColor(floatArrayOf((hue + 60f) % 360f, 0.75f, 0.65f))

        canvas.drawPaint(
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    start, end, Shader.TileMode.CLAMP
                )
            }
        )
        // Vai vong tron cho de phan biet cac manh khi thu nghiem.
        val dotPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(70, 255, 255, 255)
        }
        val step = size / 6f
        for (i in 1..5) {
            for (j in 1..5) {
                canvas.drawCircle(i * step, j * step, step * 0.18f, dotPaint)
            }
        }
        return bitmap
    }

    private fun openStream(source: PuzzleSource): InputStream? = when (source) {
        is PuzzleSource.Asset -> context.assets.open(source.path)
    }

    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        var longest = max(width, height)
        while (longest / 2 >= maxDimension) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun cropCenterSquare(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val side = min(bitmap.width, bitmap.height)
        val cropped = Bitmap.createBitmap(
            bitmap,
            (bitmap.width - side) / 2,
            (bitmap.height - side) / 2,
            side,
            side
        )
        if (cropped !== bitmap) bitmap.recycle()

        if (cropped.width <= maxDimension) return cropped
        return Bitmap.createScaledBitmap(cropped, maxDimension, maxDimension, true)
            .also { if (it !== cropped) cropped.recycle() }
    }
}
