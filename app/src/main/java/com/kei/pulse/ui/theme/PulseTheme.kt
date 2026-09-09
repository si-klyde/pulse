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
 * One fixed scheme regardless of system light/dark: black housing, white ink, no chromatic accent.
 * `primary` IS the ink — a selected control is an inverted fill, not a coloured one. The custom-accent
 * setting still works for people who want colour back; it only recolours `primary`.
 */
private fun instrumentColorScheme(settings: AppSettings): ColorScheme {
    val accent = if (settings.colorSource == AppColorSource.CUSTOM_ACCENT) Color(settings.accentColor) else Ink
    val onAccent = if (settings.colorSource == AppColorSource.CUSTOM_ACCENT) Ink else OnInk
    return darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = Raised,
        onPrimaryContainer = Ink,
        secondary = Ink2,
        onSecondary = OnInk,
        secondaryContainer = Rule2,
        onSecondaryContainer = Ink,
        tertiary = Ink2,
        onTertiary = OnInk,
        tertiaryContainer = Raised,
        onTertiaryContainer = Ink,
        background = Housing,
        onBackground = Ink,
        surface = Housing,
        onSurface = Ink,
        surfaceVariant = Raised,
        onSurfaceVariant = Ink2,
        surfaceContainerLowest = Housing,
        surfaceContainerLow = Housing,
        surfaceContainer = Housing,
        surfaceContainerHigh = Housing,
        surfaceContainerHighest = Raised,
        surfaceBright = Rule2,
        surfaceDim = Housing,
        surfaceTint = Color.Transparent,
        inverseSurface = Ink,
        inverseOnSurface = OnInk,
        outline = Rule2,
        outlineVariant = Rule,
        error = Brick,
        onError = Ink,
        errorContainer = BrickDeep,
        onErrorContainer = Brick,
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
