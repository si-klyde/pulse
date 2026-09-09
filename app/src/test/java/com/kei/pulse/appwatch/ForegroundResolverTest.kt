package com.kei.pulse.appwatch

import android.app.usage.UsageEvents
import com.kei.pulse.appwatch.ForegroundResolver.Ev
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundResolverTest {
    private val R = UsageEvents.Event.ACTIVITY_RESUMED
    private val P = UsageEvents.Event.ACTIVITY_PAUSED
    private val S = UsageEvents.Event.ACTIVITY_STOPPED

    @Test
    fun gameResumedLongAgoIsStillInFront() {
        val evs = sequenceOf(Ev(R, "home", "Launcher"), Ev(P, "home", "Launcher"), Ev(R, "game", "Game"))
        assertEquals("game", ForegroundResolver.latestForeground(evs))
    }

    @Test
    fun loadingActivityStoppingAfterGameResumesDoesNotHideTheGame() {
        // The real Wuthering Waves sequence: Loading RESUMED, Loading PAUSED, Game RESUMED, Loading STOPPED.
        val evs = sequenceOf(Ev(R, "wuwa", "Loading"), Ev(P, "wuwa", "Loading"), Ev(R, "wuwa", "Game"), Ev(S, "wuwa", "Loading"))
        assertEquals("wuwa", ForegroundResolver.latestForeground(evs))
    }

    @Test
    fun everythingPausedMeansNothingInFront() {
        val evs = sequenceOf(Ev(R, "game", "Game"), Ev(P, "game", "Game"), Ev(S, "game", "Game"))
        assertNull(ForegroundResolver.latestForeground(evs))
    }

    @Test
    fun switchingAppsPicksTheLatestResumed() {
        val evs = sequenceOf(Ev(R, "a", "A"), Ev(P, "a", "A"), Ev(R, "b", "B"), Ev(S, "a", "A"))
        assertEquals("b", ForegroundResolver.latestForeground(evs))
    }

    @Test
    fun ignoresNonLifecycleEvents() {
        val evs = sequenceOf(Ev(R, "game", "G"), Ev(UsageEvents.Event.USER_INTERACTION, "other"), Ev(UsageEvents.Event.CONFIGURATION_CHANGE, "x"))
        assertEquals("game", ForegroundResolver.latestForeground(evs))
    }

    @Test
    fun emptyIsNull() {
        assertNull(ForegroundResolver.latestForeground(emptySequence()))
    }
}
