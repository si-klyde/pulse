package com.kei.pulse.appwatch

import android.content.Context
import android.content.pm.PackageManager
import com.kei.pulse.data.FpsReader
import com.kei.pulse.data.PowerSource
import com.kei.pulse.data.SessionFeed
import com.kei.pulse.data.SessionStore
import com.kei.pulse.data.TelemetrySnapshot
import com.kei.pulse.model.GameSession
import com.kei.pulse.model.SessionSample

/**
 * Records one [GameSession] at a time from the watcher's tick. Called once per tick with whatever the
 * watcher already read — it adds no device I/O of its own. A new package ends the previous session; a
 * neutral foreground (home, PULSE, Android UI) pauses and, after [IDLE_END_MS], ends it. Persists every
 * [PERSIST_EVERY] samples and on end, so a kill mid-game still leaves a recap.
 */
class SessionRecorder(
    private val context: Context,
    private val store: SessionStore = SessionStore(context),
) {
    private var live: GameSession? = null
    private var sinceSave = 0
    private var lastSampleAtMs = 0L

    init {
        // Boot the feed with whatever the last process left behind, so the app shows a recap immediately.
        if (SessionFeed.current.value == null) SessionFeed.publish(store.load()?.let { if (it.isLive) it.ended(it.startedAtMs + it.durationMs) else it })
    }

    fun sample(pkg: String, fps: FpsReader.FpsSample?, telemetry: TelemetrySnapshot, targetFps: Int) {
        val now = System.currentTimeMillis()
        var s = live
        if (s != null && s.packageName != pkg) { finish(now); s = null }
        if (s == null) {
            s = GameSession(packageName = pkg, label = labelFor(pkg), startedAtMs = now, targetFps = targetFps)
        } else if (s.targetFps != targetFps && targetFps > 0) {
            s = s.copy(targetFps = targetFps)
        }
        val plugged = PowerSource.isPlugged(context)
        val frameMs = fps?.fps?.takeIf { it > 0f }?.let { 1000f / it }
        val draw = if (plugged) null else telemetry.batteryDrawW
        s = s.withSample(SessionSample(t = now - s.startedAtMs, frameMs = frameMs, drawW = draw, cpuC = telemetry.cpuTempC, gpuC = telemetry.gpuTempC))
        live = s
        lastSampleAtMs = now
        SessionFeed.publish(s)
        if (++sinceSave >= PERSIST_EVERY) { sinceSave = 0; store.save(s) }
    }

    /** Nothing game-like in front this tick. Ends the session once it has been idle long enough. */
    fun idle() {
        val s = live ?: return
        if (System.currentTimeMillis() - lastSampleAtMs >= IDLE_END_MS) finish(lastSampleAtMs)
    }

    fun finish(atMs: Long = System.currentTimeMillis()) {
        val s = live ?: return
        val done = s.ended(atMs)
        live = null
        sinceSave = 0
        store.save(done)
        SessionFeed.publish(done)
    }

    private fun labelFor(pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg.substringAfterLast('.'))

    companion object {
        const val PERSIST_EVERY = 30
        const val IDLE_END_MS = 90_000L
    }
}
