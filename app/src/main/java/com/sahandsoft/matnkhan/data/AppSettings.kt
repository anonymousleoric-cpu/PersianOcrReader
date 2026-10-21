package com.sahandsoft.matnkhan.data

/** Theme preference of the user. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

/** OCR language selection. Tess codes match Tesseract traineddata names. */
enum class OcrLanguage(val tessCode: String, val displayName: String) {
    PERSIAN("fas", "فارسی"),
    ENGLISH("eng", "انگلیسی"),
    PERSIAN_AND_ENGLISH("fas+eng", "فارسی و انگلیسی"),
}

/** Font size of the editable result field. */
enum class ResultTextSize(val value: Float, val displayName: String) {
    SMALL(14f, "کوچک"),
    MEDIUM(16f, "متوسط"),
    LARGE(19f, "بزرگ"),
    EXTRA_LARGE(22f, "بزرگ‌تر"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val ocrLanguage: OcrLanguage = OcrLanguage.PERSIAN_AND_ENGLISH,
    val resultTextSize: ResultTextSize = ResultTextSize.MEDIUM,
)
