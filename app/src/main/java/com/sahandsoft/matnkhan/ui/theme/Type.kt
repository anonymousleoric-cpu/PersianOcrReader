package com.sahandsoft.matnkhan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.sahandsoft.matnkhan.R

/** Vazirmatn — open (OFL) Persian font with full Latin coverage. */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val Base = Typography()

val AppTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Vazirmatn),
    displayMedium = Base.displayMedium.copy(fontFamily = Vazirmatn),
    displaySmall = Base.displaySmall.copy(fontFamily = Vazirmatn),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Vazirmatn),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Vazirmatn),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Vazirmatn),
    titleLarge = Base.titleLarge.copy(fontFamily = Vazirmatn),
    titleMedium = Base.titleMedium.copy(fontFamily = Vazirmatn),
    titleSmall = Base.titleSmall.copy(fontFamily = Vazirmatn),
    bodyLarge = Base.bodyLarge.copy(fontFamily = Vazirmatn),
    bodyMedium = Base.bodyMedium.copy(fontFamily = Vazirmatn),
    bodySmall = Base.bodySmall.copy(fontFamily = Vazirmatn),
    labelLarge = Base.labelLarge.copy(fontFamily = Vazirmatn),
    labelMedium = Base.labelMedium.copy(fontFamily = Vazirmatn),
    labelSmall = Base.labelSmall.copy(fontFamily = Vazirmatn),
)
