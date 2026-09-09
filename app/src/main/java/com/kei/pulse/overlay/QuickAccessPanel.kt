package com.kei.pulse.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kei.pulse.data.FanController
import com.kei.pulse.model.AppSettings
import com.kei.pulse.model.AutoTdpBias
import com.kei.pulse.model.OverlayPreset
import com.kei.pulse.model.PerAppConfig
import com.kei.pulse.model.PowerTier
import com.kei.pulse.model.RgbMode
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

private val FPS_OPTIONS = listOf(30, 60, 120)
private const val FLASH_MS = 1800L // how long an applied-feedback confirmation stays in the footer

/**
 * One controller-focusable control. [render] draws it (highlighted when it's the cursor); [onActivate] /
 * [onLeft] / [onRight] are what A / D-pad left-right do to it. Touch works via the inner control's own onClick.
 */
private class NavItem(
    val render: @Composable (focused: Boolean) -> Unit,
    val onActivate: () -> Unit = {},
    val onLeft: () -> Unit = {},
    val onRight: () -> Unit = {},
)

/** A titled group of items in the single column; the bumpers jump between groups. */
private class NavGroup(val title: String, val status: String?, val items: List<NavItem>)

/** Root of the Quick Access overlay content — a collapsed handle, or the expanded right-docked panel. */
@Composable
fun QuickAccessContent(
    statsFlow: StateFlow<OverlayStats>,
    settingsFlow: StateFlow<AppSettings>,
    perAppFlow: StateFlow<PerAppConfig?>,
    expandedFlow: StateFlow<Boolean>,
    showHandleFlow: StateFlow<Boolean>,
    navIntents: SharedFlow<QuickAccessNavIntent>,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    onAction: (QuickAccessAction) -> Unit,
) {
    val expanded by expandedFlow.collectAsState()
    if (!expanded) {
        val showHandle by showHandleFlow.collectAsState()
        if (showHandle) QuickAccessHandle(onExpand) else Box(Modifier.size(1.dp)) // combo-only: invisible
        return
    }
    val stats by statsFlow.collectAsState()
    val settings by settingsFlow.collectAsState()
    val perApp by perAppFlow.collectAsState()
    QuickAccessPanel(stats, settings, perApp, navIntents, onClose, onAction)
}

/** 22×66 dp handle on the screen edge, rounded on the game side. */
@Composable
private fun QuickAccessHandle(onExpand: () -> Unit) {
    val shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
    Box(
        modifier = Modifier
            .size(width = 22.dp, height = 66.dp)
            .clip(shape)
            .background(SmokeDeep)
            .border(1.dp, Hair, shape)
            .clickable { onExpand() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Open Quick Access bar", tint = OsdInk, modifier = Modifier.size(18.dp))
    }
}

/**
 * The panel: one column ordered by how often it's reached for mid-game — brightness and volume first, then
 * Power (Auto | Manual | Off, frame rate, lean), Fan, Overlay, Lights. No tab rail: the D-pad walks straight
 * down, the bumpers jump between groups, and a thumb never hunts for a tab.
 */
@Composable
private fun QuickAccessPanel(
    stats: OverlayStats,
    settings: AppSettings,
    perApp: PerAppConfig?,
    navIntents: SharedFlow<QuickAccessNavIntent>,
    onClose: () -> Unit,
    onAction: (QuickAccessAction) -> Unit,
) {
    var cursor by remember { mutableIntStateOf(0) }
    // Optimistic local values for sliders so ←/→/tap step instantly instead of waiting a poll; re-synced from
    // telemetry while not being touched.
    val sliderLocal = remember { mutableStateMapOf<String, Int>() }
    // Applied-feedback flash in the footer. Auto-clears; flashKey retriggers when the same text flashes twice.
    var flash by remember { mutableStateOf<String?>(null) }
    var flashKey by remember { mutableIntStateOf(0) }
    val showFlash: (String) -> Unit = { flash = it; flashKey++ }
    LaunchedEffect(flashKey) { if (flash != null) { kotlinx.coroutines.delay(FLASH_MS); flash = null } }
    // The scope control's UNCOMMITTED ←/→ selection (browsing must never apply — committing "All games"
    // deletes the game's profile, so it takes an explicit A press / tap).
    var pendingScope by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(settings.quickAccessPerGameScope) { pendingScope = null }
    val autoTdpLive = QuickAccessPerApp.effectiveAutoTdpOn(perApp, settings.autoTdpDefaultEnabled)
    val dispatch: (QuickAccessAction) -> Unit = { a ->
        onAction(a)
        flashLabel(a, autoTdpLive)?.let(showFlash)
    }

    val groups: List<NavGroup> = listOf(
        NavGroup("", null, systemItems(stats, sliderLocal, dispatch)),
        powerGroup(stats, settings, perApp, sliderLocal, dispatch),
        fanGroup(settings, autoTdpLive, dispatch),
        overlayGroup(settings, dispatch),
        lightsGroup(settings, dispatch),
    )
    val items = groups.flatMap { it.items }
    val groupStarts = buildList { var i = 0; groups.forEach { add(i); i += it.items.size } }
    val curItems by rememberUpdatedState(items)
    val curStarts by rememberUpdatedState(groupStarts)
    LaunchedEffect(items.size) { cursor = QuickAccessNav.clampItem(cursor, items.size) }
    LaunchedEffect(stats.brightnessPercent, stats.volumePercent, stats.gpuCapKhz) {
        if (sliderLocal["bri"] == stats.brightnessPercent) sliderLocal.remove("bri")
        if (sliderLocal["vol"] == stats.volumePercent) sliderLocal.remove("vol")
        if (sliderLocal["gpucap"] == stats.gpuCapKhz) sliderLocal.remove("gpucap")
    }
    LaunchedEffect(Unit) {
        navIntents.collect { intent ->
            when (intent) {
                QuickAccessNavIntent.TAB_PREV -> cursor = QuickAccessNav.moveGroup(cursor, -1, curStarts)
                QuickAccessNavIntent.TAB_NEXT -> cursor = QuickAccessNav.moveGroup(cursor, 1, curStarts)
                QuickAccessNavIntent.UP -> cursor = QuickAccessNav.moveItem(cursor, -1, curItems.size)
                QuickAccessNavIntent.DOWN -> cursor = QuickAccessNav.moveItem(cursor, 1, curItems.size)
                QuickAccessNavIntent.LEFT -> curItems.getOrNull(cursor)?.onLeft?.invoke()
                QuickAccessNavIntent.RIGHT -> curItems.getOrNull(cursor)?.onRight?.invoke()
                QuickAccessNavIntent.ACTIVATE -> curItems.getOrNull(cursor)?.onActivate?.invoke()
            }
        }
    }

    val reduce = rememberReduceMotion()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enter by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(if (reduce) 0 else 200, easing = FastOutSlowInEasing),
        label = "qaEnter",
    )

    Row(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enter
                translationX = (1f - enter) * 36.dp.toPx()
            }
            .background(QaColors.Surface),
    ) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(Hair))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Header(stats, settings, perApp, pendingScope, { pendingScope = it }, showFlash, dispatch)
                var index = 0
                groups.forEach { g ->
                    if (g.title.isNotEmpty()) QaGroupHeader(g.title, g.status)
                    g.items.forEach { item -> val i = index++; item.render(i == cursor) }
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(Hair))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    flash ?: "",
                    style = QaLabel.copy(color = if (flash != null) OsdInk else OsdInk3, fontWeight = if (flash != null) FontWeight.SemiBold else FontWeight.Normal),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Close", style = QaBody, modifier = Modifier.clickable { onClose() }.padding(horizontal = 10.dp, vertical = 8.dp))
            }
        }
    }
}

/** PULSE + live strip, then the game and the This game / All games scope. */
@Composable
private fun Header(
    stats: OverlayStats,
    settings: AppSettings,
    perApp: PerAppConfig?,
    pendingScope: Int?,
    onPendingScope: (Int?) -> Unit,
    onFlash: (String) -> Unit,
    onAction: (QuickAccessAction) -> Unit,
) {
    val t = stats.telemetry
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("PULSE", style = QaBody.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${stats.fps?.fps?.roundToInt() ?: "—"} fps", style = QaNum)
                Text("${t.cpuTempC ?: "—"}°", style = QaNum.copy(color = meterTempColor(t.cpuTempC)))
                Text("${t.gpuTempC ?: "—"}°", style = QaNum.copy(color = meterTempColor(t.gpuTempC)))
                stats.powerDrawW?.let { Text(String.format(java.util.Locale.US, "%.1f W", it), style = QaNum.copy(color = OsdInk2)) }
                t.batteryPercent?.let { Text("$it%", style = QaNum.copy(color = OsdInk2)) }
            }
        }
        val perGame = settings.quickAccessPerGameScope
        val committed = if (perGame) 0 else 1
        val shown = pendingScope ?: committed
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stats.gameLabel ?: "This game", style = QaTitle, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x0F_FFFFFF)).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("This game", "All games").forEachIndexed { i, label ->
                    val sel = i == shown
                    Text(
                        label,
                        style = QaLabel.copy(color = if (sel) OsdInk else OsdInk3),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sel) Color(0x24_FFFFFF) else Color.Transparent)
                            .clickable {
                                val toPerGame = i == 0
                                if (toPerGame != perGame) {
                                    onAction(QuickAccessAction.SetScope(toPerGame))
                                    onFlash(
                                        when {
                                            toPerGame && perApp == null -> "Per-game profile created"
                                            toPerGame -> "Editing this game's profile"
                                            perApp != null -> "Profile removed — following All games"
                                            else -> "Following All games"
                                        },
                                    )
                                }
                                onPendingScope(null)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hair))
    }
}

// ---- Groups (pure builders; each NavItem carries its render + controller actions) ----

private fun powerGroup(
    stats: OverlayStats,
    settings: AppSettings,
    perApp: PerAppConfig?,
    sliderLocal: MutableMap<String, Int>,
    onAction: (QuickAccessAction) -> Unit,
): NavGroup {
    val perGame = settings.quickAccessPerGameScope
    val autoOn = if (perGame) QuickAccessPerApp.effectiveAutoTdpOn(perApp, settings.autoTdpDefaultEnabled) else settings.autoTdpDefaultEnabled
    val tier = if (perGame) PerAppConfig.tierFromBinding(perApp?.profileBinding) else PowerTier.entries.firstOrNull { it.label == settings.activeTierLabel }
    val stockSelected = if (perGame) PerAppConfig.isAutoOff(perApp?.profileBinding) else !autoOn && tier == null
    // Auto | Manual | Off. Auto = AutoTDP; Manual = a fixed tier (the current one, else Balanced); Off = stock.
    val mode = when {
        autoOn -> 0
        stockSelected -> 2
        else -> 1
    }
    val items = mutableListOf<NavItem>()
    items += chipNavItem(null, listOf("Auto", "Manual", "Off"), mode) { i ->
        when (i) {
            0 -> if (!autoOn) onAction(QuickAccessAction.ToggleAutoTdp)
            1 -> onAction(QuickAccessAction.SetTier(tier ?: PowerTier.BALANCED))
            else -> if (!stockSelected) onAction(QuickAccessAction.SetStockMode)
        }
    }
    when (mode) {
        0 -> {
            val fps = if (perGame) QuickAccessPerApp.effectiveFps(perApp, settings.autoTdpFpsTarget) else settings.autoTdpFpsTarget
            items += chipNavItem("Frame rate to hold", FPS_OPTIONS.map { it.toString() }, FPS_OPTIONS.indexOf(fps), mono = true) { i ->
                onAction(QuickAccessAction.SetFpsTarget(FPS_OPTIONS[i]))
            }
            val bias = if (perGame) QuickAccessPerApp.effectiveBias(perApp, settings.autoTdpBias) else settings.autoTdpBias
            items += chipNavItem("Lean towards", AutoTdpBias.entries.map { it.label }, AutoTdpBias.entries.indexOf(bias)) { i ->
                onAction(QuickAccessAction.SetBias(AutoTdpBias.entries[i]))
            }
            val park = if (perGame) QuickAccessPerApp.effectiveAggressivePark(perApp, settings.autoTdpAggressivePark) else settings.autoTdpAggressivePark
            items += toggleNavItem("Aggressive park", park) { onAction(QuickAccessAction.SetAggressivePark(!park)) }
        }
        1 -> {
            items += chipNavItem("Tier", PowerTier.entries.map { shortTier(it) }, PowerTier.entries.indexOf(tier)) { i -> onAction(QuickAccessAction.SetTier(PowerTier.entries[i])) }
            if (tier == PowerTier.CUSTOM) {
                val pt = if (settings.powerTargetEnabled) settings.powerTargetPercent else 100
                items += sliderNavItem("Power target", pt) { v ->
                    onAction(QuickAccessAction.SetPowerTarget(v.coerceIn(QuickAccess.POWER_TARGET_MIN, QuickAccess.POWER_TARGET_MAX)))
                }
                val levels = stats.gpuLevels?.sorted()
                if (!levels.isNullOrEmpty()) {
                    val cap = sliderLocal["gpucap"] ?: stats.gpuCapKhz ?: levels.last()
                    val idx = levels.indices.minByOrNull { kotlin.math.abs(levels[it] - cap) } ?: levels.lastIndex
                    items += stepperNavItem("GPU cap", "${levels[idx] / 1000} MHz") { d ->
                        val next = levels[(idx + d).coerceIn(0, levels.lastIndex)]
                        if (next != levels[idx]) { sliderLocal["gpucap"] = next; onAction(QuickAccessAction.SetGpuCap(next)) }
                    }
                }
            }
        }
    }
    val status = when (mode) {
        0 -> stats.autoTdp?.let { a -> "caps " + a.cpuClusters.joinToString(" ") { "${it.capPercent}" } + (a.gpuCapPercent?.let { " · gpu $it" } ?: "") }
        1 -> tier?.tagline
        else -> "runs stock"
    }
    return NavGroup("Power", status, items)
}

private fun shortTier(t: PowerTier): String = when (t) {
    PowerTier.MAX -> "Max"
    PowerTier.BALANCED -> "Balanced"
    PowerTier.POWER_SAVING -> "Saving"
    PowerTier.CUSTOM -> "Custom"
}

private fun fanGroup(settings: AppSettings, autoTdpLive: Boolean, onAction: (QuickAccessAction) -> Unit): NavGroup {
    val items = mutableListOf<NavItem>()
    val modes = FanController.MODES
    items += chipNavItem(null, modes.map { it.label }, modes.indexOfFirst { it.value == settings.managedFanMode }) { i ->
        onAction(QuickAccessAction.SetFanMode(modes[i].value))
    }
    if (autoTdpLive && settings.managedFanMode != FanController.CUSTOM) {
        items += noteItem("Runs as Smart while Auto tunes this game; only Custom keeps its own loop.")
    }
    if (settings.managedFanMode == FanController.CUSTOM) {
        items += toggleNavItem("Hold target temp", settings.fanSmartEnabled) { onAction(QuickAccessAction.SetFanSmart(!settings.fanSmartEnabled)) }
        if (settings.fanSmartEnabled) {
            items += stepperNavItem("Target", "${settings.fanTargetTempC} °C") { d -> onAction(QuickAccessAction.SetFanTargetTemp(settings.fanTargetTempC + d)) }
        } else {
            items += stepperNavItem("Cool ⟷ Quiet", fanBiasLabel(settings.fanBias)) { d ->
                val next = (settings.fanBias + d * 5).coerceIn(-com.kei.pulse.model.FanCurve.MAX_BIAS, com.kei.pulse.model.FanCurve.MAX_BIAS)
                if (next != settings.fanBias) onAction(QuickAccessAction.SetFanBias(next))
            }
        }
    }
    val status = if (settings.managedFanMode == FanController.CUSTOM && settings.fanSmartEnabled) "hold ${settings.fanTargetTempC}°" else null
    return NavGroup("Fan", status, items)
}

private fun fanBiasLabel(bias: Int): String = when {
    bias > 0 -> "+$bias cooler"
    bias < 0 -> "$bias quieter"
    else -> "0"
}

private fun overlayGroup(settings: AppSettings, onAction: (QuickAccessAction) -> Unit): NavGroup = NavGroup(
    "Overlay",
    null,
    buildList {
        add(toggleNavItem("Show overlay", settings.overlayEnabled) { onAction(QuickAccessAction.SetOverlayEnabled(!settings.overlayEnabled)) })
        add(chipNavItem(null, OverlayPreset.entries.map { it.label }, OverlayPreset.entries.indexOf(settings.overlayPreset)) { i ->
            onAction(QuickAccessAction.SetOverlayPreset(OverlayPreset.entries[i]))
        })
    },
)

private fun lightsGroup(settings: AppSettings, onAction: (QuickAccessAction) -> Unit): NavGroup {
    val items = mutableListOf<NavItem>()
    items += chipNavItem(null, RgbMode.entries.map { it.label }, RgbMode.entries.indexOf(settings.rgbMode)) { i ->
        onAction(QuickAccessAction.SetRgbMode(RgbMode.entries[i]))
    }
    if (settings.rgbMode == RgbMode.MANUAL) {
        items += swatchNavItem(RGB_SWATCHES, settings.rgbManualLeftColor) { c -> onAction(QuickAccessAction.SetRgbColor(c)) }
    }
    return NavGroup("Lights", null, items)
}

private fun systemItems(stats: OverlayStats, local: MutableMap<String, Int>, onAction: (QuickAccessAction) -> Unit): List<NavItem> = buildList {
    val bri = local["bri"] ?: stats.brightnessPercent ?: 50
    add(sliderNavItem("Brightness", bri) { v -> local["bri"] = v; onAction(QuickAccessAction.SetBrightness(v)) })
    val vol = local["vol"] ?: stats.volumePercent ?: 50
    add(sliderNavItem("Volume", vol) { v -> local["vol"] = v; onAction(QuickAccessAction.SetVolume(v)) })
}

// ---- NavItem builders ----

private fun chipNavItem(label: String?, options: List<String>, selectedIndex: Int, mono: Boolean = false, onPick: (Int) -> Unit) = NavItem(
    render = { focused -> QaFocusRow(focused) { QaSegmentedRow(label, options, selectedIndex, onPick, mono) } },
    onActivate = {},
    onLeft = { if (selectedIndex > 0) onPick(selectedIndex - 1) else if (selectedIndex < 0 && options.isNotEmpty()) onPick(0) },
    onRight = { if (selectedIndex in 0 until options.size - 1) onPick(selectedIndex + 1) else if (selectedIndex < 0 && options.isNotEmpty()) onPick(0) },
)

private fun toggleNavItem(label: String, checked: Boolean, onToggle: () -> Unit) = NavItem(
    render = { focused -> QaFocusRow(focused) { QaToggleRow(label, checked) { onToggle() } } },
    onActivate = onToggle,
    onLeft = onToggle,
    onRight = onToggle,
)

private fun stepperNavItem(label: String, value: String, onStep: (Int) -> Unit) = NavItem(
    render = { focused -> QaFocusRow(focused) { QaStepperRow(label, value) { onStep(it) } } },
    onLeft = { onStep(-1) },
    onRight = { onStep(1) },
)

private fun noteItem(text: String) = NavItem(render = { _ -> Text(text, style = QaLabel, modifier = Modifier.padding(start = 18.dp, end = 4.dp)) })

private fun swatchNavItem(colors: List<Int>, selected: Int, onPick: (Int) -> Unit): NavItem {
    val idx = colors.indexOfFirst { (it or 0xFF000000.toInt()) == (selected or 0xFF000000.toInt()) }.coerceAtLeast(0)
    return NavItem(
        render = { focused -> QaFocusRow(focused) { ColorSwatchRow(colors, selected, onPick) } },
        onLeft = { onPick(colors[(idx - 1).coerceAtLeast(0)]) },
        onRight = { onPick(colors[(idx + 1).coerceAtMost(colors.size - 1)]) },
    )
}

private fun sliderNavItem(label: String, percent: Int, step: Int = 5, onSet: (Int) -> Unit) = NavItem(
    render = { focused -> QaFocusRow(focused) { QaSlider(label, percent, onSet) } },
    onLeft = { onSet((percent - step).coerceIn(0, 100)) },
    onRight = { onSet((percent + step).coerceIn(0, 100)) },
)

/** Label · 12 dp track · value. Controller ←/→ in 5 % steps, tap-to-set; the value updates optimistically. */
@Composable
private fun QaSlider(label: String, percent: Int, onSet: (Int) -> Unit) {
    val onSetState = rememberUpdatedState(onSet)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = QaLabel, modifier = Modifier.width(72.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(QaColors.TrackBg)
                .pointerInput(Unit) {
                    detectTapGestures { offset -> onSetState.value((offset.x / size.width * 100f).roundToInt().coerceIn(0, 100)) }
                },
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((percent.coerceIn(0, 100)) / 100f).background(OsdInk, RoundedCornerShape(6.dp)))
        }
        Text("$percent%", style = QaNum, modifier = Modifier.width(38.dp))
    }
}

private val RGB_SWATCHES = listOf(
    0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFF34C759, 0xFF32ADE6, 0xFF5856D6, 0xFFFF2D55, 0xFFFFFFFF,
).map { it.toInt() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatchRow(colors: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { c ->
            val isSel = (c or 0xFF000000.toInt()) == (selected or 0xFF000000.toInt())
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(c))
                    .border(if (isSel) 2.dp else 1.dp, if (isSel) OsdInk else Hair, RoundedCornerShape(6.dp))
                    .clickable { onPick(c) },
            )
        }
    }
}

/**
 * Footer confirmation for an applied action, or null for controls that confirm themselves (sliders, scope).
 * A fan pick under a live AutoTDP session is labelled as deferred: it applies once the game exits.
 */
private fun flashLabel(a: QuickAccessAction, autoTdpLive: Boolean): String? = when (a) {
    QuickAccessAction.ToggleAutoTdp -> "Auto on"
    is QuickAccessAction.SetTier -> "${a.tier.label} tier"
    QuickAccessAction.SetStockMode -> "Runs stock"
    is QuickAccessAction.SetFpsTarget -> "Holding ${a.fps} fps"
    is QuickAccessAction.SetBias -> a.bias.label
    is QuickAccessAction.SetAggressivePark -> if (a.enabled) "Aggressive park on" else "Aggressive park off"
    is QuickAccessAction.SetFanMode -> {
        val label = "Fan ${FanController.labelFor(a.mode)}"
        if (QuickAccessPerApp.fanModeDeferredByAutoTdp(autoTdpLive, a.mode)) "$label — after Auto" else label
    }
    is QuickAccessAction.SetFanSmart -> if (a.enabled) "Holding target temp" else "Manual curve"
    is QuickAccessAction.SetFanTargetTemp -> "Target ${a.tempC} °C"
    is QuickAccessAction.SetFanBias -> fanBiasLabel(a.bias)
    is QuickAccessAction.SetRgbMode -> "Lights ${a.mode.label}"
    is QuickAccessAction.SetRgbColor -> "Colour set"
    is QuickAccessAction.SetOverlayEnabled -> if (a.enabled) "Overlay on" else "Overlay off"
    is QuickAccessAction.SetOverlayPreset -> "Overlay ${a.preset.label}"
    is QuickAccessAction.SetGpuCap -> "GPU cap ${a.freqKhz / 1000} MHz"
    else -> null
}
