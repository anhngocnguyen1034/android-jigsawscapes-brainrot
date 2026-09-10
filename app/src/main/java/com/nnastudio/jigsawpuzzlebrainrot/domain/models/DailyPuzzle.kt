package com.nnastudio.jigsawpuzzlebrainrot.domain.models

/**
 * Buc anh cua ngay: chon theo so ngay tinh tu epoch nen suot mot ngay ai cung thay cung
 * mot buc, va sang ngay moi la doi. Danh sach rong thi khong co anh nao.
 */
fun List<PuzzleImage>.puzzleOfDay(epochDay: Long): PuzzleImage? {
    if (isEmpty()) return null
    val index = ((epochDay % size) + size) % size
    return this[index.toInt()]
}
