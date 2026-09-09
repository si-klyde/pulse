package com.kei.pulse.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.kei.pulse.model.AppColorSource
import com.kei.pulse.model.AppSettings

/**
 * One fixed dark scheme regardless of system light/dark: the app is an instrument panel, and a panel
 * does not change its housing with the room. The only user-tunable colour is the accent
 * (CUSTOM_ACCENT); everything else is the graphite/ink palette in [Color.kt].
 */
private fun instrumentColorScheme(settings: AppSettings): ColorScheme {
    val accent = if (settings.colorSource == AppColorSource.CUSTOM_ACCENT) Color(settings.accentColor) else InstrumentAccent
    return darkColorScheme(
        primary = accent,
        onPrimary = InstrumentOnAccent,
        primaryContainer = InstrumentAccentDeep,
        onPrimaryContainer = accent,
        secondary = InstrumentInkDim,
        onSecondary = InstrumentHousing,
        secondaryContainer = InstrumentRaised2,
        onSecondaryContainer = InstrumentInk,
        tertiary = InstrumentInkDim,
        onTertiary = InstrumentHousing,
        tertiaryContainer = InstrumentRaised,
        onTertiaryContainer = InstrumentInk,
        background = InstrumentHousing,
        onBackground = InstrumentInk,
        surface = InstrumentPanel,
        onSurface = InstrumentInk,
        surfaceVariant = InstrumentRaised,
        onSurfaceVariant = InstrumentInkDim,
        surfaceContainerLowest = InstrumentHousing,
        surfaceContainerLow = InstrumentBase,
        surfaceContainer = InstrumentPanel,
        surfaceContainerHigh = InstrumentRaised,
        surfaceContainerHighest = InstrumentRaised2,
        surfaceBright = InstrumentRaised2,
        surfaceDim = InstrumentBase,
        surfaceTint = accent,
        inverseSurface = InstrumentInk,
        inverseOnSurface = InstrumentHousing,
        outline = InstrumentRule,
        outlineVariant = InstrumentInkFaint,
        error = InstrumentBrick,
        onError = InstrumentInk,
        errorContainer = InstrumentBrickDeep,
        onErrorContainer = InstrumentBrick,
        scrim = Color(0xCC000000),
    )
}

@Composable
fun PulseTheme(
    settings: AppSettings = AppSettings(),
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = instrumentColorScheme(settings),
        typography = PulseTypography,
        shapes = PulseShapes,
        content = content,
    )
}
