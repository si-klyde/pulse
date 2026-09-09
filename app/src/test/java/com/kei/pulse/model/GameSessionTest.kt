package com.kei.pulse.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {

    private fun session(target: Int, frames: List<Float?>): GameSession =
        GameSession("pkg", "Game", startedAtMs = 1_000L, targetFps = target, samples = frames.mapIndexed { i, f -> SessionSample(t = i * 1000L, frameMs = f) })

    @Test
    fun heldShareCountsFramesWithinTenPercentOfTarget() {
        // 16.7 ms target; 18.3 ms is the tolerance edge (16.7 × 1.1 ≈ 18.3).
        val s = session(60, listOf(16.6f, 16.7f, 18.0f, 20.0f, 33.0f, null))
        assertEquals(3f / 5f, s.heldShare!!, 1e-4f)
    }

    @Test
    fun heldShareIsNullWithoutTargetOrFrames() {
        assertNull(session(0, listOf(16f)).heldShare)
        assertNull(session(60, listOf(null, null)).heldShare)
    }

    @Test
    fun withSampleDownsamplesPastTheCapAndKeepsShape() {
        var s = session(60, emptyList())
        repeat(GameSession.MAX_SAMPLES + 1) { i -> s = s.withSample(SessionSample(t = i * 1000L, frameMs = if (i % 2 == 0) 10f else 20f)) }
        assertTrue(s.samples.size <= GameSession.MAX_SAMPLES)
        assertEquals((GameSession.MAX_SAMPLES + 1 + 1) / 2, s.samples.size)
        // Pair-averaged 10/20 → 15 everywhere except a possible odd tail.
        assertEquals(15f, s.samples.first().frameMs!!, 1e-4f)
        assertEquals(0L, s.samples.first().t)
    }

    @Test
    fun downsampleAveragesOnlyPresentValues() {
        val d = GameSession.downsample(listOf(SessionSample(0, frameMs = null, drawW = 4f), SessionSample(1000, frameMs = 16f, drawW = null)))
        assertEquals(1, d.size)
        assertEquals(16f, d[0].frameMs!!, 1e-4f)
        assertEquals(4f, d[0].drawW!!, 1e-4f)
    }

    @Test
    fun durationUsesLastSampleWhileLiveAndEndTimeAfterwards() {
        val live = session(60, listOf(16f, 16f, 16f))
        assertEquals(2_000L, live.durationMs)
        assertTrue(live.isLive)
        val done = live.ended(atMs = 61_000L)
        assertEquals(60_000L, done.durationMs)
    }

    @Test
    fun aggregatesIgnoreNulls() {
        val s = GameSession("p", "G", 0L, samples = listOf(
            SessionSample(0, frameMs = 16f, drawW = 4f, cpuC = 50, gpuC = 40),
            SessionSample(1000, frameMs = null, drawW = null, cpuC = 70, gpuC = 71),
            SessionSample(2000, frameMs = 32f, drawW = 6f, cpuC = null, gpuC = null),
        ))
        assertEquals(5f, s.avgDrawW!!, 1e-4f)
        assertEquals(6f, s.peakDrawW!!, 1e-4f)
        assertEquals(71, s.peakTempC)
        assertEquals(1000f / 24f, s.avgFps!!, 1e-3f)
    }
}
