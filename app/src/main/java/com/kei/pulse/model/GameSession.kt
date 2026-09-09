package com.kei.pulse.model

import kotlinx.serialization.Serializable

/** One per-second sample of a game session. Nulls mean "not measurable then" (no frames, or on charger). */
@Serializable
data class SessionSample(
    /** Milliseconds since the session started. */
    val t: Long,
    val frameMs: Float? = null,
    val drawW: Float? = null,
    val cpuC: Int? = null,
    val gpuC: Int? = null,
)

/**
 * A recorded game session: what the watcher saw while one game was in front. The home screen's hero is
 * built from this — live while the game runs, as a recap afterwards.
 *
 * Samples are downsampled by pair-averaging once they exceed [MAX_SAMPLES], so a long session costs the
 * same memory and disk as a short one and the trace keeps its overall shape.
 */
@Serializable
data class GameSession(
    val packageName: String,
    val label: String,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    /** Frame-rate target while the session ran (0 = no target). */
    val targetFps: Int = 0,
    val samples: List<SessionSample> = emptyList(),
) {
    val isLive: Boolean get() = endedAtMs == null
    val durationMs: Long get() = (endedAtMs ?: (samples.lastOrNull()?.t?.plus(startedAtMs) ?: startedAtMs)) - startedAtMs

    val frameTimes: List<Float> get() = samples.mapNotNull { it.frameMs }
    val draws: List<Float> get() = samples.mapNotNull { it.drawW }

    /** Share (0..1) of measurable frames at or under the target frame time (+10 % tolerance); null without a target. */
    val heldShare: Float?
        get() {
            if (targetFps <= 0) return null
            val f = frameTimes
            if (f.isEmpty()) return null
            val limit = 1000f / targetFps * 1.10f
            return f.count { it <= limit }.toFloat() / f.size
        }

    val avgDrawW: Float? get() = draws.takeIf { it.isNotEmpty() }?.average()?.toFloat()
    val peakDrawW: Float? get() = draws.maxOrNull()
    val peakTempC: Int? get() = samples.mapNotNull { maxOf(it.cpuC ?: 0, it.gpuC ?: 0).takeIf { c -> c > 0 } }.maxOrNull()
    val avgFps: Float? get() = frameTimes.takeIf { it.isNotEmpty() }?.let { 1000f / it.average().toFloat() }

    fun withSample(s: SessionSample): GameSession {
        val next = samples + s
        return copy(samples = if (next.size > MAX_SAMPLES) downsample(next) else next)
    }

    fun ended(atMs: Long): GameSession = copy(endedAtMs = atMs)

    companion object {
        const val MAX_SAMPLES = 2400

        /** Pair-average adjacent samples, halving the count; nulls average only over present values. */
        fun downsample(xs: List<SessionSample>): List<SessionSample> = xs.chunked(2).map { pair ->
            if (pair.size == 1) return@map pair[0]
            val (a, b) = pair
            fun avgF(x: Float?, y: Float?): Float? = when {
                x == null -> y
                y == null -> x
                else -> (x + y) / 2f
            }
            fun avgI(x: Int?, y: Int?): Int? = when {
                x == null -> y
                y == null -> x
                else -> (x + y) / 2
            }
            SessionSample(t = a.t, frameMs = avgF(a.frameMs, b.frameMs), drawW = avgF(a.drawW, b.drawW), cpuC = avgI(a.cpuC, b.cpuC), gpuC = avgI(a.gpuC, b.gpuC))
        }
    }
}
