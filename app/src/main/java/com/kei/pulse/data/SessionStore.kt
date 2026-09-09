package com.kei.pulse.data

import android.content.Context
import com.kei.pulse.model.GameSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Process-wide feed of the session the home screen should show: the live one while a game runs, else the
 * last finished one. The watcher writes it; the ViewModel reads it (same process, no IPC). [SessionStore]
 * persists the last session so a process kill mid-game, or a reboot, still leaves a recap.
 */
object SessionFeed {
    private val _current = MutableStateFlow<GameSession?>(null)
    val current: StateFlow<GameSession?> = _current

    fun publish(session: GameSession?) { _current.value = session }
}

class SessionStore(context: Context) {
    private val dir = File(context.applicationContext.filesDir, "sessions").apply { mkdirs() }
    private val file = File(dir, "last.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): GameSession? = runCatching {
        if (!file.isFile) null else json.decodeFromString(GameSession.serializer(), file.readText())
    }.getOrNull()

    /** Atomic: write to a sibling, then rename, so a kill mid-write never leaves a torn file. */
    fun save(session: GameSession) {
        runCatching {
            val tmp = File(dir, "last.json.tmp")
            tmp.writeText(json.encodeToString(GameSession.serializer(), session))
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        }
    }
}
