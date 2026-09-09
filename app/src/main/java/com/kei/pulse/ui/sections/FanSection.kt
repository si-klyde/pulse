package com.kei.pulse.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.kei.pulse.data.FanController
import com.kei.pulse.ui.FanCurveEditor
import com.kei.pulse.ui.FanCurveEditorBindings
import com.kei.pulse.ui.fanUnderAutoTdpCaption
import com.kei.pulse.ui.shell.Note
import com.kei.pulse.ui.shell.Seg
import com.kei.pulse.ui.theme.Ink
import com.kei.pulse.ui.theme.Ink2
import com.kei.pulse.ui.theme.PulseTypography
import com.kei.pulse.ui.theme.ReadoutSmall

/**
 * Fan: the mode row with live duty on the right, the one-line truth about fan behaviour under Auto, and the
 * Custom curve editor when Custom is selected.
 */
@Composable
fun FanSection(
    currentMode: Int?,
    onSelectMode: (Int) -> Unit,
    editor: FanCurveEditorBindings?,
    autoOn: Boolean,
    liveDutyPercent: Int?,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FanController.MODES.forEach { m ->
                    if (m.value != FanController.CUSTOM || editor != null) {
                        Seg(m.label, currentMode == m.value, { onSelectMode(m.value) }, height = 44)
                    }
                }
            }
            liveDutyPercent?.let {
                Text("$it %", style = ReadoutSmall, color = Ink)
            } ?: Text(FanController.labelFor(currentMode), style = PulseTypography.bodyMedium, color = Ink2)
        }
        if (autoOn) Note(fanUnderAutoTdpCaption(currentMode, customAvailable = editor != null))
        if (currentMode == FanController.CUSTOM && editor != null) {
            FanCurveEditor(bindings = editor)
        }
    }
}
