package com.kei.pulse.root

import android.content.Context
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object RootSupport {

    // Process-wide serialization of PServer access. The binder cannot service overlapping transacts
    // reliably, and several callers (per-app watcher apply, telemetry poll, tile, sleep monitor) hit it
    // concurrently. Every PServer path funnels through this lock. It is reentrant so runGeneratedScript
    // can hold it across write+exec while delegating the exec to the same guarded call.
    private val pServerLock = ReentrantLock()

    // The PServer binder is looked up via reflection. A successful lookup is cached for the process
    // lifetime; a FAILED lookup is not, because at boot the service can come up after PULSE does and
    // latching "unavailable" would leave every root path dead until the app is reopened.
    @Volatile private var cachedExecutor: RootExecutor? = null

    /** Test seam: replaced by unit tests to inject a fake executor. Production always builds [RootExec]. */
    internal var executorFactory: () -> RootExecutor = { RootExec() }

    private fun executor(): RootExecutor {
        cachedExecutor?.let { return it }
        val fresh = executorFactory()
        if (fresh.pServerAvailable) cachedExecutor = fresh
        return fresh
    }

    val isAvailable: Boolean
        get() = executor().pServerAvailable

    fun runRootCommand(command: String): String? {
        return pServerLock.withLock {
            executor().executeAsRoot(command).getOrNull()
        }
    }

    /** `cat` one sysfs node as root; null when missing/empty. Path is quoted so odd characters stay literal. */
    fun cat(path: String): String? =
        runRootCommand("cat ${shellQuote(path)} 2>/dev/null")?.trim()?.takeIf { it.isNotEmpty() }

    fun runGeneratedScript(
        context: Context,
        scriptName: String,
        scriptContents: String,
    ): String? = runGeneratedScript(File(context.filesDir, "root-scripts"), scriptName, scriptContents)

    // The generated script lives in app-private storage but MUST be world-readable/-executable: the stock
    // PServer service runs it as root from a DIFFERENT uid, so it has to read+exec our file. This is the core
    // of the no-root mechanism, not an oversight — hence the deliberate suppression.
    //
    // Write AND exec happen under the lock: callers share fixed script names (apply-frequencies.sh), so a
    // write outside the lock let one caller overwrite the file another caller was about to execute.
    @android.annotation.SuppressLint("SetWorldReadable")
    fun runGeneratedScript(
        scriptDir: File,
        scriptName: String,
        scriptContents: String,
    ): String? = pServerLock.withLock {
        if (!scriptDir.exists()) scriptDir.mkdirs()
        val scriptFile = File(scriptDir, scriptName)
        scriptFile.writeText(scriptContents)
        scriptFile.setReadable(true, false)
        scriptFile.setExecutable(true, false)
        runRootCommand("sh ${scriptFile.absolutePath}")
    }

    /** Drop the cached executor and restore the production factory. Unit tests only. */
    internal fun resetForTest() {
        cachedExecutor = null
        executorFactory = { RootExec() }
    }
}
