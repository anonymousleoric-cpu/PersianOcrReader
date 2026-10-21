package com.sahandsoft.matnkhan.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Thrown/returned when the OCR engine could not find any text in the image. */
class OcrEmptyException : Exception("NO_TEXT_DETECTED")

/** Current readiness state of the offline OCR engine. */
sealed interface OcrStatus {
    data object Idle : OcrStatus
    data object Preparing : OcrStatus
    data class Ready(val language: String) : OcrStatus
    data class Error(val message: String) : OcrStatus
}

/** Result of a successful recognition pass. */
data class OcrResult(val text: String, val meanConfidence: Int)

/**
 * Single access point to the Tesseract OCR engine.
 *
 * - Copies bundled trained models (assets/tessdata) into internal storage on first use.
 * - Lazily initializes TessBaseAPI and re-initializes it only when the language changes.
 * - All calls are serialized with a mutex because TessBaseAPI is not thread-safe.
 * - Runs completely offline; no network access is requested anywhere in the app.
 */
object OcrManager {

    private const val TAG = "OcrManager"
    private const val TESS_PARENT_DIR = "tesseract"
    private const val TESSDATA_ASSET_DIR = "tessdata"
    private const val DEFAULT_LANGUAGE = "fas+eng"

    private val _status = MutableStateFlow<OcrStatus>(OcrStatus.Idle)
    val status: StateFlow<OcrStatus> = _status.asStateFlow()

    private val mutex = Mutex()
    private var tess: TessBaseAPI? = null
    private var activeLanguage: String? = null

    private fun tessDataRoot(context: Context): File =
        File(context.filesDir, TESS_PARENT_DIR)

    /** Copies bundled traineddata files into filesDir/tesseract/tessdata when missing. */
    private fun copyModelsIfNeeded(context: Context): Boolean {
        val tessDataDir = File(tessDataRoot(context), "tessdata")
        if (!tessDataDir.exists() && !tessDataDir.mkdirs()) return false
        val names = context.assets.list(TESSDATA_ASSET_DIR) ?: return false
        if (names.isEmpty()) return false
        for (name in names) {
            val target = File(tessDataDir, name)
            if (target.exists() && target.length() > 0L) continue
            try {
                context.assets.open("$TESSDATA_ASSET_DIR/$name").use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to copy model $name", t)
                return false
            }
        }
        return true
    }

    private fun obtainEngine(context: Context, language: String): TessBaseAPI {
        val api = tess ?: TessBaseAPI().also { tess = it }
        if (activeLanguage == language) return api
        val rootPath = tessDataRoot(context).absolutePath
        val ok = api.init(rootPath, language)
        if (!ok) {
            throw IllegalStateException(
                "بارگذاری مدل OCR برای زبان «$language» ناموفق بود."
            )
        }
        api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
        api.setVariable("preserve_interword_spaces", "1")
        activeLanguage = language
        return api
    }

    /**
     * Makes sure models are copied and the engine is initialized.
     * Safe to call repeatedly; cheap when everything is already ready.
     */
    suspend fun ensureReady(
        context: Context,
        language: String = DEFAULT_LANGUAGE,
    ): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                _status.value = OcrStatus.Preparing
                if (!copyModelsIfNeeded(context)) {
                    _status.value = OcrStatus.Error(
                        "فایل‌های مدل OCR در حافظه برنامه کپی نشدند."
                    )
                    return@withLock false
                }
                obtainEngine(context, language)
                _status.value = OcrStatus.Ready(language)
                true
            } catch (t: Throwable) {
                Log.e(TAG, "ensureReady failed", t)
                _status.value = OcrStatus.Error(
                    t.message ?: "آماده‌سازی موتور OCR ناموفق بود."
                )
                false
            }
        }
    }

    /** Runs OCR on [bitmap] using Tesseract with the requested language codes (e.g. "fas+eng"). */
    suspend fun recognize(
        context: Context,
        bitmap: Bitmap,
        language: String,
    ): Result<OcrResult> = withContext(Dispatchers.Default) {
        mutex.withLock {
            var api: TessBaseAPI? = null
            try {
                _status.value = OcrStatus.Preparing
                if (!copyModelsIfNeeded(context)) {
                    throw IllegalStateException("مدل‌های OCR در دسترس نیستند.")
                }
                val engine = obtainEngine(context, language)
                api = engine
                engine.setImage(bitmap)
                val text = engine.getUTF8Text()?.trim().orEmpty()
                val confidence = engine.meanConfidence()
                engine.clear()
                _status.value = OcrStatus.Ready(language)
                if (text.isEmpty()) {
                    Result.failure(OcrEmptyException())
                } else {
                    Result.success(OcrResult(text, confidence))
                }
            } catch (t: Throwable) {
                Log.e(TAG, "recognize failed", t)
                runCatching { api?.clear() }
                _status.value = OcrStatus.Error(
                    t.message ?: "خطای ناشناخته در پردازش OCR"
                )
                Result.failure(t)
            }
        }
    }
}
