package com.kei.pulse.ui.shell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
 * The one bold element on the home screen: a session's frame time (ink) over its power draw (faint), the
 * frame-time target as a band, a dashed 2× reference, and a red tick on the baseline for every sample over
 * `jankMs`. Nulls are gaps (no frames, or on charger) and break the line rather than drawing zero.
 * Oldest sample left, newest touching the right edge.
 */
@Composable
fun HeroTrace(
    frameTimesMs: List<Float?>,
    drawWatts: List<Float?>,
    targetMs: Float,
    modifier: Modifier = Modifier,
    maxMs: Float = targetMs * 2.4f,
    maxWatts: Float = 20f,
    jankMs: Float = targetMs * 1.5f,
    live: Boolean = false,
) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val top = 10f
            val bottom = h - 8f
            fun yMs(ms: Float) = bottom - ((ms / maxMs).coerceIn(0f, 1f)) * (bottom - top)
            fun yW(watts: Float) = bottom - ((watts / maxWatts).coerceIn(0f, 1f)) * (bottom - top)

            drawRect(Raised, topLeft = Offset(0f, yMs(targetMs) - 3f), size = Size(w, 6f))
            drawLine(Rule2, Offset(0f, yMs(targetMs)), Offset(w, yMs(targetMs)), strokeWidth = 1f)
            drawLine(
                Rule, Offset(0f, yMs(targetMs * 2f)), Offset(w, yMs(targetMs * 2f)), strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f)),
            )

            fun xAt(i: Int, n: Int) = if (n <= 1) w else i * w / (n - 1)

            fun polyline(values: List<Float?>, y: (Float) -> Float): Path {
                val p = Path()
                var pen = false
                values.forEachIndexed { i, v ->
                    if (v == null) { pen = false; return@forEachIndexed }
                    val x = xAt(i, values.size)
                    if (pen) p.lineTo(x, y(v)) else p.moveTo(x, y(v))
                    pen = true
                }
                return p
            }

            if (drawWatts.size >= 2) drawPath(polyline(drawWatts, ::yW), Ink4, style = Stroke(width = 1f))

            if (frameTimesMs.size >= 2) {
                // Area fill only under measured stretches.
                val area = Path()
                var runStart = -1
                fun closeRun(end: Int) {
                    if (runStart < 0) return
                    area.moveTo(xAt(runStart, frameTimesMs.size), bottom)
                    for (i in runStart..end) area.lineTo(xAt(i, frameTimesMs.size), yMs(frameTimesMs[i]!!))
                    area.lineTo(xAt(end, frameTimesMs.size), bottom)
                    area.close()
                    runStart = -1
                }
                frameTimesMs.forEachIndexed { i, v -> if (v == null) closeRun(i - 1) else if (runStart < 0) runStart = i }
                closeRun(frameTimesMs.lastIndex)
                drawPath(area, Ink.copy(alpha = 0.07f))
                drawPath(polyline(frameTimesMs, ::yMs), Ink, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
                val last = frameTimesMs.lastOrNull { it != null }
                if (live && last != null) drawCircle(Ink, radius = 3f, center = Offset(w, yMs(last)))
                frameTimesMs.forEachIndexed { i, v ->
                    if (v != null && v > jankMs) {
                        val x = xAt(i, frameTimesMs.size)
                        drawLine(MeterHot, Offset(x, h - 6f), Offset(x, h), strokeWidth = 2f)
                    }
                }
            }
        }
    }
}
