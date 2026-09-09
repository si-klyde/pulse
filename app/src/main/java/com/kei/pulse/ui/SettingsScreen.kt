package com.kei.pulse.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.height
import com.kei.pulse.ui.shell.PulseSwitch
import com.kei.pulse.ui.shell.Seg
import com.kei.pulse.ui.shell.Chip
import com.kei.pulse.ui.shell.pulseSliderColors
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kei.pulse.model.AppColorSource
import com.kei.pulse.model.OverlayElement
import com.kei.pulse.model.OverlayPreset
import com.kei.pulse.model.RgbMode
import com.kei.pulse.model.RgbStick
import com.kei.pulse.ui.theme.HudBackground
import com.kei.pulse.model.AppSettings
import com.kei.pulse.model.PerformanceProfile
import com.kei.pulse.model.TileInteractionBehavior
import kotlin.math.roundToInt


@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onPulseEnabledChange: (Boolean) -> Unit = {},
    onRgbModeChange: (RgbMode) -> Unit = {},
    onRgbManualTargetChange: (RgbStick) -> Unit = {},
    onRgbManualStickChange: (RgbStick, Int, Float) -> Unit = { _, _, _ -> },
    onColorSourceChange: (AppColorSource) -> Unit,
    onAccentColorChange: (Int) -> Unit,
    onTileTapBehaviorChange: (TileInteractionBehavior) -> Unit,
    onApplyLastProfileOnBootChange: (Boolean) -> Unit,
    sleepProfileOptions: List<PerformanceProfile>,
    onSleepProfileEnabledChange: (Boolean) -> Unit,
    onSleepProfileChange: (String?) -> Unit,
    onResetProfiles: () -> Unit,
    onExportProfiles: () -> Unit,
    onImportProfiles: () -> Unit,
    onRequestAddQuickSettingsTile: () -> Unit,
    canRequestAddQuickSettingsTile: Boolean,
    isQuickSettingsTileAdded: Boolean,
    perAppEnabled: Boolean = false,
    perAppConfiguredCount: Int = 0,
    onPerAppEnabledChange: (Boolean) -> Unit = {},
    onOpenPerApps: () -> Unit = {},
    perAppSwitchNotices: Boolean = true,
    onPerAppSwitchNoticesChange: (Boolean) -> Unit = {},
    overlayEnabled: Boolean = false,
    overlayPreset: OverlayPreset = OverlayPreset.COMPACT,
    overlayElements: Set<OverlayElement> = OverlayPreset.COMPACT.elements,
    overlayOpacity: Int = 90,
    onOverlayEnabledChange: (Boolean) -> Unit = {},
    onOverlayPresetChange: (OverlayPreset) -> Unit = {},
    onOverlayElementToggle: (OverlayElement, Boolean) -> Unit = { _, _ -> },
    onOverlayOpacityChange: (Int) -> Unit = {},
    onQuickAccessChange: (Boolean) -> Unit = {},
    onQuickAccessShowHandleChange: (Boolean) -> Unit = {},
    onSetQuickAccessCombo: () -> Unit = {},
    onClearQuickAccessCombo: () -> Unit = {},
    capturingCombo: Boolean = false,
    /** Vendor charging controls (RP6): null = not readable yet; the group hides when the device lacks the node. */
    chargingSupported: Boolean = false,
    chargingSeparation: Boolean? = null,
    chargeLimit80: Boolean? = null,
    onChargingSeparationChange: (Boolean) -> Unit = {},
    onChargeLimit80Change: (Boolean) -> Unit = {},
    onChargeWhileScreenOffChange: (Boolean) -> Unit = {},
    /** Hosted in the rail shell: no page background, no title row. */
    embedded: Boolean = false,
    /** When set, only sections whose title is listed render (the rail splits Settings into Overlay / Lights / System). */
    only: Set<String>? = null,
) {
    var showResetConfirmation by remember { mutableStateOf(false) }
    val show: (String) -> Boolean = { only == null || it in only }

    HudBackground(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = if (embedded) 24.dp else 20.dp, vertical = if (embedded) 14.dp else 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (!embedded) Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "P.U.L.S.E.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Performance Utility for Load and System Efficiency",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onBack) {
                Text("Done")
            }
        }

        if (show("PULSE")) SettingsSection(title = "PULSE") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (settings.pulseEnabled) "PULSE is on" else "System in control",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Off hands every control back to the manufacturer defaults — uncapped clocks, Smart fan, " +
                            "stock governor and refresh rate — and stops the background service. Turn it off before uninstalling.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                PulseSwitch(
                    checked = settings.pulseEnabled,
                    onCheckedChange = onPulseEnabledChange,
                )
            }
        }

        if (show("Quick Settings Tile")) SettingsSection(title = "Quick Settings Tile") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Add PULSE to Quick Settings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(
                    onClick = onRequestAddQuickSettingsTile,
                    enabled = canRequestAddQuickSettingsTile && !isQuickSettingsTileAdded,
                ) {
                    Text(
                        when {
                            isQuickSettingsTileAdded -> "Tile already added"
                            canRequestAddQuickSettingsTile -> "Add tile"
                            else -> "Unavailable"
                        },
                    )
                }
            }
            SettingsControlGroup(label = "Single tap") {
                TileBehaviorSelector(
                    selected = settings.tileTapBehavior,
                    onChange = onTileTapBehaviorChange,
                )
            }
        }

        if (show("Charging") && chargingSupported) SettingsSection(title = "Charging") {
            ChargingRow(
                title = "Charging separation",
                caption = "While the screen is on, power comes from the charger and the battery is left alone — cooler and " +
                    "kinder to the cell. The vendor turns charging back on when the screen goes off.",
                checked = chargingSeparation,
                onChange = onChargingSeparationChange,
            )
            ChargingRow(
                title = "Stop at 80 %",
                caption = "The vendor's charge limit for battery longevity.",
                checked = chargeLimit80,
                onChange = onChargeLimit80Change,
            )
            ChargingRow(
                title = "Always charge while the screen is off",
                caption = "Fixes a vendor bug: after plugging in while asleep, or after low memory, separation can stay on with " +
                    "the screen off and the battery never charges. PULSE checks once a minute while the screen is off and " +
                    "re-enables charging if needed. Never touches anything while the screen is on.",
                checked = settings.chargeWhileScreenOff,
                onChange = onChargeWhileScreenOffChange,
            )
        }

        if (show("Startup")) SettingsSection(title = "Startup") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Re-apply manual clocks after a reboot",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Puts your last tier or Custom limits back as soon as the device boots, before PULSE is opened. Auto does not need this — it takes over whenever a game is in front.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                PulseSwitch(
                    checked = settings.applyLastProfileOnBoot,
                    onCheckedChange = onApplyLastProfileOnBootChange,
                )
            }
        }

        if (show("Sleep")) SettingsSection(title = "Sleep") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Apply sleep profile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "When enabled, PULSE keeps a low-priority notification so it can apply this profile when the screen turns off and restore the previous limits when the device wakes.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                PulseSwitch(
                    checked = settings.sleepProfileEnabled,
                    onCheckedChange = onSleepProfileEnabledChange,
                    enabled = sleepProfileOptions.isNotEmpty(),
                )
            }
            if (sleepProfileOptions.isEmpty()) {
                Text(
                    text = "No profiles are available yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                SettingsControlGroup(label = "Profile while asleep") {
                    SleepProfileSelector(
                        profiles = sleepProfileOptions,
                        selectedProfileId = settings.sleepProfileId,
                        enabled = settings.sleepProfileEnabled,
                        onChange = onSleepProfileChange,
                    )
                }
            }
        }

        if (show("Per-app profiles")) SettingsSection(title = "Per-app profiles") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Switch profiles per app",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Applies an app's bound profile and extras when it launches, and restores the previous state when it exits. Needs the Usage access permission.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                PulseSwitch(
                    checked = perAppEnabled,
                    onCheckedChange = onPerAppEnabledChange,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (perAppConfiguredCount == 1) {
                        "1 app configured"
                    } else {
                        "$perAppConfiguredCount apps configured"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenPerApps) {
                    Text("Configure apps")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Switch notifications",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Show a toast and status notification when a per-app profile applies or restores.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                PulseSwitch(
                    checked = perAppSwitchNotices,
                    onCheckedChange = onPerAppSwitchNoticesChange,
                )
            }
        }

        if (show("On-screen overlay")) SettingsSection(title = "On-screen overlay") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Show in-game overlay",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Floats live FPS, clocks, temps, power and battery time-left over apps you've configured in Per-app profiles. Needs the \"Display over other apps\" permission. While unlocked, drag to reposition and tap LAYOUT to switch density; the notification's \"Move overlay\" action toggles the lock during play.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                PulseSwitch(
                    checked = overlayEnabled,
                    onCheckedChange = onOverlayEnabledChange,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Quick Access bar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Adjust performance, fan, and lighting without leaving your game. Open the panel " +
                            "with the edge handle or your controller shortcut — changes apply instantly.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                PulseSwitch(
                    checked = settings.quickAccessEnabled,
                    onCheckedChange = onQuickAccessChange,
                )
            }
            if (settings.quickAccessEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Edge handle",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Show a small tab on the screen edge that opens the panel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PulseSwitch(
                        checked = settings.quickAccessShowHandle,
                        onCheckedChange = onQuickAccessShowHandleChange,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Controller shortcut: " +
                            com.kei.pulse.data.InputComboParser.displayName(
                                com.kei.pulse.data.InputComboParser.decode(settings.quickAccessCombo),
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (capturingCombo) {
                            Text(
                                text = "Hold the buttons on your controller…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        } else {
                            TextButton(onClick = onSetQuickAccessCombo) { Text("Set shortcut") }
                            TextButton(onClick = onClearQuickAccessCombo) { Text("Clear") }
                        }
                    }
                }
            }
            SettingsControlGroup(label = "Layout · density + quick-fill") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverlayPreset.entries.forEach { preset ->
                        Chip(preset.label, overlayPreset == preset) { onOverlayPresetChange(preset) }
                    }
                }
            }
            SettingsControlGroup(label = "Shown items") {
                Text(
                    text = "Tap to add or remove. Grouped by what it measures; a few items only render in the " +
                        "Detailed/Full layouts (core bars, chip name, FPS trend).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OverlayItemGroups.forEach { group ->
                    OverlayItemGroup(
                        title = group.title,
                        items = group.items,
                        selected = overlayElements,
                        onToggle = onOverlayElementToggle,
                    )
                }
            }
            SettingsControlGroup(label = "Opacity · $overlayOpacity%") {
                Slider(
                    colors = pulseSliderColors(),
                    value = overlayOpacity.toFloat(),
                    onValueChange = { onOverlayOpacityChange(it.roundToInt()) },
                    valueRange = 40f..100f,
                )
            }
        }

        if (show("Joystick RGB")) SettingsSection(title = "Joystick RGB") {
            Text(
                text = "Color the controller's joystick LEDs. Battery and Heat glow with device status; " +
                    "Manual sets your own color per stick. Turn the lights on in your system settings to see them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RgbMode.entries.forEach { mode ->
                    Chip(mode.label, settings.rgbMode == mode) { onRgbModeChange(mode) }
                }
            }
            if (settings.rgbMode == RgbMode.MANUAL) {
                ManualRgbControls(
                    settings = settings,
                    onTargetChange = onRgbManualTargetChange,
                    onStickChange = onRgbManualStickChange,
                )
            }
        }

        if (show("Profiles")) SettingsSection(title = "Profiles") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Share profiles",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Export profiles to JSON or import a shared profile file.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onImportProfiles) {
                        Text("Import")
                    }
                    TextButton(onClick = onExportProfiles) {
                        Text("Export")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Reset profiles to default",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Bundled profiles are restored and custom profiles are removed.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(onClick = { showResetConfirmation = true }) {
                    Text("Reset")
                }
            }
        }
        if (show("About")) SettingsSection(title = "About") {
            Text(
                text = "PULSE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "No-root CPU, GPU, fan and lighting control for the Retroid Pocket 6, AYN Odin 3 and AYN Thor. " +
                    "Uses the device's own PServer service; never asks for root.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Fork of PULSE 1.19.6 by keiretrogaming · GPL v2 · credits in NOTICE.md",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset profiles?") },
            text = {
                Text("This removes custom profiles and restores the bundled defaults.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        onResetProfiles()
                    },
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepProfileSelector(
    profiles: List<PerformanceProfile>,
    selectedProfileId: String?,
    enabled: Boolean,
    onChange: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedProfile = profiles.firstOrNull { profile -> profile.id == selectedProfileId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedProfile?.name ?: "Select profile",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = enabled)
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.name) },
                    onClick = {
                        onChange(profile.id)
                        expanded = false
                    },
                )
            }
        }
    }
}




@Composable
private fun TileBehaviorSelector(
    selected: TileInteractionBehavior,
    onChange: (TileInteractionBehavior) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Seg("Quick dialog", selected == TileInteractionBehavior.SHOW_DIALOG, { onChange(TileInteractionBehavior.SHOW_DIALOG) }, height = 40)
        Seg("Cycle profiles", selected == TileInteractionBehavior.CYCLE_PROFILES, { onChange(TileInteractionBehavior.CYCLE_PROFILES) }, height = 40)
        Seg("Open app", selected == TileInteractionBehavior.OPEN_APP, { onChange(TileInteractionBehavior.OPEN_APP) }, height = 40)
    }
}


@Composable
private fun ManualRgbControls(
    settings: AppSettings,
    onTargetChange: (RgbStick) -> Unit,
    onStickChange: (RgbStick, Int, Float) -> Unit,
) {
    val target = settings.rgbManualTarget
    // The edited stick's stored color (at full value) + brightness. "Both" edits from the Left values.
    val storedColor = if (target == RgbStick.RIGHT) settings.rgbManualRightColor else settings.rgbManualLeftColor
    val storedBright = if (target == RgbStick.RIGHT) settings.rgbManualRightBrightness else settings.rgbManualLeftBrightness

    // Local pick state, re-seeded whenever the edited stick changes so each loads its own stored values.
    val seedHue = remember(target) { FloatArray(3).also { android.graphics.Color.colorToHSV(storedColor, it) }[0] }
    var hue by remember(target) { mutableStateOf(seedHue) }
    var bright by remember(target) { mutableStateOf(storedBright) }

    fun commit() = onStickChange(target, android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)), bright)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left — stick target + L/R swatches
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Stick",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RgbStick.entries.forEach { stick ->
                    Chip(stick.label, target == stick) { onTargetChange(stick) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ManualStickSwatch("L", settings.rgbManualLeftColor, settings.rgbManualLeftBrightness)
                ManualStickSwatch("R", settings.rgbManualRightColor, settings.rgbManualRightBrightness)
            }
        }
        // Right — the wordmark picker, fixed width, flush to the right edge
        Column(
            modifier = Modifier.width(380.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PulseColorPicker(
                modifier = Modifier.fillMaxWidth(),
                hue = hue,
                brightness = bright,
                onHue = { hue = it },
                onBrightness = { bright = it },
                onCommit = { commit() },
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text(
                    text = "color",
                    modifier = Modifier.weight(17f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "brightness",
                    modifier = Modifier.weight(12f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ManualStickSwatch(label: String, color: Int, brightness: Float) {
    val b = brightness.coerceIn(0f, 1f)
    val swatch = Color(
        red = (((color shr 16) and 0xFF) * b) / 255f,
        green = (((color shr 8) and 0xFF) * b) / 255f,
        blue = ((color and 0xFF) * b) / 255f,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(swatch, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private val PULSE_GLYPHS = listOf(
    listOf("11110", "10001", "10001", "11110", "10000", "10000", "10000"), // P
    listOf("10001", "10001", "10001", "10001", "10001", "10001", "01110"), // U
    listOf("10000", "10000", "10000", "10000", "10000", "10000", "11111"), // L
    listOf("01111", "10000", "10000", "01110", "00001", "00001", "11110"), // S
    listOf("11111", "10000", "10000", "11110", "10000", "10000", "11111"), // E
)

/**
 * The PULSE wordmark, made into the picker. P-U-L are a hue spectrum — drag across to choose colour. S-E are a
 * brightness ramp in the chosen hue — drag up/down to choose brightness. Equalizer bars above and below pulse.
 * Left ~3/5 (x < split) edits colour by x; right ~2/5 edits brightness by y. Commits on release.
 */
@Composable
private fun PulseColorPicker(
    hue: Float,
    brightness: Float,
    onHue: (Float) -> Unit,
    onBrightness: (Float) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // pointerInput(Unit) runs once and captures these — keep them fresh so switching sticks isn't ignored.
    val latestOnHue = rememberUpdatedState(onHue)
    val latestOnBrightness = rememberUpdatedState(onBrightness)
    val latestOnCommit = rememberUpdatedState(onCommit)
    val barAlpha by rememberInfiniteTransition(label = "bars").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1300, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "barAlpha",
    )
    Canvas(
        modifier = modifier
            .aspectRatio(2.4f)
            .pointerInput(Unit) {
                // One low-level gesture that CONSUMES from touch-down, so the parent vertical scroll can't steal
                // the drag (the old tap+drag detectors fought the scroll and froze). Handles tap and drag alike.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pickFromPosition(down.position, size.width.toFloat(), size.height.toFloat(), latestOnHue.value, latestOnBrightness.value)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        pickFromPosition(change.position, size.width.toFloat(), size.height.toFloat(), latestOnHue.value, latestOnBrightness.value)
                    }
                    latestOnCommit.value()
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val px = w * 0.04f
        val b = (w - px * 2f) / 29f
        val topOff = (h - 12.6f * b) / 2f
        val barH = 1.6f * b
        val letterY0 = topOff + 2.4f * b
        val letterH = 7f * b
        val barBotY = topOff + 10.2f * b
        val segGap = b * 0.3f

        fun bars(y: Float) {
            val cz = listOf(Color(0xFFE23B86), Color(0xFF2BD07A), Color(0xFF5B7CFF), Color(0xFFB56CFF))
            val czSeg = (17f * b - 3 * segGap) / 4f
            for (k in 0..3) drawRect(cz[k].copy(alpha = barAlpha), Offset(px + k * (czSeg + segGap), y), Size(czSeg, barH))
            val bzStart = px + 18f * b
            val bz = listOf(Color(0xFFCFD6E6), Color(0xFF9AA3B8), Color(0xFF5B6478))
            val bzSeg = (11f * b - 2 * segGap) / 3f
            for (k in 0..2) drawRect(bz[k].copy(alpha = barAlpha), Offset(bzStart + k * (bzSeg + segGap), y), Size(bzSeg, barH))
        }
        bars(topOff)
        bars(barBotY)

        val blk = b * 0.84f
        for (i in 0..4) {
            val glyph = PULSE_GLYPHS[i]
            val lx = px + i * 6f * b
            for (r in 0..6) for (c in 0..4) {
                if (glyph[r][c] != '1') continue
                val fill = if (i < 3) {
                    Color(android.graphics.Color.HSVToColor(floatArrayOf((i * 5 + c) / 14f * 360f, 0.85f, 1f)))
                } else {
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.85f, (1f - r / 6f).coerceIn(0.08f, 1f))))
                }
                drawRect(fill, Offset(lx + c * b, letterY0 + r * b), Size(blk, blk))
            }
        }

        val cw = 3.dp.toPx()
        // Hue cursor — vertical line over the P-U-L spectrum.
        val hx = px + (hue / 360f).coerceIn(0f, 1f) * 17f * b
        drawRect(Color.Black.copy(alpha = 0.5f), Offset(hx - cw, letterY0 - b * 0.4f), Size(cw * 2.2f, letterH + b * 0.8f))
        drawRect(Color.White, Offset(hx - cw / 2f, letterY0 - b * 0.4f), Size(cw, letterH + b * 0.8f))
        // Brightness cursor — horizontal line over the S-E ramp.
        val seL = px + 18f * b
        val seR = px + 29f * b
        val by = letterY0 + (1f - brightness.coerceIn(0f, 1f)) * letterH
        drawRect(Color.Black.copy(alpha = 0.5f), Offset(seL, by - cw), Size(seR - seL, cw * 2.2f))
        drawRect(Color.White, Offset(seL, by - cw / 2f), Size(seR - seL, cw))
    }
}

/** Map a touch position to hue (left zone, by x) or brightness (right zone, by y), matching the draw geometry. */
private fun pickFromPosition(pos: Offset, w: Float, h: Float, onHue: (Float) -> Unit, onBrightness: (Float) -> Unit) {
    val px = w * 0.04f
    val b = (w - px * 2f) / 29f
    val splitX = px + 17.5f * b
    val letterY0 = (h - 12.6f * b) / 2f + 2.4f * b
    val letterH = 7f * b
    if (pos.x < splitX) {
        onHue(((pos.x - px) / (17f * b)).coerceIn(0f, 1f) * 360f)
    } else {
        onBrightness((1f - (pos.y - letterY0) / letterH).coerceIn(0f, 1f))
    }
}

@Composable
private fun SettingsControlGroup(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

/** A hardware-domain grouping of overlay items, so the picker reads like an Afterburner/Adrenalin OSD config. */
private class OverlayItemGroupDef(val title: String, val items: List<Pair<OverlayElement, String>>)

// Short, in-context labels (the group header carries the "GPU"/"CPU" prefix) covering all 15 OverlayElements.
private val OverlayItemGroups = listOf(
    OverlayItemGroupDef(
        "Frame rate",
        listOf(OverlayElement.FPS to "FPS", OverlayElement.FPS_TREND to "Avg · Low · Trend"),
    ),
    OverlayItemGroupDef(
        "GPU",
        listOf(
            OverlayElement.GPU_LOAD to "Load",
            OverlayElement.GPU_CLOCK to "Clock",
            OverlayElement.GPU_TEMP to "Temp",
        ),
    ),
    OverlayItemGroupDef(
        "CPU",
        listOf(
            OverlayElement.CPU_LOAD to "Load",
            OverlayElement.CPU_CLOCK to "Clock",
            OverlayElement.CPU_TEMP to "Temp",
            OverlayElement.CORE_BARS to "Core bars",
        ),
    ),
    OverlayItemGroupDef(
        "System",
        listOf(
            OverlayElement.RAM to "RAM",
            OverlayElement.POWER to "Power",
            OverlayElement.BATTERY_LEFT to "Time left",
        ),
    ),
    OverlayItemGroupDef(
        "Status",
        listOf(
            OverlayElement.AUTOTDP to "AutoTDP",
            OverlayElement.SESSION_TIMER to "Timer",
            OverlayElement.SOC_NAME to "Chip name",
        ),
    ),
)

/** One domain row: a muted caption + a wrapping cluster of toggle chips (checked = shown on the overlay). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverlayItemGroup(
    title: String,
    items: List<Pair<OverlayElement, String>>,
    selected: Set<OverlayElement>,
    onToggle: (OverlayElement, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEach { (element, label) ->
                val isOn = element in selected
                Chip(label, isOn) { onToggle(element, !isOn) }
            }
        }
    }
}

@Composable
private fun ChargingRow(title: String, caption: String, checked: Boolean?, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PulseSwitch(checked = checked ?: false, onCheckedChange = onChange, enabled = checked != null)
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Flat group, not a card: a hairline above, the title as a quiet label, then the rows.
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

