package com.kei.pulse.overlay

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kei.pulse.ui.theme.Azeret
import com.kei.pulse.ui.theme.Bricolage

/*
 * Quick Access control vocabulary. Same smoke surface as the OSD; 34 dp pills with an 8 dp radius (this is a
 * floating panel over a game, where a little radius reads better than the app's square); unselected = hairline,
 * selected = inverted ink. Colour only for temperature in the live strip.
 */
internal object QaColors {
    val Surface = SmokeDeep
    val Text = OsdInk
    val Muted = OsdInk3
    val Outline = Hair
    val TrackBg = Color(0x1F_FFFFFF)
    val FocusFill = Color(0x14_FFFFFF)
}

internal val QaLabel = TextStyle(fontFamily = Bricolage, fontSize = 11.sp, lineHeight = 14.sp, color = OsdInk3)
internal val QaBody = TextStyle(fontFamily = Bricolage, fontSize = 13.sp, lineHeight = 16.sp, color = OsdInk)
internal val QaTitle = TextStyle(fontFamily = Bricolage, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = OsdInk)
internal val QaNum = TextStyle(fontFamily = Azeret, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = (-0.02).sp, fontFeatureSettings = "tnum", color = OsdInk)

/** True when the OS animator scale is 0 (Developer Options "Animations off" / reduced motion), read once. */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember {
        runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }
            .getOrDefault(1f) == 0f
    }
}

/**
 * Controller cursor treatment: a faint fill and a 2 dp ink bar on the left edge when [focused], and the row
 * pulls itself into view. Touch never sees this; it's the D-pad's caret.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun QaFocusRow(focused: Boolean, content: @Composable () -> Unit) {
    val bring = remember { BringIntoViewRequester() }
    LaunchedEffect(focused) { if (focused) runCatching { bring.bringIntoView() } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bring)
            .background(if (focused) QaColors.FocusFill else Color.Transparent)
            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Box(Modifier.width(2.dp).height(18.dp).background(if (focused) OsdInk else Color.Transparent))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}

/** One option in a vertical list: label + tagline; selected = ink text with a filled square marker. */
@Composable
internal fun QaModeRow(label: String, tagline: String, selected: Boolean, onSelect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onSelect() }) {
        Box(Modifier.size(12.dp).then(if (selected) Modifier.background(OsdInk) else Modifier.border(1.dp, Hair)))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = QaBody.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) OsdInk else OsdInk2))
            if (tagline.isNotBlank()) Text(tagline, style = QaLabel)
        }
    }
}

/** A labelled segmented row of 34 dp pills that share the width; unselected hairline, selected inverted. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QaSegmentedRow(label: String?, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, mono: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, style = QaLabel)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { i, text ->
                val sel = i == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) OsdInk else Color.Transparent)
                        .then(if (sel) Modifier else Modifier.border(1.dp, Hair, RoundedCornerShape(8.dp)))
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text,
                        style = (if (mono) QaNum else QaBody).copy(
                            color = if (sel) Color.Black else OsdInk2,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** A label + square ink toggle. The whole row taps; the controller path activates via [onToggle]. */
@Composable
internal fun QaToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onToggle() }) {
        Text(label, style = QaBody, modifier = Modifier.weight(1f))
        QaToggle(checked)
    }
}

@Composable
private fun QaToggle(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(if (checked) Modifier.background(OsdInk) else Modifier.border(1.dp, Hair, RoundedCornerShape(10.dp)))
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(7.dp)).background(if (checked) Color.Black else OsdInk3))
    }
}

/** A label + −/value/+ stepper. Controller steps via ←/→; the buttons handle touch. */
@Composable
internal fun QaStepperRow(label: String, value: String, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = QaBody, modifier = Modifier.weight(1f))
        QaStepButton("−") { onStep(-1) }
        Text(value, style = QaNum, modifier = Modifier.padding(horizontal = 10.dp))
        QaStepButton("+") { onStep(1) }
    }
}

@Composable
private fun QaStepButton(glyph: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Hair, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = QaBody.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
    }
}

/** Section header inside the single column: hairline above, quiet title left, optional status right. */
@Composable
internal fun QaGroupHeader(title: String, status: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hair))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
            Text(title, style = QaLabel, modifier = Modifier.weight(1f))
            if (status != null) Text(status, style = QaLabel)
        }
    }
}
