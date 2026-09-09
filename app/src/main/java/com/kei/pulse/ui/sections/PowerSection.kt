package com.kei.pulse.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kei.pulse.data.AutoTuneController
import com.kei.pulse.model.AutoTdpBias
import com.kei.pulse.ui.shell.FactsRow
import com.kei.pulse.ui.shell.HairRow
import com.kei.pulse.ui.shell.InkToggle
import com.kei.pulse.ui.shell.Note
import com.kei.pulse.ui.shell.OptionCard
import com.kei.pulse.ui.shell.Seg
import com.kei.pulse.ui.shell.SegRow
import com.kei.pulse.ui.theme.Ink2
import com.kei.pulse.ui.theme.PulseTypography

/**
 * Power: one section with an Auto | Manual switch at the top. Auto (the default most people run) shows the
 * frame rate to hold and the lean; Manual reveals the tier and clock controls through [manualContent].
 */
@Composable
fun PowerSection(
    autoOn: Boolean,
    onAutoChange: (Boolean) -> Unit,
    fpsTarget: Int,
    fpsOptions: List<Int>,
    onFpsTargetChange: (Int) -> Unit,
    bias: AutoTdpBias,
    onBiasChange: (AutoTdpBias) -> Unit,
    aggressivePark: Boolean,
    onAggressiveParkChange: (Boolean) -> Unit,
    showWattCaps: Boolean,
    displaySummary: String,
    fanSummary: String,
    perGameCount: Int,
    refreshRate: Int?,
    refreshRates: List<Int>,
    onSelectRefreshRate: (Int) -> Unit,
    compatible: Boolean,
    manualContent: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!compatible) Note("This device does not expose the PServer service PULSE needs. Nothing here can be applied.")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.weight(1f, fill = false), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Seg("Auto", autoOn, { onAutoChange(true) }, height = 40)
                Seg("Manual", !autoOn, { onAutoChange(false) }, height = 40)
            }
            Text(
                if (autoOn) "Tunes clocks live to hold your frame rate at the least power" else "Fixed ceilings you set",
                style = PulseTypography.bodyMedium,
                color = Ink2,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
        }

        if (autoOn) {
            SegRow("Frame rate to hold", height = 44) {
                fpsOptions.forEach { fps -> Seg("$fps", fpsTarget == fps, { onFpsTargetChange(fps) }, mono = true) }
            }
            SegRow("Lean towards", height = 56) {
                AutoTdpBias.entries.forEach { b ->
                    val cap = if (showWattCaps) " · caps ${watt(AutoTuneController.powerCeilingW(b))} W" else ""
                    OptionCard(
                        title = b.label,
                        caption = when (b) {
                            AutoTdpBias.EFFICIENT -> "Cool and quiet$cap"
                            AutoTdpBias.BALANCED -> "Middle$cap"
                            AutoTdpBias.SMOOTH -> "Higher clocks$cap"
                        },
                        selected = bias == b,
                        onClick = { onBiasChange(b) },
                    )
                }
            }
            SegRow("Refresh rate", height = 40) {
                refreshRates.forEach { hz -> Seg("$hz Hz", refreshRate == hz, { onSelectRefreshRate(hz) }) }
            }
            HairRow("Aggressive park", "Sleep idle prime cores harder · saves power, may stutter", height = 48) {
                InkToggle(aggressivePark, onAggressiveParkChange)
            }
            Spacer(Modifier.weight(1f))
            FactsRow(
                "Display" to displaySummary,
                "Fan" to fanSummary,
                "" to if (perGameCount == 1) "1 game overrides this" else "$perGameCount games override this",
            )
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                manualContent()
            }
        }
    }
}

private fun watt(w: Float): String = String.format(java.util.Locale.US, "%.1f", w).removeSuffix(".0")
