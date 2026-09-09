package com.kei.pulse.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kei.pulse.ui.theme.Ink
import com.kei.pulse.ui.theme.Ink2
import com.kei.pulse.ui.theme.Ink3
import com.kei.pulse.ui.theme.Ink4
import com.kei.pulse.ui.theme.OnInk
import com.kei.pulse.ui.theme.PulseTypography
import com.kei.pulse.ui.theme.ReadoutSmall
import com.kei.pulse.ui.theme.Rule
import com.kei.pulse.ui.theme.Rule2

/*
 * The design's control vocabulary. Every control is a rectangle: unselected = hairline outline, selected =
 * inverted ink fill. No radius, no tint, no elevation. Hit targets are 40–48 dp.
 */

/** One option in a segmented row; the row's children share width equally. */
@Composable
fun RowScope.Seg(label: String, selected: Boolean, onClick: () -> Unit, height: Int = 44, mono: Boolean = false) {
    Box(
        Modifier
            .weight(1f)
            .height(height.dp)
            .then(if (selected) Modifier.background(Ink) else Modifier.border(1.dp, Rule2))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = when {
                mono -> ReadoutSmall
                selected -> PulseTypography.titleMedium
                else -> PulseTypography.bodyLarge
            },
            color = if (selected) OnInk else Ink2,
            maxLines = 1,
        )
    }
}

/** A segmented row: label on the left (fixed 128 dp), equal-width options to the right. */
@Composable
fun SegRow(label: String?, modifier: Modifier = Modifier, height: Int = 44, options: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (label != null) Text(label, style = PulseTypography.bodyMedium, color = Ink2, modifier = Modifier.width(112.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) { options() }
    }
}

/** A selectable card with a title and a caption — the tier / lean pattern. */
@Composable
fun RowScope.OptionCard(title: String, caption: String, selected: Boolean, onClick: () -> Unit, height: Int = 56) {
    Column(
        Modifier
            .weight(1f)
            .height(height.dp)
            .then(if (selected) Modifier.background(Ink) else Modifier.border(1.dp, Rule2))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = if (selected) PulseTypography.titleMedium else PulseTypography.bodyLarge, color = if (selected) OnInk else Ink, maxLines = 1)
        Text(caption, style = PulseTypography.labelSmall, color = if (selected) Ink4 else Ink3, maxLines = 2)
    }
}

/** A hairline-separated row: title + optional caption on the left, a trailing control on the right. */
@Composable
fun HairRow(title: String, caption: String? = null, height: Int = 52, trailing: @Composable () -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))
        Row(Modifier.fillMaxWidth().height(height.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = PulseTypography.bodyLarge, color = Ink, maxLines = 1)
                if (caption != null) Text(caption, style = PulseTypography.labelSmall, color = Ink3, maxLines = 1)
            }
            trailing()
        }
    }
}

/** 44×24 square toggle. On = ink block with a black knob on the right. */
@Composable
fun InkToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(width = 44.dp, height = 24.dp)
            .then(if (checked) Modifier.background(Ink) else Modifier.border(1.dp, Ink4))
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .size(if (checked) 18.dp else 16.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .background(if (checked) OnInk else Ink4),
        )
    }
}

/** Small caption line under a group of controls. */
@Composable
fun Note(text: String) {
    Text(text, style = PulseTypography.bodySmall, color = Ink3)
}

/** Section footer: a hairline, then key · value facts spread across the width. */
@Composable
fun FactsRow(vararg facts: Pair<String, String>) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            facts.forEach { (k, v) ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(k, style = PulseTypography.bodyMedium, color = Ink2)
                    Text(v, style = PulseTypography.bodyMedium, color = Ink3)
                }
            }
        }
    }
}
