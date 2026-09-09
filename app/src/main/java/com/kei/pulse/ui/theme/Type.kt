@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.kei.pulse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kei.pulse.R

// Bricolage Grotesque for words, Azeret Mono for every live number. Both are variable fonts (OFL), so
// each weight is one FontVariation instance of the single file. Both set tabular figures so readouts
// don't jitter as digits change.
private fun bricolage(weight: FontWeight) = Font(
    R.font.bricolage_grotesque,
    weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private fun azeret(weight: FontWeight) = Font(
    R.font.azeret_mono,
    weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Bricolage = FontFamily(
    bricolage(FontWeight.Normal),
    bricolage(FontWeight.Medium),
    bricolage(FontWeight.SemiBold),
)

val Azeret = FontFamily(
    azeret(FontWeight.Normal),
    azeret(FontWeight.Medium),
)

private const val TABULAR = "tnum"

private fun sans(weight: FontWeight, size: Int, line: Int) = TextStyle(
    fontFamily = Bricolage,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = TABULAR,
)

private fun mono(weight: FontWeight, size: Int, line: Int) = TextStyle(
    fontFamily = Azeret,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = (-0.02).sp,
    fontFeatureSettings = TABULAR,
)

/** Live numeric readouts. Not part of the Material scale on purpose. */
val ReadoutHero = mono(FontWeight.Medium, 26, 28)   // live column clocks
val ReadoutLarge = mono(FontWeight.Medium, 22, 24)  // header fps / ms / W
val ReadoutMedium = mono(FontWeight.Normal, 16, 18) // gauges
val ReadoutSmall = mono(FontWeight.Normal, 15, 18)  // slider values, chips
val ReadoutTiny = mono(FontWeight.Normal, 12, 14)   // overlay

val PulseTypography = Typography(
    displayLarge   = sans(FontWeight.SemiBold, 40, 44),
    displayMedium  = sans(FontWeight.SemiBold, 32, 36),
    headlineMedium = sans(FontWeight.SemiBold, 24, 28),
    headlineSmall  = sans(FontWeight.SemiBold, 20, 22),
    titleLarge     = sans(FontWeight.SemiBold, 16, 20),
    titleMedium    = sans(FontWeight.Medium, 15, 20),
    titleSmall     = sans(FontWeight.Medium, 14, 18),
    bodyLarge      = sans(FontWeight.Normal, 15, 20),
    bodyMedium     = sans(FontWeight.Normal, 13, 18),
    bodySmall      = sans(FontWeight.Normal, 12, 16),
    labelLarge     = sans(FontWeight.Medium, 13, 16),
    labelMedium    = sans(FontWeight.Normal, 12, 16),
    labelSmall     = sans(FontWeight.Normal, 11, 14),
)
