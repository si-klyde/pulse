package com.kei.pulse.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kei.pulse.data.TelemetrySnapshot
import com.kei.pulse.model.OverlayElement
import com.kei.pulse.model.OverlayPreset
import com.kei.pulse.ui.theme.Azeret
import com.kei.pulse.ui.theme.Bricolage
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/*
 * In-game overlay. It sits on someone else's frame, so it is NOT the app's black: a smoke surface the game
 * reads through, one hairline, and colour only where it means heat or load. Dimensions are the app's
 * long-standing ones (docked strip 16/6 dp; card 300 dp, 12 dp radius, 10/8 dp padding; 9/12 sp type).
 */
internal val Smoke = Color(0xB8_16161A)      // 72 %
internal val SmokeDeep = Color(0xD1_12121A)  // 82 %
internal val Hair = Color(0x24_FFFFFF)       // 14 %
internal val OsdInk = Color(0xFFF4F2EE)
internal val OsdInk2 = Color(0xB3_FFFFFF)    // 70 %
internal val OsdInk3 = Color(0x8C_FFFFFF)    // 55 %
internal val OsdInk4 = Color(0x73_FFFFFF)    // 45 %

private val LabelStyle = TextStyle(fontFamily = Bricolage, fontSize = 9.sp, lineHeight = 10.sp, color = OsdInk3)
private val ValueStyle = TextStyle(fontFamily = Azeret, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.02).sp, fontFeatureSettings = "tnum")
private val BigStyle = TextStyle(fontFamily = Azeret, fontSize = 20.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.02).sp, fontFeatureSettings = "tnum")
private val StripStyle = TextStyle(fontFamily = Azeret, fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = (-0.02).sp, fontFeatureSettings = "tnum")
private val SmallStyle = TextStyle(fontFamily = Bricolage, fontSize = 10.sp, lineHeight = 12.sp, color = OsdInk3)

@Composable
fun OverlayContent(
    statsFlow: StateFlow<OverlayStats>,
    configFlow: StateFlow<OverlayConfig>,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onCyclePreset: () -> Unit,
    onToggleLock: () -> Unit,
) {
    val stats by statsFlow.collectAsState()
    val config by configFlow.collectAsState()
    val opacity = (config.opacityPercent / 100f).coerceIn(0.4f, 1f)

    if (config.preset == OverlayPreset.COMPACT) {
        DockedStrip(stats, config, opacity, onCyclePreset, onToggleLock)
    } else {
        FloatingCard(stats, config, opacity, onDrag, onCyclePreset, onToggleLock)
    }
}

/** Detailed/Full: the 300 dp card, draggable when unlocked (the window is WRAP_CONTENT). */
@Composable
private fun FloatingCard(
    stats: OverlayStats,
    config: OverlayConfig,
    opacity: Float,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onCyclePreset: () -> Unit,
    onToggleLock: () -> Unit,
) {
    Box(
        modifier = Modifier
            .alpha(opacity)
            .then(
                if (!config.locked) {
                    Modifier.pointerInput(config.locked) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            onDrag(drag.x, drag.y)
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Smoke)
                .border(1.dp, Hair, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .widthIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!config.locked) Controls(dragHint = true, onCyclePreset, onToggleLock)
            when (config.preset) {
                OverlayPreset.DETAILED -> DetailedCard(stats, config.elements)
                OverlayPreset.FULL -> FullCard(stats, config.elements)
                OverlayPreset.COMPACT -> Unit
            }
            if (isThrottling(stats.telemetry)) ThermalTag()
        }
    }
}

/**
 * Compact, docked: a full-width strip pinned to the top (its window is MATCH_PARENT, not draggable). Label-less
 * apart from unit hints; groups separated by hairlines. Fixed order so numbers never shift as they change.
 */
@Composable
private fun DockedStrip(
    stats: OverlayStats,
    config: OverlayConfig,
    opacity: Float,
    onCyclePreset: () -> Unit,
    onToggleLock: () -> Unit,
) {
    val t = stats.telemetry
    val els = config.elements
    Column(Modifier.fillMaxWidth().alpha(opacity).background(Smoke)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (OverlayElement.FPS in els) {
                StripValue(fpsText(stats.fps?.fps), "fps", OsdInk, big = true)
                Divider()
            }
            if (els.any(OverlayElement.GPU_LOAD, OverlayElement.GPU_TEMP)) {
                if (OverlayElement.GPU_LOAD in els) StripValue("GPU ${pct(t.gpuLoadPercent)}", "%", OsdInk2)
                if (OverlayElement.GPU_TEMP in els) StripValue(num(t.gpuTempC), "°", tempColor(t.gpuTempC))
                Divider()
            }
            if (els.any(OverlayElement.CPU_LOAD, OverlayElement.CPU_TEMP)) {
                if (OverlayElement.CPU_LOAD in els) StripValue("CPU ${pct(t.cpuLoadPercent)}", "%", OsdInk2)
                if (OverlayElement.CPU_TEMP in els) StripValue(num(t.cpuTempC), "°", tempColor(t.cpuTempC))
                Divider()
            }
            if (OverlayElement.POWER in els) StripValue(powerValue(stats), "W", OsdInk2)
            if (OverlayElement.BATTERY_LEFT in els) StripValue(leftText(stats.minutesLeft), if (stats.powerIsCharging) "charging" else "left", OsdInk2)
            if (isThrottling(t)) ThermalTag()
            Spacer(Modifier.weight(1f))
            if (!config.locked) Controls(dragHint = false, onCyclePreset, onToggleLock)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hair))
    }
}

@Composable
private fun Divider() {
    Box(Modifier.width(1.dp).height(12.dp).background(Hair))
}

@Composable
private fun StripValue(value: String, unit: String, color: Color, big: Boolean = false) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = if (big) StripStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium) else StripStyle, color = color)
        Text(unit, style = LabelStyle)
    }
}

/** Quiet text controls; only visible while unlocked. */
@Composable
private fun Controls(dragHint: Boolean, onCyclePreset: () -> Unit, onToggleLock: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (dragHint) Text("drag to move", style = SmallStyle)
        Text("layout", style = SmallStyle.copy(color = OsdInk), modifier = Modifier.clickable { onCyclePreset() })
        Text("lock", style = SmallStyle.copy(color = OsdInk), modifier = Modifier.clickable { onToggleLock() })
    }
}

@Composable
private fun ThermalTag() {
    Text(
        "thermal",
        style = SmallStyle.copy(color = OsdInk, fontWeight = FontWeight.Medium),
        modifier = Modifier.background(MeterHot).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

// True if ANY of [e] is in the enabled set, used to skip a row entirely when none of its items show.
private fun Set<OverlayElement>.any(vararg e: OverlayElement): Boolean = e.any { it in this }

@Composable
private fun DetailedCard(stats: OverlayStats, els: Set<OverlayElement>) {
    val t = stats.telemetry
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (els.any(OverlayElement.FPS, OverlayElement.FPS_TREND, OverlayElement.SESSION_TIMER)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (OverlayElement.FPS in els) Text(fpsText(stats.fps?.fps), style = BigStyle, color = OsdInk)
                if (OverlayElement.FPS_TREND in els) {
                    Text("avg ${fpsText(stats.fps?.avgFps)} · low ${fpsText(stats.fps?.onePercentLowFps)}", style = SmallStyle, modifier = Modifier.padding(bottom = 1.dp))
                }
                if (OverlayElement.SESSION_TIMER in els) {
                    Spacer(Modifier.weight(1f))
                    Text(formatTimer(stats.sessionElapsedMs), style = ValueStyle, color = OsdInk)
                }
            }
        }
        if (els.any(OverlayElement.GPU_LOAD, OverlayElement.GPU_CLOCK, OverlayElement.GPU_TEMP, OverlayElement.POWER)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (OverlayElement.GPU_LOAD in els) Metric("GPU", pct(t.gpuLoadPercent), "%", loadColor(t.gpuLoadPercent))
                if (OverlayElement.GPU_CLOCK in els) Metric("GPU clk", num(t.gpuMhz), "MHz")
                if (OverlayElement.GPU_TEMP in els) Metric("GPU °", num(t.gpuTempC), "", tempColor(t.gpuTempC))
                if (OverlayElement.POWER in els) Metric("draw", powerValue(stats), "W")
            }
        }
        if (els.any(OverlayElement.CPU_LOAD, OverlayElement.CPU_CLOCK, OverlayElement.CPU_TEMP, OverlayElement.RAM)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (OverlayElement.CPU_LOAD in els) Metric("CPU", pct(t.cpuLoadPercent), "%", loadColor(t.cpuLoadPercent))
                if (OverlayElement.CPU_CLOCK in els) Metric("CPU clk", ghz(t.cpuClocksMhz.values.maxOrNull()), "GHz")
                if (OverlayElement.CPU_TEMP in els) Metric("CPU °", num(t.cpuTempC), "", tempColor(t.cpuTempC))
                if (OverlayElement.RAM in els) Metric("RAM", pct(t.ramUsedPercent), "%")
            }
        }
        if (els.any(OverlayElement.BATTERY_LEFT, OverlayElement.AUTOTDP)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Hair))
            if (OverlayElement.BATTERY_LEFT in els) Metric(if (stats.powerIsCharging) "charging" else "left", leftText(stats.minutesLeft), "")
            if (OverlayElement.AUTOTDP in els) ProfileOrAutoTdp(stats, showClocks = false)
        }
    }
}

@Composable
private fun FullCard(stats: OverlayStats, els: Set<OverlayElement>) {
    val t = stats.telemetry
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (els.any(OverlayElement.SOC_NAME, OverlayElement.SESSION_TIMER)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                if (OverlayElement.SOC_NAME in els) Text(stats.socModel ?: "PULSE", style = SmallStyle.copy(color = OsdInk2))
                Spacer(Modifier.weight(1f))
                if (OverlayElement.SESSION_TIMER in els) Text(formatTimer(stats.sessionElapsedMs), style = SmallStyle)
            }
        }
        if (els.any(OverlayElement.FPS, OverlayElement.FPS_TREND)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (OverlayElement.FPS in els) Text(fpsText(stats.fps?.fps), style = BigStyle, color = OsdInk)
                if (OverlayElement.FPS_TREND in els) {
                    Column {
                        Text("avg ${fpsText(stats.fps?.avgFps)} · low ${fpsText(stats.fps?.onePercentLowFps)}", style = SmallStyle)
                        Text("${fmt1(stats.fps?.frameTimeMs)} ms", style = SmallStyle)
                    }
                    Spacer(Modifier.weight(1f))
                    stats.fps?.recentFps?.let { Sparkline(it, OsdInk, Modifier.size(width = 64.dp, height = 22.dp)) }
                }
            }
        }
        if (els.any(OverlayElement.GPU_LOAD, OverlayElement.GPU_CLOCK, OverlayElement.GPU_TEMP, OverlayElement.POWER)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (OverlayElement.GPU_LOAD in els) Metric("GPU", pct(t.gpuLoadPercent), "%", loadColor(t.gpuLoadPercent))
                if (OverlayElement.GPU_CLOCK in els) Metric("GPU clk", num(t.gpuMhz), "MHz")
                if (OverlayElement.GPU_TEMP in els) Metric("GPU °", num(t.gpuTempC), "", tempColor(t.gpuTempC))
                if (OverlayElement.POWER in els) Metric("draw", powerValue(stats), "W")
            }
        }
        if (els.any(OverlayElement.CPU_LOAD, OverlayElement.CPU_CLOCK, OverlayElement.CPU_TEMP, OverlayElement.CORE_BARS)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (OverlayElement.CPU_LOAD in els) Metric("CPU", pct(t.cpuLoadPercent), "%", loadColor(t.cpuLoadPercent))
                if (OverlayElement.CPU_CLOCK in els) Metric("CPU clk", ghz(t.cpuClocksMhz.values.maxOrNull()), "GHz")
                if (OverlayElement.CPU_TEMP in els) Metric("CPU °", num(t.cpuTempC), "", tempColor(t.cpuTempC))
                if (OverlayElement.CORE_BARS in els && t.cpuCoreLoadsPercent.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))
                    CoreBars(t.cpuCoreLoadsPercent, OsdInk, Modifier.size(width = 60.dp, height = 22.dp))
                }
            }
        }
        if (els.any(OverlayElement.RAM, OverlayElement.BATTERY_LEFT, OverlayElement.AUTOTDP)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Hair))
            if (els.any(OverlayElement.RAM, OverlayElement.BATTERY_LEFT)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
                    if (OverlayElement.RAM in els) {
                        Metric(
                            "RAM",
                            if (t.ramUsedMb != null && t.ramTotalMb != null) "${t.ramUsedMb / 1024}.${(t.ramUsedMb % 1024) * 10 / 1024}/${t.ramTotalMb / 1024}" else "—",
                            "GB",
                        )
                    }
                    if (OverlayElement.BATTERY_LEFT in els) Metric(if (stats.powerIsCharging) "charging" else "left", leftText(stats.minutesLeft), "")
                }
            }
            if (OverlayElement.AUTOTDP in els) ProfileOrAutoTdp(stats, showClocks = true)
        }
    }
}

/** AutoTDP live readout when a session is active, else the profile/tier banner. */
@Composable
private fun ProfileOrAutoTdp(stats: OverlayStats, showClocks: Boolean) {
    val auto = stats.autoTdp
    when {
        auto != null -> AutoTdpRow(auto, showClocks)
        stats.profileLabel.isNotBlank() -> Metric("mode", stats.profileLabel, "")
    }
}

@Composable
private fun AutoTdpRow(a: AutoTdpReadout, showClocks: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            buildString {
                append("Auto · ${if (a.targetFps <= 0) "max" else "${a.targetFps} fps"}")
                if (a.primeParked) append(" · parked")
                append(if (a.learned) " · learned" else " · learning ${a.learningPercent} %")
            },
            style = LabelStyle,
            softWrap = false,
            maxLines = 1,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            Text("caps", style = LabelStyle, softWrap = false)
            Text(a.cpuClusters.joinToString(" ") { "${it.capPercent}" }, style = ValueStyle, color = capColor(a.cpuClusters.minOfOrNull { it.capPercent } ?: 100), softWrap = false, maxLines = 1)
            a.gpuCapPercent?.let { Text("gpu $it", style = ValueStyle, color = capColor(it), softWrap = false, maxLines = 1) }
        }
        if (showClocks) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                Text("clk", style = LabelStyle, softWrap = false)
                Text(a.cpuClusters.joinToString(" / ") { ghz(it.mhz) } + (a.gpuMhz?.let { " · gpu ${ghz(it)}" } ?: ""), style = ValueStyle, color = OsdInk2, softWrap = false, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, unit: String, color: Color = OsdInk) {
    Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = LabelStyle)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = ValueStyle, color = color)
            if (unit.isNotEmpty()) Text(unit, style = LabelStyle)
        }
    }
}

@Composable
private fun CoreBars(values: List<Int>, color: Color, modifier: Modifier) {
    val track = Color(0x1F_FFFFFF)
    Canvas(modifier) {
        val n = values.size
        if (n == 0) return@Canvas
        val gap = 2f
        val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        values.forEachIndexed { i, v ->
            val x = i * (barW + gap)
            drawRect(color = track, topLeft = Offset(x, 0f), size = Size(barW, size.height))
            val h = size.height * (v / 100f).coerceIn(0f, 1f)
            drawRect(color = color, topLeft = Offset(x, size.height - h), size = Size(barW, h))
        }
    }
}

@Composable
private fun Sparkline(values: List<Float>, color: Color, modifier: Modifier) {
    if (values.size < 2) return
    val min = values.minOrNull() ?: return
    val max = values.maxOrNull() ?: return
    val range = (max - min).coerceAtLeast(15f)
    Canvas(modifier) {
        val stepX = size.width / (values.size - 1)
        var prev: Offset? = null
        values.forEachIndexed { i, v ->
            val p = Offset(i * stepX, size.height - ((v - min) / range) * size.height)
            prev?.let { drawLine(color, it, p, strokeWidth = 1.5f) }
            prev = p
        }
    }
}

// ── formatting / colour helpers ──────────────────────────────────────────────

private fun num(v: Int?): String = v?.toString() ?: "—"
private fun pct(v: Int?): String = v?.toString() ?: "—"
private fun fmt1(v: Float?): String = v?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—"
private fun ghz(mhz: Int?): String = mhz?.let { String.format(java.util.Locale.US, "%.2f", it / 1000f) } ?: "—"
private fun fpsText(fps: Float?): String = fps?.takeIf { it > 0f }?.roundToInt()?.toString() ?: "—"

/** "2h14m" / "47m", or "—" when not estimable (charging, idle draw, unknown capacity). */
private fun leftText(minutes: Int?): String {
    val m = minutes ?: return "—"
    val h = m / 60
    val mm = m % 60
    return if (h > 0) "${h}h${mm}m" else "${mm}m"
}

/** "3.4" (system draw on battery) or "⚡18.5" (charge rate while plugged in); "—" when unknown. */
private fun powerValue(stats: OverlayStats): String {
    val v = stats.powerDrawW?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: return "—"
    return if (stats.powerIsCharging) "⚡$v" else v
}

// Mirrors AutoTuneController.THERMAL_C (the hard thermal trip).
private const val THROTTLE_TEMP_C = 85
private fun isThrottling(t: TelemetrySnapshot): Boolean =
    (t.cpuTempC ?: 0) >= THROTTLE_TEMP_C || (t.gpuTempC ?: 0) >= THROTTLE_TEMP_C

private fun tempColor(c: Int?): Color = meterTempColor(c)
private fun loadColor(p: Int?): Color = meterLoadColor(p)

// AutoTDP caps: a trimmed domain (below 100 %) is the savings, sage; full clocks stay ink.
private fun capColor(percent: Int): Color = if (percent >= 100) OsdInk else MeterCool

private fun formatTimer(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, sec) else String.format(java.util.Locale.US, "%d:%02d", m, sec)
}
