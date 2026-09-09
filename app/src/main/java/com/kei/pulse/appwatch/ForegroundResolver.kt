package com.kei.pulse.appwatch

import android.app.usage.UsageEvents

/**
 * Pure logic behind "what is in front right now" from a chronological stream of usage events.
 *
 * The watcher normally looks only at the last few seconds, which is right for change detection but blind
 * on (re)start: a game already running produced its RESUMED event long ago. [latestForeground] walks a
 * wider window tracking each ACTIVITY separately — a game's loading activity typically STOPS right after
 * its game activity RESUMES, so "newest event" alone would wrongly say nothing is in front. The answer is
 * the package of the most recently resumed activity that has not since paused or stopped.
 */
object ForegroundResolver {

    data class Ev(val type: Int, val packageName: String, val className: String? = null)

    fun latestForeground(events: Sequence<Ev>): String? {
        // className → package for activities currently resumed, in resume order (LinkedHashMap keeps it).
        val resumed = LinkedHashMap<String, String>()
        for (e in events) {
            val key = (e.className ?: "") + "@" + e.packageName
            when (e.type) {
                UsageEvents.Event.ACTIVITY_RESUMED -> { resumed.remove(key); resumed[key] = e.packageName }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> resumed.remove(key)
            }
        }
        return resumed.values.lastOrNull()
    }
}
