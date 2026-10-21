package com.sahandsoft.matnkhan.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahandsoft.matnkhan.BuildConfig
import com.sahandsoft.matnkhan.data.AppSettings
import com.sahandsoft.matnkhan.data.OcrLanguage
import com.sahandsoft.matnkhan.data.ResultTextSize
import com.sahandsoft.matnkhan.data.ThemeMode
import com.sahandsoft.matnkhan.ocr.OcrManager
import com.sahandsoft.matnkhan.ocr.OcrStatus

private fun langLabel(code: String): String =
    code.split("+").joinToString(" + ") { lang ->
        when (lang) {
            "fas" -> "فارسی"
            "eng" -> "انگلیسی"
            else -> lang
        }
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOcrLanguageChange: (OcrLanguage) -> Unit,
    onResultTextSizeChange: (ResultTextSize) -> Unit,
) {
    val ocrStatus by OcrManager.status.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(settings.ocrLanguage) {
        OcrManager.ensureReady(context, settings.ocrLanguage.tessCode)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("تنظیمات", style = MaterialTheme.typography.headlineSmall)

            // ── Theme ─────────────────────────────────────────────────────
            SettingGroup(title = "حالت نمایش") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val modes = listOf(
                        Triple(ThemeMode.SYSTEM, "سیستم", Icons.Filled.BrightnessAuto),
                        Triple(ThemeMode.LIGHT, "روشن", Icons.Filled.LightMode),
                        Triple(ThemeMode.DARK, "تاریک", Icons.Filled.DarkMode),
                    )
                    modes.forEach { (mode, label, icon) ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
                }
            }

            // ── OCR language ─────────────────────────────────────────────
            SettingGroup(title = "زبان شناسایی (OCR)") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OcrLanguage.entries.forEach { language ->
                        FilterChip(
                            selected = settings.ocrLanguage == language,
                            onClick = { onOcrLanguageChange(language) },
                            label = { Text(language.displayName) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
            Text(
                "برای دقت بیشتر، هنگام کار با متون تک‌زبانه فقط همان زبان را انتخاب کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Result text size ─────────────────────────────────────────
            SettingGroup(title = "اندازه متن خروجی") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResultTextSize.entries.forEach { size ->
                        FilterChip(
                            selected = settings.resultTextSize == size,
                            onClick = { onResultTextSizeChange(size) },
                            label = { Text(size.displayName) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.TextFields,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            // ── OCR engine status ────────────────────────────────────────
            SettingGroup(title = "وضعیت موتور OCR") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (val status = ocrStatus) {
                        is OcrStatus.Idle -> {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("بررسی نشده", style = MaterialTheme.typography.bodyMedium)
                        }
                        is OcrStatus.Preparing -> {
                            LinearProgressIndicator(
                                modifier = Modifier.width(72.dp),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Text(
                                "در حال آماده‌سازی مدل‌ها…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is OcrStatus.Ready -> {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "مدل‌ها آماده است (${langLabel(status.language)}) — کاملاً آفلاین",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is OcrStatus.Error -> {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                status.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Text(
                    "مدل‌های زبان (فارسی و انگلیسی) داخل خود برنامه ذخیره شده‌اند و به اینترنت نیازی ندارند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // ── About ────────────────────────────────────────────────────
            Text("درباره برنامه", style = MaterialTheme.typography.titleMedium)
            Text(
                "متن‌خوان یک ابزار آفلاین برای استخراج متن فارسی و انگلیسی از تصویر است. تمام پردازش‌ها روی دستگاه شما انجام می‌شود و هیچ داده‌ای به سرور ارسال نمی‌شود.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "نسخه ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "سازنده: دانیال افشاری",
                style = MaterialTheme.typography.bodyMedium
            )
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("تلگرام:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "SahandSoft",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://t.me/SahandSoft")
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}
