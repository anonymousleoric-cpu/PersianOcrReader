package com.sahandsoft.matnkhan.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "matnkhan_settings")

/** Persists user preferences with Jetpack DataStore (Preferences). */
class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val languageKey = stringPreferencesKey("ocr_language")
    private val textSizeKey = stringPreferencesKey("result_text_size")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[themeKey]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            ocrLanguage = prefs[languageKey]
                ?.let { runCatching { OcrLanguage.valueOf(it) }.getOrNull() }
                ?: OcrLanguage.PERSIAN_AND_ENGLISH,
            resultTextSize = prefs[textSizeKey]
                ?.let { runCatching { ResultTextSize.valueOf(it) }.getOrNull() }
                ?: ResultTextSize.MEDIUM,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setOcrLanguage(language: OcrLanguage) {
        context.dataStore.edit { it[languageKey] = language.name }
    }

    suspend fun setResultTextSize(size: ResultTextSize) {
        context.dataStore.edit { it[textSizeKey] = size.name }
    }
}
