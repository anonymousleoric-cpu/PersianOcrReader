package com.sahandsoft.matnkhan.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sahandsoft.matnkhan.data.AppSettings
import com.sahandsoft.matnkhan.data.OcrLanguage
import com.sahandsoft.matnkhan.data.ResultTextSize
import com.sahandsoft.matnkhan.data.SettingsRepository
import com.sahandsoft.matnkhan.data.ThemeMode
import com.sahandsoft.matnkhan.ocr.OcrEmptyException
import com.sahandsoft.matnkhan.ocr.OcrManager
import com.sahandsoft.matnkhan.ocr.TextPostprocessor
import com.sahandsoft.matnkhan.util.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** One-shot message shown as a snackbar. */
data class UiMessage(val id: Long, val text: String, val isError: Boolean)

data class MainUiState(
    val imageFile: File? = null,
    val isProcessing: Boolean = false,
    val resultText: String = "",
    val meanConfidence: Int? = null,
    val message: UiMessage? = null,
)

/**
 * MVVM ViewModel: holds UI state, drives OCR runs off the main thread and
 * exposes the user's persisted settings.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun onImageSelected(file: File) {
        _uiState.update {
            it.copy(imageFile = file, resultText = "", meanConfidence = null, message = null)
        }
    }

    fun showMessage(text: String, isError: Boolean = false) {
        _uiState.update {
            it.copy(message = UiMessage(System.nanoTime(), text, isError))
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    /** Runs OCR in the background; re-entrant calls while processing are ignored. */
    fun extractText() {
        val current = _uiState.value
        val image = current.imageFile
        if (image == null) {
            showMessage("ابتدا یک تصویر انتخاب کنید.", isError = true)
            return
        }
        if (current.isProcessing) return // guard against double taps

        val language = settings.value.ocrLanguage.tessCode
        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch {
            val result = try {
                val bitmap = ImageUtils.loadForOcr(getApplication(), image)
                OcrManager.recognize(getApplication(), bitmap, language)
            } catch (oom: OutOfMemoryError) {
                Result.failure(
                    IllegalStateException(
                        "تصویر بسیار بزرگ است و حافظه کافی برای پردازش وجود ندارد."
                    )
                )
            } catch (t: Throwable) {
                Result.failure(t)
            }

            result.fold(
                onSuccess = { ocrResult ->
                    val cleaned = TextPostprocessor.clean(ocrResult.text)
                    if (cleaned.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                message = UiMessage(
                                    System.nanoTime(),
                                    "متنی در تصویر شناسایی نشد. از تصویری واضح‌تر، پرنورتر و نزدیک‌تر به متن استفاده کنید.",
                                    true
                                )
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                resultText = cleaned,
                                meanConfidence = ocrResult.meanConfidence
                            )
                        }
                        if (ocrResult.meanConfidence < 40) {
                            showMessage(
                                "کیفیت شناسایی پایین بود؛ از تصویر باکیفیت‌تر و پرنورتر استفاده کنید."
                            )
                        }
                    }
                },
                onFailure = { error ->
                    val message = when (error) {
                        is OcrEmptyException ->
                            "متنی در تصویر شناسایی نشد. تصویر واضح‌تری انتخاب کنید."
                        else -> error.message ?: "خطای ناشناخته در پردازش تصویر."
                    }
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            message = UiMessage(System.nanoTime(), message, true)
                        )
                    }
                }
            )
        }
    }

    fun onTextChanged(text: String) = _uiState.update { it.copy(resultText = text) }

    fun clearText() = _uiState.update { it.copy(resultText = "", meanConfidence = null) }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setOcrLanguage(language: OcrLanguage) {
        viewModelScope.launch { repository.setOcrLanguage(language) }
    }

    fun setResultTextSize(size: ResultTextSize) {
        viewModelScope.launch { repository.setResultTextSize(size) }
    }
}
