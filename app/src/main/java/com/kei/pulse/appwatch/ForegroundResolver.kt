package com.kei.pulse.appwatch

import android.app.usage.UsageEvents

/**
 * What is in front right now, derived from the stream of usage events.
 *
 * Tracks each ACTIVITY separately: RESUMED adds it, PAUSED/STOPPED removes it, and [current] is the package
 * of the most recently resumed activity that is still resumed. This is what makes a game's loading activity
 * STOPPING right after its game activity RESUMED read correctly as "the game is in front".
 *
 * [feed] is incremental: give it whatever events arrived since the last call. Events at or before the last
 * processed timestamp are skipped, so overlapping queries are harmless. Seed it once with a long lookback so
 * a (re)start while a game is already running is not blind.
 */
class ForegroundTracker {
    data class Ev(val type: Int, val packageName: String, val className: String? = null, val timeMs: Long = 0L)

    private val resumed = LinkedHashMap<String, String>()
    var lastEventMs: Long = Long.MIN_VALUE
        private set
    // Many lifecycle events share one millisecond (a loading activity pausing as the game resumes), so the
    // watermark alone can't de-dup: remember the exact events already seen AT the watermark timestamp.
    private val seenAtWatermark = HashSet<String>()

    val current: String? get() = resumed.values.lastOrNull()

    fun feed(events: Sequence<Ev>) {
        for (e in events) {
            val key = (e.className ?: "") + "@" + e.packageName
            if (e.timeMs != 0L) {
                if (e.timeMs < lastEventMs) continue
                val sig = "${e.type}:$key"
                if (e.timeMs == lastEventMs) {
                    if (!seenAtWatermark.add(sig)) continue
                } else {
                    lastEventMs = e.timeMs
                    seenAtWatermark.clear()
                    seenAtWatermark.add(sig)
                }
            }
            when (e.type) {
                UsageEvents.Event.ACTIVITY_RESUMED -> { resumed.remove(key); resumed[key] = e.packageName }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> resumed.remove(key)
            }
        }
    }
}

/** One-shot form of [ForegroundTracker] for a complete event window. */
object ForegroundResolver {
    fun latestForeground(events: Sequence<ForegroundTracker.Ev>): String? = ForegroundTracker().apply { feed(events) }.current
}
