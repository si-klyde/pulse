package com.kei.pulse.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.kei.pulse.data.FanController
import com.kei.pulse.model.AutoTdpBias
import com.kei.pulse.model.PerAppConfig
import com.kei.pulse.model.PerformanceProfile
import com.kei.pulse.model.PowerTier
import com.kei.pulse.model.ProfileSource
import com.kei.pulse.model.ProfileStateResolver
import com.kei.pulse.ui.theme.HudBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

/** Settings sub-screen: bind a power tier / saved profile (+ system extras) to installed apps. */
@Composable
fun PerAppScreen(
    configs: List<PerAppConfig>,
    learnedPackages: Set<String> = emptySet(),
    profiles: List<PerformanceProfile>,
    batteryCapacityWh: Float,
    fpsOptions: List<Int>,
    defaultFpsTarget: Int,
    defaultAggressivePark: Boolean,
    onSaveConfig: (PerAppConfig) -> Unit,
    onRemoveConfig: (String) -> Unit,
    onBack: () -> Unit,
    /** Hosted in the rail shell: no page background, no title row. */
    embedded: Boolean = false,
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var editingApp by remember { mutableStateOf<InstalledApp?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
                .asSequence()
                .map { it.activityInfo }
                .filter { it.packageName != context.packageName }
                .distinctBy { it.packageName }
                .map { info ->
                    InstalledApp(
                        packageName = info.packageName,
                        label = info.loadLabel(pm).toString(),
                        icon = runCatching {
                            info.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
                        }.getOrNull(),
                    )
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }
    }

    val configsByPackage = configs.associateBy { it.packageName }

    HudBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = if (embedded) 24.dp else 20.dp, vertical = if (embedded) 14.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!embedded) Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Per-app profiles",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tap an app to bind a profile and extras",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onBack) {
                    Text("Done")
                }
            }

            val loaded = apps
            if (loaded == null) {
                Text(
                    text = "Loading apps…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val query = searchQuery.trim()
                val filtered = if (query.isEmpty()) {
                    loaded
                } else {
                    loaded.filter { it.label.contains(query, ignoreCase = true) }
                }
                val sorted = filtered.sortedByDescending { configsByPackage.containsKey(it.packageName) }
                Text(
                    text = "Games with their own rules win over Power while they are in front. Tap a game to set or change them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    items(sorted, key = { it.packageName }) { app ->
                        val rowConfig = configsByPackage[app.packageName]
                        PerAppRow(
                            app = app,
                            config = rowConfig,
                            profiles = profiles,
                            batteryCapacityWh = batteryCapacityWh,
                            // null = not an AutoTDP binding (no badge); true = learned warm-start exists; false = still learning.
                            tuned = if (PerAppConfig.isAuto(rowConfig?.profileBinding)) {
                                app.packageName in learnedPackages
                            } else {
                                null
                            },
                            onClick = { editingApp = app },
                        )
                    }
                }
            }
        }
    }

    editingApp?.let { app ->
        PerAppConfigDialog(
            app = app,
            existing = configsByPackage[app.packageName],
            profiles = profiles,
            fpsOptions = fpsOptions,
            defaultFpsTarget = defaultFpsTarget,
            defaultAggressivePark = defaultAggressivePark,
            onSave = { config ->
                if (config.hasAnyBinding) onSaveConfig(config) else onRemoveConfig(app.packageName)
                editingApp = null
            },
            onRemove = {
                onRemoveConfig(app.packageName)
                editingApp = null
            },
            onDismiss = { editingApp = null },
        )
    }
}

private fun bindingSummary(config: PerAppConfig?, profiles: List<PerformanceProfile>): String {
    if (config == null) return "Follows Power"
    val parts = mutableListOf<String>()
    when {
        config.profileBinding == null -> {}
        PerAppConfig.isAutoOff(config.profileBinding) -> parts += "Off · runs stock"
        PerAppConfig.isAuto(config.profileBinding) -> {
            parts += "Auto" + (config.fpsTarget?.let { " · hold ${PerAppConfig.fpsTargetLabel(it)}" } ?: "")
            config.bias?.let { parts += it.label }
            if (config.aggressivePark == true) parts += "aggressive park"
        }
        else -> PerAppConfig.tierFromBinding(config.profileBinding)?.let { parts += "Manual · ${it.label} tier" }
            ?: run { parts += "Manual · ${profiles.firstOrNull { it.id == config.profileBinding }?.name ?: "saved setup"}" }
    }
    config.fanMode?.let { parts += "fan ${FanController.labelFor(it)}" }
    if (!PerAppConfig.isAuto(config.profileBinding)) config.refreshRateHz?.let { parts += "$it Hz" }
    return if (parts.isEmpty()) "Follows Power" else parts.joinToString(" · ")
}

/** "3h 51m" style runtime from a full battery at the given sustained draw. */
private fun formatRuntime(capacityWh: Float, watts: Float): String {
    val totalMinutes = (capacityWh / watts * 60f).toInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
private fun PerAppRow(
    app: InstalledApp,
    config: PerAppConfig?,
    profiles: List<PerformanceProfile>,
    batteryCapacityWh: Float,
    tuned: Boolean?,
    onClick: () -> Unit,
) {
    val configured = config != null
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            app.icon?.let { icon -> Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp)) }
                ?: Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (tuned == true) {
                        Text(
                            "learned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline).padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(
                    text = bindingSummary(config, profiles) + if (tuned == false) " · learning" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (configured) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
                    maxLines = 1,
                )
            }
            val peakW = config?.measuredPeakW ?: 0f
            val avgW = config?.measuredAvgW ?: 0f
            val drawW = if (avgW > 0f) avgW else peakW
            if (drawW > 0f) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f W", drawW),
                        style = com.kei.pulse.ui.theme.ReadoutSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (batteryCapacityWh > 0f) {
                        Text(
                            text = "≈ ${formatRuntime(batteryCapacityWh, drawW)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerAppConfigDialog(
    app: InstalledApp,
    existing: PerAppConfig?,
    profiles: List<PerformanceProfile>,
    fpsOptions: List<Int>,
    defaultFpsTarget: Int,
    defaultAggressivePark: Boolean,
    onSave: (PerAppConfig) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var profileBinding by remember { mutableStateOf(existing?.profileBinding) }
    var fanMode by remember { mutableStateOf(existing?.fanMode) }
    var refreshRate by remember { mutableStateOf(existing?.refreshRateHz) }
    // No "Default" chips: pre-fill from the global setting (snapped to this SoC's options) and always save
    // a concrete value. Old null bindings still inherit via the service until re-saved here.
    var fpsTarget by remember {
        mutableStateOf(PerAppConfig.snapFpsTarget(existing?.fpsTarget ?: defaultFpsTarget, fpsOptions))
    }
    var aggressivePark by remember { mutableStateOf(existing?.aggressivePark ?: defaultAggressivePark) }
    var bias by remember { mutableStateOf(existing?.bias) } // null = inherit the global default

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, maxLines = 1) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DialogGroupLabel("Power for this game")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DialogChip("Follows Power", profileBinding == null) { profileBinding = null }
                    DialogChip("Auto", PerAppConfig.isAuto(profileBinding)) {
                        profileBinding = PerAppConfig.AUTO_BINDING
                    }
                    DialogChip("Off · runs stock", PerAppConfig.isAutoOff(profileBinding)) {
                        profileBinding = PerAppConfig.AUTO_OFF_BINDING
                    }
                    PowerTier.entries.forEach { tier ->
                        val binding = PerAppConfig.tierBinding(tier)
                        DialogChip(tier.label, profileBinding == binding) { profileBinding = binding }
                    }
                    profiles
                        .filter { it.source != ProfileSource.VIRTUAL }
                        .forEach { profile ->
                            DialogChip(profile.name, profileBinding == profile.id) {
                                profileBinding = profile.id
                            }
                        }
                }

                // AutoTDP uses the global fan choice (Custom cascades, anything else runs as Smart); the per-app
                // fan picker is replaced by a note for it until the service layer supports per-app fan overrides
                // during AutoTDP.
                if (PerAppConfig.isAuto(profileBinding)) {
                    DialogGroupLabel("Fan")
                    Text(
                        text = "Uses the global fan while AutoTDP tunes this app: a Custom fan keeps running " +
                            "(cascaded); Silent, Smart or Sport run as Smart.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    DialogGroupLabel("Fan")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DialogChip("Same as Power", fanMode == null) { fanMode = null }
                        FanController.MODES.forEach { mode ->
                            DialogChip(mode.label, fanMode == mode.value) { fanMode = mode.value }
                        }
                    }
                }

                if (PerAppConfig.isAuto(profileBinding)) {
                    // AutoTDP owns the refresh rate (pins the panel to max), so the user picks an FPS
                    // target instead, AutoTDP trims clocks to hold it.
                    DialogGroupLabel("Frame rate to hold")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        fpsOptions.forEach { target ->
                            DialogChip(PerAppConfig.fpsTargetLabel(target), fpsTarget == target) {
                                fpsTarget = target
                            }
                        }
                    }
                    // Aggressive core parking is part of the AutoTDP algorithm, so it's set per app here.
                    DialogGroupLabel("Aggressive park")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DialogChip("On", aggressivePark) { aggressivePark = true }
                        DialogChip("Off", !aggressivePark) { aggressivePark = false }
                    }
                    DialogGroupLabel("Lean towards")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DialogChip("Same as Power", bias == null) { bias = null }
                        AutoTdpBias.entries.forEach { b ->
                            DialogChip(b.label, bias == b) { bias = b }
                        }
                    }
                } else {
                    DialogGroupLabel("Refresh rate")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DialogChip("Same as Power", refreshRate == null) { refreshRate = null }
                        listOf(60, 90, 120).forEach { hz ->
                            DialogChip("$hz Hz", refreshRate == hz) { refreshRate = hz }
                        }
                    }
                }

                Text(
                    text = "Applied when this app comes to the foreground; the previous state is restored when it leaves. \"Default\" leaves that control alone. AutoTDP pins the panel to max refresh and trims the CPU then GPU to hold your FPS target at the lowest power; pair it with the global Custom fan on the main screen. \"Default\" target uses the global default, \"Max\" runs uncapped. Custom applies your saved Custom setup, for frequencies unique to this app, save a profile on the main screen and bind it here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    PerAppConfig(
                        packageName = app.packageName,
                        appLabel = app.label,
                        profileBinding = profileBinding,
                        fanMode = fanMode,
                        refreshRateHz = refreshRate,
                        fpsTarget = fpsTarget,
                        aggressivePark = aggressivePark,
                        bias = bias,
                    ),
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = onRemove) {
                        Text("Remove")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}

@Composable
private fun DialogGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DialogChip(label: String, selected: Boolean, onClick: () -> Unit) = com.kei.pulse.ui.shell.Chip(label, selected, onClick)
