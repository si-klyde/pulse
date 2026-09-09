package com.kei.pulse.ui.shell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kei.pulse.overlay.MeterHot
import com.kei.pulse.ui.theme.Ink
import com.kei.pulse.ui.theme.Ink4
import com.kei.pulse.ui.theme.Raised
import com.kei.pulse.ui.theme.Rule
import com.kei.pulse.ui.theme.Rule2

/**
 * The one bold element on the home screen: 60 s of frame time (ink) over power draw (faint), with the
 * frame-time target as a band, a dashed 2× reference, and a red tick on the baseline for every sample
 * that exceeded `jankMs`. Oldest sample on the left, newest touching the right edge.
 *
 * Pure drawing, no allocation per frame beyond two Paths; callers own the sampling cadence.
 */
@Composable
fun HeroTrace(
    frameTimesMs: List<Float>,
    drawWatts: List<Float>,
    targetMs: Float,
    modifier: Modifier = Modifier,
    maxMs: Float = targetMs * 2.4f,
    maxWatts: Float = 20f,
    jankMs: Float = targetMs * 1.5f,
) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val top = 10f
            val bottom = h - 8f
            fun yMs(ms: Float) = bottom - ((ms / maxMs).coerceIn(0f, 1f)) * (bottom - top)
            fun yW(watts: Float) = bottom - ((watts / maxWatts).coerceIn(0f, 1f)) * (bottom - top)

            // Target band + 2× reference.
            drawRect(Raised, topLeft = Offset(0f, yMs(targetMs) - 3f), size = androidx.compose.ui.geometry.Size(w, 6f))
            drawLine(Rule2, Offset(0f, yMs(targetMs)), Offset(w, yMs(targetMs)), strokeWidth = 1f)
            drawLine(
                Rule, Offset(0f, yMs(targetMs * 2f)), Offset(w, yMs(targetMs * 2f)), strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f)),
            )

            fun xAt(i: Int, n: Int) = if (n <= 1) w else i * w / (n - 1)

            if (drawWatts.size >= 2) {
                val p = Path()
                drawWatts.forEachIndexed { i, v -> val x = xAt(i, drawWatts.size); if (i == 0) p.moveTo(x, yW(v)) else p.lineTo(x, yW(v)) }
                drawPath(p, Ink4, style = Stroke(width = 1f))
            }

            if (frameTimesMs.size >= 2) {
                val line = Path()
                val area = Path()
                frameTimesMs.forEachIndexed { i, v ->
                    val x = xAt(i, frameTimesMs.size)
                    val y = yMs(v)
                    if (i == 0) { line.moveTo(x, y); area.moveTo(x, bottom); area.lineTo(x, y) } else { line.lineTo(x, y); area.lineTo(x, y) }
                }
                area.lineTo(w, bottom)
                area.close()
                drawPath(area, Ink.copy(alpha = 0.07f))
                drawPath(line, Ink, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
                drawCircle(Ink, radius = 3f, center = Offset(w, yMs(frameTimesMs.last())))
                frameTimesMs.forEachIndexed { i, v ->
                    if (v > jankMs) {
                        val x = xAt(i, frameTimesMs.size)
                        drawLine(MeterHot, Offset(x, h - 6f), Offset(x, h), strokeWidth = 2f)
                    }
                }
            }
        }
    }
}
