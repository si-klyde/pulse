package com.kei.pulse.appwatch

import android.app.usage.UsageEvents
import com.kei.pulse.appwatch.ForegroundTracker.Ev
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundResolverTest {
    private val R = UsageEvents.Event.ACTIVITY_RESUMED
    private val P = UsageEvents.Event.ACTIVITY_PAUSED
    private val S = UsageEvents.Event.ACTIVITY_STOPPED

    @Test
    fun gameResumedLongAgoIsStillInFront() {
        val evs = sequenceOf(Ev(R, "home", "Launcher", 1), Ev(P, "home", "Launcher", 2), Ev(R, "game", "Game", 3))
        assertEquals("game", ForegroundResolver.latestForeground(evs))
    }

    @Test
    fun loadingActivityStoppingAfterGameResumesDoesNotHideTheGame() {
        val evs = sequenceOf(Ev(R, "wuwa", "Loading", 1), Ev(P, "wuwa", "Loading", 2), Ev(R, "wuwa", "Game", 3), Ev(S, "wuwa", "Loading", 4))
        assertEquals("wuwa", ForegroundResolver.latestForeground(evs))
    }

    @Test
    fun everythingPausedMeansNothingInFront() {
        val evs = sequenceOf(Ev(R, "game", "Game", 1), Ev(P, "game", "Game", 2), Ev(S, "game", "Game", 3))
        assertNull(ForegroundResolver.latestForeground(evs))
    }

    @Test
    fun switchingAppsPicksTheLatestResumed() {
        val evs = sequenceOf(Ev(R, "a", "A", 1), Ev(P, "a", "A", 2), Ev(R, "b", "B", 3), Ev(S, "a", "A", 4))
        assertEquals("b", ForegroundResolver.latestForeground(evs))
    }

    @Test
    fun incrementalFeedsAccumulate() {
        val t = ForegroundTracker()
        t.feed(sequenceOf(Ev(R, "game", "Game", 10)))
        assertEquals("game", t.current)
        t.feed(sequenceOf(Ev(P, "game", "Game", 20), Ev(R, "home", "Launcher", 21)))
        assertEquals("home", t.current)
        t.feed(emptySequence())
        assertEquals("home", t.current)
    }

    @Test
    fun overlappingQueriesDoNotReplayOldEvents() {
        val t = ForegroundTracker()
        t.feed(sequenceOf(Ev(R, "game", "Game", 10), Ev(P, "game", "Game", 20)))
        assertNull(t.current)
        // A second query whose window overlaps the first re-delivers the old RESUMED; it must be ignored.
        t.feed(sequenceOf(Ev(R, "game", "Game", 10), Ev(P, "game", "Game", 20)))
        assertNull(t.current)
        assertEquals(20L, t.lastEventMs)
    }

    @Test
    fun eventsSharingOneMillisecondAreAllApplied() {
        // Real device: Loading RESUMED, Loading PAUSED, Game RESUMED, Loading STOPPED all stamped the same ms.
        val evs = sequenceOf(Ev(R, "wuwa", "Loading", 5), Ev(P, "wuwa", "Loading", 5), Ev(R, "wuwa", "Game", 5), Ev(S, "wuwa", "Loading", 5))
        assertEquals("wuwa", ForegroundResolver.latestForeground(evs))
    }

    @Test
    fun replayAtTheWatermarkIsIgnoredButNewEventsAtTheSameMsAreNot() {
        val t = ForegroundTracker()
        t.feed(sequenceOf(Ev(R, "a", "A", 7)))
        // Overlapping query re-delivers the same event, plus a genuinely new one at the same ms.
        t.feed(sequenceOf(Ev(R, "a", "A", 7), Ev(P, "a", "A", 7), Ev(R, "b", "B", 7)))
        assertEquals("b", t.current)
        t.feed(sequenceOf(Ev(R, "a", "A", 7), Ev(P, "a", "A", 7), Ev(R, "b", "B", 7)))
        assertEquals("b", t.current)
    }

    @Test
    fun ignoresNonLifecycleEvents() {
        val evs = sequenceOf(Ev(R, "game", "G", 1), Ev(UsageEvents.Event.USER_INTERACTION, "other", null, 2), Ev(UsageEvents.Event.CONFIGURATION_CHANGE, "x", null, 3))
        assertEquals("game", ForegroundResolver.latestForeground(evs))
    }

    @Test
    fun emptyIsNull() {
        assertNull(ForegroundResolver.latestForeground(emptySequence()))
    }
}
