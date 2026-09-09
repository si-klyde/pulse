package com.kei.pulse.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kei.pulse.data.TelemetrySnapshot
import com.kei.pulse.model.CpuPolicyInfo
import com.kei.pulse.overlay.MeterWarm
import com.kei.pulse.overlay.meterTempColor
import com.kei.pulse.ui.theme.Ink
import com.kei.pulse.ui.theme.Ink3
import com.kei.pulse.ui.theme.Ink4
import com.kei.pulse.ui.theme.PulseTypography
import com.kei.pulse.ui.theme.ReadoutHero
import com.kei.pulse.ui.theme.ReadoutMedium
import com.kei.pulse.ui.theme.Rule
import com.kei.pulse.ui.theme.Rule2

/** Always-visible telemetry: clocks as current-over-ceiling, temps as gauges with 70°/90° ticks, battery. */
@Composable
fun LiveColumn(
    telemetry: TelemetrySnapshot,
    policies: List<CpuPolicyInfo>,
    fanPercent: Int?,
    batteryTimeLeft: String?,
    modifier: Modifier = Modifier,
) {
    val cpuPolicies = policies.filterNot { it.isGpu }.sortedBy { it.id }
    val cpuCeilingMhz = cpuPolicies.maxOfOrNull { it.selectableMaxFreq }?.div(1000)
    val cpuNowMhz = telemetry.cpuClocksMhz.values.maxOrNull()
    val gpuCeilingMhz = telemetry.gpuCeilingMhz ?: policies.firstOrNull { it.isGpu }?.selectableMaxFreq?.div(1000)

    Column(
        modifier = modifier.fillMaxHeight().padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("CPU" + (cpuCeilingMhz?.let { " · of ${ghz(it)} GHz ceiling" } ?: ""), style = PulseTypography.labelMedium, color = Ink3)
            Readout(cpuNowMhz?.let { ghz(it) } ?: "—", "GHz")
            // One segment per cluster, weighted by core count, lit when that cluster is running.
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                cpuPolicies.forEach { p ->
                    val ceiling = p.selectableMaxFreq / 1000
                    val now = telemetry.cpuClocksMhz[p.id] ?: 0
                    val frac = if (ceiling > 0) (now.toFloat() / ceiling).coerceIn(0f, 1f) else 0f
                    Box(Modifier.weight(p.cpuIds.size.toFloat()).height(4.dp).background(Rule2)) {
                        Box(Modifier.fillMaxWidth(frac).height(4.dp).background(Ink))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                cpuPolicies.forEach { p -> Text("${clusterShort(p, cpuPolicies)} ×${p.cpuIds.size}", style = PulseTypography.labelSmall, color = Ink4) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("GPU" + (gpuCeilingMhz?.let { " · of $it MHz ceiling" } ?: ""), style = PulseTypography.labelMedium, color = Ink3)
            Readout(telemetry.gpuMhz?.toString() ?: "—", "MHz")
            val frac = if (gpuCeilingMhz != null && gpuCeilingMhz > 0 && telemetry.gpuMhz != null) (telemetry.gpuMhz.toFloat() / gpuCeilingMhz).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(4.dp).background(Rule2)) { Box(Modifier.fillMaxWidth(frac).height(4.dp).background(Ink)) }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))
            Gauge("CPU temp", telemetry.cpuTempC?.toString() ?: "—", "°C", (telemetry.cpuTempC ?: 0) / 100f, meterTempColor(telemetry.cpuTempC), ticks = listOf(0.7f, 0.9f))
            Gauge("GPU temp", telemetry.gpuTempC?.toString() ?: "—", "°C", (telemetry.gpuTempC ?: 0) / 100f, meterTempColor(telemetry.gpuTempC), ticks = listOf(0.7f, 0.9f))
            Gauge("Fan", fanPercent?.toString() ?: "—", "%", (fanPercent ?: 0) / 100f, Ink)
        }

        Spacer(Modifier.weight(1f))
        val pct = telemetry.batteryPercent
        Gauge(
            label = "Battery" + (batteryTimeLeft?.let { " · $it left" } ?: ""),
            value = pct?.toString() ?: "—",
            unit = "%",
            fraction = (pct ?: 0) / 100f,
            color = if (pct != null && pct <= 20) MeterWarm else Ink,
        )
    }
}

@Composable
private fun Readout(value: String, unit: String) {
    Text(
        buildAnnotatedString {
            append(value)
            withStyle(PulseTypography.labelMedium.toSpanStyle().copy(color = Ink3)) { append("  $unit") }
        },
        style = ReadoutHero,
        color = Ink,
    )
}

@Composable
private fun Gauge(label: String, value: String, unit: String, fraction: Float, color: Color, ticks: List<Float> = emptyList()) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(label, style = PulseTypography.labelMedium, color = Ink3)
            Text(
                buildAnnotatedString { append(value); withStyle(PulseTypography.labelSmall.toSpanStyle().copy(color = Ink3)) { append(" $unit") } },
                style = ReadoutMedium,
                color = color,
            )
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Rule2)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(2.dp).background(color))
            ticks.forEach { t ->
                Box(Modifier.fillMaxWidth(t)) { Box(Modifier.align(Alignment.CenterEnd).width(1.dp).height(8.dp).background(Ink4).padding(top = 0.dp)) }
            }
        }
    }
}

private fun ghz(mhz: Int): String = String.format(java.util.Locale.US, "%.2f", mhz / 1000f)

private fun clusterShort(p: CpuPolicyInfo, all: List<CpuPolicyInfo>): String {
    val sorted = all.sortedBy { it.selectableMaxFreq }
    return when {
        sorted.size >= 3 && p.id == sorted.last().id -> "prime"
        sorted.size >= 3 && p.id == sorted.first().id -> "eff"
        sorted.size >= 3 -> "perf"
        p.id == sorted.last().id -> "big"
        else -> "little"
    }
}
