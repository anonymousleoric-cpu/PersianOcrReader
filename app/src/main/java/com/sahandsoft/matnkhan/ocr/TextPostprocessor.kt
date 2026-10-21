package com.sahandsoft.matnkhan.ocr

/**
 * Light, safe post-processing of raw Tesseract output.
 *
 * Rules (deliberately conservative — never reorders characters, so Unicode
 * bidi rendering of mixed Persian/English text stays intact):
 *  - normalize line endings
 *  - map Arabic Yeh/Kaf to their Persian equivalents (ي → ی ، ك → ک)
 *  - remove tatweel (ـ) stretch characters
 *  - convert Arabic-Indic digits (٠..٩) to Persian digits (۰..۹);
 *    Persian and Latin digits are left untouched
 *  - trim each line and collapse repeated spaces/tabs
 *  - collapse 3+ consecutive newlines into a single blank line
 */
object TextPostprocessor {

    private val multiSpace = Regex("[\\t\\u0020\\u00A0\\u2007\\u202F]+")
    private val manyNewlines = Regex("\n{3,}")

    fun clean(raw: String): String {
        if (raw.isBlank()) return ""
        val builder = StringBuilder(raw.length)
        for (ch in raw.replace("\r\n", "\n").replace('\r', '\n')) {
            when {
                ch == '\u0640' -> {} // tatweel: drop
                ch == '\u064A' -> builder.append('\u06CC') // ي -> ی
                ch == '\u0643' -> builder.append('\u06A9') // ك -> ک
                ch in '\u0660'..'\u0669' ->
                    builder.append(('۰'.code + (ch - '\u0660')).toChar())
                else -> builder.append(ch)
            }
        }
        return builder.toString()
            .split('\n')
            .joinToString("\n") { line -> line.trim().replace(multiSpace, " ") }
            .replace(manyNewlines, "\n\n")
            .trim()
    }
}
