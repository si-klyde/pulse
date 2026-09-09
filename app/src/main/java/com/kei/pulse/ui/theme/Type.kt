package com.kei.pulse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kei.pulse.R

// One family for the interface (IBM Plex Sans); its mono sibling only where a live number IS the content.
// Both use tabular figures so readouts don't jitter as digits change.
val PlexSans = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
)

val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

private const val TABULAR = "tnum"

private fun sans(weight: FontWeight, size: Int, line: Int) = TextStyle(
    fontFamily = PlexSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = TABULAR,
)

private fun mono(weight: FontWeight, size: Int, line: Int) = TextStyle(
    fontFamily = PlexMono,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = TABULAR,
)

/** Live numeric readouts (clocks, temps, fps). Not part of the Material scale on purpose. */
val ReadoutLarge = mono(FontWeight.Medium, 28, 32)
val ReadoutMedium = mono(FontWeight.Medium, 18, 22)
val ReadoutSmall = mono(FontWeight.Normal, 13, 16)

val PulseTypography = Typography(
    displayLarge   = sans(FontWeight.SemiBold, 40, 44),
    displayMedium  = sans(FontWeight.SemiBold, 32, 36),
    headlineMedium = sans(FontWeight.SemiBold, 24, 28),
    headlineSmall  = sans(FontWeight.SemiBold, 20, 24),
    titleLarge     = sans(FontWeight.Medium, 18, 22),
    titleMedium    = sans(FontWeight.Medium, 16, 20),
    titleSmall     = sans(FontWeight.Medium, 14, 18),
    bodyLarge      = sans(FontWeight.Normal, 15, 20),
    bodyMedium     = sans(FontWeight.Normal, 13, 18),
    bodySmall      = sans(FontWeight.Normal, 12, 16),
    labelLarge     = sans(FontWeight.Medium, 13, 16),
    labelMedium    = sans(FontWeight.Medium, 12, 16),
    labelSmall     = sans(FontWeight.Medium, 11, 14),
)
