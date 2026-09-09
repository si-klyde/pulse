package com.kei.pulse.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kei.pulse.data.TelemetrySnapshot
import com.kei.pulse.model.CpuPolicyInfo
import com.kei.pulse.overlay.MeterHot
import com.kei.pulse.ui.theme.Housing
import com.kei.pulse.ui.theme.Ink
import com.kei.pulse.ui.theme.Ink2
import com.kei.pulse.ui.theme.Ink3
import com.kei.pulse.ui.theme.Ink4
import com.kei.pulse.ui.theme.OnInk
import com.kei.pulse.ui.theme.PulseTypography
import com.kei.pulse.ui.theme.ReadoutLarge
import com.kei.pulse.ui.theme.Rule

// The RP6 renders at ~831×467 dp (1080p at density 2.3), so the fixed columns are sized for that, not 960×540.
private val RailWidth = 148.dp
private val LiveWidth = 204.dp
private val HeaderHeight = 100.dp

/**
 * The fixed RP6 shell: header (title + status, hero trace, live fps/ms/W), a six-item rail, the section
 * slot, and the always-visible live column. Nothing here scrolls; sections manage their own overflow.
 */
@Composable
fun RailShell(
    section: Section,
    onSelectSection: (Section) -> Unit,
    statusLine1: String,
    statusLine2: String,
    frameTimesMs: List<Float>,
    drawWatts: List<Float>,
    targetFps: Int,
    currentFps: Float?,
    currentDrawW: Float?,
    telemetry: TelemetrySnapshot,
    policies: List<CpuPolicyInfo>,
    fanPercent: Int?,
    batteryTimeLeft: String?,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Housing).statusBarsPadding()) {
        Header(statusLine1, statusLine2, frameTimesMs, drawWatts, targetFps, currentFps, currentDrawW)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))
        Row(Modifier.fillMaxSize()) {
            Rail(section, onSelectSection)
            Box(Modifier.width(1.dp).fillMaxHeight().background(Rule))
            Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            Box(Modifier.width(1.dp).fillMaxHeight().background(Rule))
            LiveColumn(telemetry, policies, fanPercent, batteryTimeLeft, Modifier.width(LiveWidth))
        }
    }
}

@Composable
private fun Header(
    line1: String,
    line2: String,
    frameTimesMs: List<Float>,
    drawWatts: List<Float>,
    targetFps: Int,
    currentFps: Float?,
    currentDrawW: Float?,
) {
    Row(Modifier.fillMaxWidth().height(HeaderHeight)) {
        Column(
            Modifier.width(RailWidth).fillMaxHeight().padding(start = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("PULSE", style = PulseTypography.headlineSmall, color = Ink)
            Spacer(Modifier.height(3.dp))
            Text(line1, style = PulseTypography.labelMedium, color = Ink3)
            Text(line2, style = PulseTypography.labelMedium, color = Ink3)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Rule))
        Box(Modifier.weight(1f).fillMaxHeight()) {
            val targetMs = if (targetFps > 0) 1000f / targetFps else 16.7f
            HeroTrace(frameTimesMs, drawWatts, targetMs, Modifier.fillMaxSize().padding(top = 4.dp))
            Row(
                Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Frame time · last 60 s", style = PulseTypography.labelSmall, color = Ink3)
                Text("grey · power draw", style = PulseTypography.labelSmall, color = Ink4)
            }
            Row(
                Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                val ms = currentFps?.takeIf { it > 0f }?.let { 1000f / it }
                BigReadout(currentFps?.let { String.format(java.util.Locale.US, "%.0f", it) } ?: "—", "fps")
                BigReadout(ms?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—", "ms")
                BigReadout(currentDrawW?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—", "W")
            }
            Row(
                Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("— ${String.format(java.util.Locale.US, "%.1f", targetMs)} ms target", style = PulseTypography.labelSmall, color = Ink4)
                Text("·· ${String.format(java.util.Locale.US, "%.0f", targetMs * 2)} ms", style = PulseTypography.labelSmall, color = Ink4)
                Text("▎jank", style = PulseTypography.labelSmall, color = MeterHot)
            }
        }
    }
}

@Composable
private fun BigReadout(value: String, unit: String) {
    Text(
        buildAnnotatedString { append(value); withStyle(PulseTypography.labelMedium.toSpanStyle().copy(color = Ink3)) { append(" $unit") } },
        style = ReadoutLarge,
        color = Ink,
    )
}

@Composable
private fun Rail(section: Section, onSelect: (Section) -> Unit) {
    Column(Modifier.width(RailWidth).fillMaxHeight().padding(top = 8.dp, bottom = 8.dp)) {
        Section.primary.forEach { RailItem(it, it == section) { onSelect(it) } }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))
        Spacer(Modifier.height(8.dp))
        RailItem(Section.SYSTEM, section == Section.SYSTEM) { onSelect(Section.SYSTEM) }
    }
}

@Composable
private fun RailItem(section: Section, active: Boolean, onClick: () -> Unit) {
    val fg = if (active) OnInk else Ink2
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(if (active) Ink else Housing)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(section.icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(section.label, style = if (active) PulseTypography.titleMedium else PulseTypography.bodyLarge, color = fg)
    }
}
