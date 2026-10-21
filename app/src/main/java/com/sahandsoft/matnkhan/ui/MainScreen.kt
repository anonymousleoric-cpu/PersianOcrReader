package com.sahandsoft.matnkhan.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sahandsoft.matnkhan.R
import com.sahandsoft.matnkhan.vm.MainViewModel
import com.sahandsoft.matnkhan.util.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val cameraFile = remember { ImageUtils.cameraOutputFile(context) }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cameraFile)
    }

    // ── Activity-result launchers (no runtime permissions needed) ─────────
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            viewModel.showMessage("انتخاب تصویر لغو شد.")
        } else {
            val copied = ImageUtils.copyUriToCache(context, uri)
            if (copied != null) {
                viewModel.onImageSelected(copied)
            } else {
                viewModel.showMessage(
                    "خواندن تصویر ممکن نشد؛ فایل ممکن است خراب یا پشتیبانی‌نشده باشد.",
                    isError = true
                )
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        when {
            !success -> viewModel.showMessage("گرفتن عکس لغو شد.")
            cameraFile.exists() && cameraFile.length() > 0 ->
                viewModel.onImageSelected(cameraFile)
            else -> viewModel.showMessage(
                "تصویر گرفته‌شده ذخیره نشد. دوباره تلاش کنید.",
                isError = true
            )
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) {
            viewModel.showMessage("ذخیره فایل لغو شد.")
        } else {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(uiState.resultText.toByteArray(Charsets.UTF_8))
                    output.flush()
                } ?: throw IllegalStateException("output stream is null")
            }.isSuccess
            viewModel.showMessage(
                if (ok) "فایل TXT با موفقیت ذخیره شد." else "ذخیره فایل ناموفق بود.",
                isError = !ok
            )
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(
                message = message.text,
                withDismissAction = true,
                duration = if (message.isError) SnackbarDuration.Long else SnackbarDuration.Short
            )
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton40("تنظیمات") { showSettings = true }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.imageFile == null) {
                GuideCard()
            }

            // ── Image selection card ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("انتخاب از گالری")
                        }
                        FilledTonalButton(
                            onClick = { cameraLauncher.launch(cameraUri) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("گرفتن با دوربین")
                        }
                    }

                    uiState.imageFile?.let { file ->
                        AsyncImage(
                            model = file,
                            contentDescription = "پیش‌نمایش تصویر انتخاب‌شده",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }

            // ── Main extract button ───────────────────────────────────────
            Button(
                onClick = { viewModel.extractText() },
                enabled = uiState.imageFile != null && !uiState.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("در حال پردازش…")
                } else {
                    Icon(
                        Icons.Filled.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("استخراج متن از تصویر")
                }
            }

            // ── Result card ───────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "متن استخراج‌شده",
                            style = MaterialTheme.typography.titleMedium
                        )
                        uiState.meanConfidence?.let { confidence ->
                            val label = when {
                                confidence >= 75 -> "دقت: بالا"
                                confidence >= 40 -> "دقت: متوسط"
                                else -> "دقت: پایین"
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    "$label ($confidence٪)",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.resultText,
                        onValueChange = viewModel::onTextChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 380.dp),
                        placeholder = {
                            Text(
                                "هنوز متنی استخراج نشده است.\nیک تصویر انتخاب کنید و دکمه «استخراج متن از تصویر» را لمس کنید.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = settings.resultTextSize.value.sp,
                            textDirection = TextDirection.Content
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ActionChip(
                            label = "کپی متن",
                            icon = Icons.Filled.ContentCopy,
                            onClick = {
                                if (uiState.resultText.isEmpty()) {
                                    viewModel.showMessage("متنی برای کپی وجود ندارد.", isError = true)
                                } else {
                                    clipboard.setText(AnnotatedString(uiState.resultText))
                                    viewModel.showMessage("متن کپی شد.")
                                }
                            }
                        )
                        ActionChip(
                            label = "اشتراک‌گذاری",
                            icon = Icons.Filled.Share,
                            onClick = {
                                if (uiState.resultText.isEmpty()) {
                                    viewModel.showMessage("متنی برای اشتراک‌گذاری وجود ندارد.", isError = true)
                                } else {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, uiState.resultText)
                                    }
                                    try {
                                        context.startActivity(
                                            Intent.createChooser(sendIntent, "اشتراک‌گذاری متن")
                                        )
                                    } catch (e: ActivityNotFoundException) {
                                        viewModel.showMessage(
                                            "هیچ برنامه‌ای برای اشتراک‌گذاری یافت نشد.",
                                            isError = true
                                        )
                                    }
                                }
                            }
                        )
                        ActionChip(
                            label = "ذخیره در فایل TXT",
                            icon = Icons.Filled.Save,
                            onClick = {
                                if (uiState.resultText.isEmpty()) {
                                    viewModel.showMessage("متنی برای ذخیره وجود ندارد.", isError = true)
                                } else {
                                    val stamp = SimpleDateFormat(
                                        "yyyyMMdd-HHmm", Locale.US
                                    ).format(Date())
                                    saveLauncher.launch("matnkhan-$stamp.txt")
                                }
                            }
                        )
                        ActionChip(
                            label = "پاک کردن متن",
                            icon = Icons.Filled.Delete,
                            onClick = { showClearDialog = true }
                        )
                        ActionChip(
                            label = "استخراج مجدد",
                            icon = Icons.Filled.Refresh,
                            enabled = uiState.imageFile != null && !uiState.isProcessing,
                            onClick = { viewModel.extractText() }
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            onDismiss = { showSettings = false },
            onThemeModeChange = viewModel::setThemeMode,
            onOcrLanguageChange = viewModel::setOcrLanguage,
            onResultTextSizeChange = viewModel::setResultTextSize
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("پاک کردن متن") },
            text = { Text("آیا از پاک کردن کل متن استخراج‌شده مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearText()
                    showClearDialog = false
                    viewModel.showMessage("متن پاک شد.")
                }) { Text("پاک کن") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun GuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "راهنمای سریع",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                "۱. یک تصویر از گالری انتخاب کنید یا با دوربین عکس بگیرید.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "۲. روی «استخراج متن از تصویر» بزنید؛ پردازش کاملاً آفلاین و روی خود دستگاه انجام می‌شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "۳. متن را ویرایش کنید و آن را کپی، اشتراک‌گذاری یا به‌صورت فایل TXT ذخیره کنید.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "نکته: در اولین اجرا، بارگذاری مدل‌های زبان چند ثانیه طول می‌کشد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        enabled = enabled,
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    )
}

@Composable
private fun IconButton40(contentDescription: String, onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        Icon(Icons.Filled.Settings, contentDescription = contentDescription)
    }
}
