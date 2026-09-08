package com.nnastudio.jigsawpuzzlebrainrot.presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.EdgeShape
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.EdgeType
import com.nnastudio.jigsawpuzzlebrainrot.domain.models.PieceEdges
import kotlin.math.hypot

/** Nua be rong cua num ghep, tinh theo canh o (tab size 20%). */
private const val TAB_SIZE = 0.10f

/**
 * Num nho ra 2.5 * [TAB_SIZE] so voi canh o, cong them phan meo cua duong cat. Day la ti le
 * dung de chua le xung quanh o cho num khong bi cat mat.
 *
 * Num tren canh ngang nho ra theo chieu cao o, num tren canh doc theo chieu rong o, nen le
 * phai tinh theo canh o lon hon (o vuong thi hai canh bang nhau).
 */
const val TAB_RATIO = 0.30f

/** Ban kinh bo bon goc ngoai cua ca buc anh, tinh theo canh ban co (2mm tren 300mm). */
const val CORNER_RADIUS_RATIO = 2f / 300f

/**
 * Dung duong bao cua mot manh ghep trong he toa do cuc bo cua manh:
 * o chinh nam tai (margin, margin) - (margin + width, margin + height), phan tai loi
 * an ra ngoai toi da [margin] px.
 *
 * Duyet theo chieu kim dong ho (tren -> phai -> duoi -> trai) nen phap tuyen huong ra
 * ngoai luon la (dy, -dx).
 *
 * [cornerRadius] chi duoc dung o goc ma ca hai canh ke deu phang, tuc bon goc ngoai cua
 * ca buc anh; goc giua ban co van vuong de cac manh khit vao nhau.
 */
fun jigsawPiecePath(
    edges: PieceEdges,
    width: Float,
    height: Float,
    margin: Float,
    cornerRadius: Float = 0f
): Path {
    val corners = listOf(
        Offset(margin, margin),
        Offset(margin + width, margin),
        Offset(margin + width, margin + height),
        Offset(margin, margin + height)
    )
    val shapes = listOf(edges.top, edges.right, edges.bottom, edges.left)
    // Canh ke voi mot goc tron luon la canh phang, nen viec cat bot dau canh khong bao gio
    // cham vao doan cong cua num.
    val maxRadius = minOf(width, height) / 2f
    val radii = List(4) { corner ->
        val incoming = shapes[(corner + 3) % 4]
        if (incoming.type == EdgeType.FLAT && shapes[corner].type == EdgeType.FLAT) {
            cornerRadius.coerceAtMost(maxRadius)
        } else {
            0f
        }
    }
    // Chieu dai theo huong vuong goc voi canh: num nho ra tinh theo canh o ben kia.
    val laterals = listOf(height, width, height, width)

    fun startOf(index: Int): Offset {
        val from = corners[index]
        val to = corners[(index + 1) % 4]
        val direction = (to - from) / hypot(to.x - from.x, to.y - from.y)
        return from + direction * radii[index]
    }

    fun endOf(index: Int): Offset {
        val from = corners[index]
        val to = corners[(index + 1) % 4]
        val direction = (to - from) / hypot(to.x - from.x, to.y - from.y)
        return to - direction * radii[(index + 1) % 4]
    }

    return Path().apply {
        val first = startOf(0)
        moveTo(first.x, first.y)
        for (index in 0..3) {
            edge(startOf(index), endOf(index), shapes[index], laterals[index])
            val corner = corners[(index + 1) % 4]
            val next = startOf((index + 1) % 4)
            quadraticTo(corner.x, corner.y, next.x, next.y)
        }
        close()
    }
}

/**
 * Ve mot canh tu [from] den [to]. Canh FLAT la doan thang; TAB/BLANK la ba doan cong noi
 * tiep tao thanh mot num tron co co that (undercut) de hai manh khoa vao nhau.
 *
 * Cac diem dieu khien lay theo cach dung cua trinh sinh manh ghep quen thuoc: doan giua
 * dung hai diem dieu khien o do sau 3 * [TAB_SIZE] keo len thanh num, hai doan hai ben
 * keo nguoc xuong -[TAB_SIZE] tao co. [lateral] la canh o theo huong vuong goc voi canh
 * nay - moi ti le trong cong thuc deu nhan voi no.
 */
private fun Path.edge(from: Offset, to: Offset, edge: EdgeShape, lateral: Float) {
    if (edge.type == EdgeType.FLAT) {
        lineTo(to.x, to.y)
        return
    }

    val along = to - from
    val length = hypot(along.x, along.y)
    // Phap tuyen huong ra ngoai manh khi duyet theo chieu kim dong ho.
    val normal = Offset(along.y / length, -along.x / length)
    val outward = if (edge.type == EdgeType.TAB) 1f else -1f

    /** t = ti le doc canh (0..1), n = do nho ra, tinh theo canh o vuong goc. */
    fun point(t: Float, n: Float) = Offset(
        x = from.x + along.x * t + normal.x * n * lateral * outward,
        y = from.y + along.y * t + normal.y * n * lateral * outward
    )

    val s = TAB_SIZE
    val (_, a, b, c, d, e) = edge

    cubicTo(point(0.2f, a), point(0.5f + b + d, -s + c), point(0.5f - s + b, s + c))
    cubicTo(
        point(0.5f - 2f * s + b - d, 3f * s + c),
        point(0.5f + 2f * s + b - d, 3f * s + c),
        point(0.5f + s + b, s + c)
    )
    cubicTo(point(0.5f + b + d, -s + c), point(0.8f, e), point(1f, 0f))
}

private fun Path.cubicTo(c1: Offset, c2: Offset, to: Offset) =
    cubicTo(c1.x, c1.y, c2.x, c2.y, to.x, to.y)
