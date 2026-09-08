package com.nnastudio.jigsawpuzzlebrainrot.domain.models

import androidx.compose.runtime.Immutable

/** Kieu canh cua mot manh ghep: phang (vien ngoai), loi ra, hoac lom vao. */
enum class EdgeType {
    FLAT,
    TAB,
    BLANK;

    /** Canh cua manh ke ben phai nguoc lai thi hai manh moi khop nhau. */
    fun opposite(): EdgeType = when (this) {
        FLAT -> FLAT
        TAB -> BLANK
        BLANK -> TAB
    }
}

/**
 * Hinh dang mot canh cat. Ngoai loi/lom, moi canh con mang nam so ngau nhien lam duong cat
 * meo di mot chut nhu manh ghep that (khong manh nao giong manh nao):
 *
 * - [a], [e]: do nghieng cua duong cat khi roi/vao hai dau canh - hai canh ke nhau tren cung
 *   mot duong cat dung chung so nay (dau nguoc nhau) nen cho noi khong bi gay goc;
 * - [b]: xe ca cai num sang trai/phai doc canh;
 * - [c]: num nho ra nong/sau hon;
 * - [d]: keo co num rong/hep.
 *
 * Tat ca deu la ti le so voi canh o (a, c, e theo chieu ngang canh; b, d theo chieu doc canh).
 *
 * Hai manh ke nhau dung chung mot canh nhung duyet nguoc chieu nhau, nen manh ben kia giu
 * ban [reversed] - cung mot duong cong, doc tu dau kia.
 */
@Immutable
data class EdgeShape(
    val type: EdgeType,
    val a: Float = 0f,
    val b: Float = 0f,
    val c: Float = 0f,
    val d: Float = 0f,
    val e: Float = 0f
) {
    /** Cung duong cong nhung duyet nguoc chieu: t -> 1 - t, doi ben loi/lom. */
    fun reversed(): EdgeShape = EdgeShape(
        type = type.opposite(),
        a = e,
        b = -b,
        c = c,
        d = -d,
        e = a
    )

    companion object {
        val Flat = EdgeShape(EdgeType.FLAT)
    }
}

@Immutable
data class PieceEdges(
    val top: EdgeShape,
    val right: EdgeShape,
    val bottom: EdgeShape,
    val left: EdgeShape
)

/**
 * Mot manh ghep trong luoi. [row]/[col] la vi tri dung cua manh tren anh goc,
 * [edges] mo ta hinh dang 4 canh de ve duong cat.
 */
@Immutable
data class JigsawPiece(
    val id: Int,
    val row: Int,
    val col: Int,
    val edges: PieceEdges
)
