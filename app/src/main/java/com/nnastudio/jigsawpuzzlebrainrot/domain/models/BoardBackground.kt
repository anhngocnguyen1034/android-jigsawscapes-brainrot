package com.nnastudio.jigsawpuzzlebrainrot.domain.models

/**
 * Nen ban choi (phan phia sau khung ghep va cac manh roi). Nguoi choi doi duoc ngay trong
 * van tu thanh cong cu; lua chon duoc luu trong [AppSettings] nen van sau van giu.
 *
 * Chi luu [value] chu khong luu mau: mau la viec cua theme (sang / toi), o day chi la ten
 * cua lua chon.
 */
enum class BoardBackground(val value: String) {
    /** Theo theme sang / toi cua app. */
    DEFAULT("default"),
    CREAM("cream"),
    WOOD("wood"),
    GREY("grey"),
    NIGHT("night"),
    PURPLE("purple");

    companion object {
        fun fromValue(value: String?): BoardBackground =
            entries.firstOrNull { it.value == value } ?: DEFAULT
    }
}
