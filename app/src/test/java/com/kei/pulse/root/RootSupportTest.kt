package com.kei.pulse.root

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RootSupportTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Executes `sh <path>` by reading the file back, after a pause that widens any write/exec race. */
    private class FileReadingExecutor(private val pauseMs: Long = 0L) : RootExecutor {
        override val pServerAvailable = true
        val commands = mutableListOf<String>()

        override fun executeAsRoot(cmd: String): Result<String?> {
            synchronized(commands) { commands += cmd }
            if (pauseMs > 0) Thread.sleep(pauseMs)
            val path = cmd.removePrefix("sh ").trim()
            return Result.success(File(path).takeIf { it.isFile }?.readText())
        }
    }

    @Before
    fun setUp() = RootSupport.resetForTest()

    @After
    fun tearDown() = RootSupport.resetForTest()

    @Test
    fun generatedScriptExecutesExactlyWhatItsCallerWrote() {
        RootSupport.executorFactory = { FileReadingExecutor(pauseMs = 5) }
        val dir = tmp.newFolder("root-scripts")
        val threads = 4
        val rounds = 10
        val mismatches = AtomicInteger()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)
        repeat(threads) { t ->
            pool.submit {
                start.await()
                repeat(rounds) { r ->
                    val body = "#!/system/bin/sh\necho thread=$t round=$r\n"
                    val out = RootSupport.runGeneratedScript(dir, "apply-frequencies.sh", body)
                    if (out != body) mismatches.incrementAndGet()
                }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))
        assertEquals("a caller ran another caller's script", 0, mismatches.get())
    }

    @Test
    fun generatedScriptIsWorldReadableAndExecutable() {
        RootSupport.executorFactory = { FileReadingExecutor() }
        val dir = tmp.newFolder("root-scripts")
        RootSupport.runGeneratedScript(dir, "probe.sh", "echo hi\n")
        val file = File(dir, "probe.sh")
        assertTrue(file.canRead())
        assertTrue(file.canExecute())
    }

    @Test
    fun availableExecutorIsCreatedOnceAndReused() {
        val created = AtomicInteger()
        RootSupport.executorFactory = {
            created.incrementAndGet()
            FileReadingExecutor()
        }
        repeat(25) { RootSupport.runRootCommand("cat /x") }
        assertTrue(RootSupport.isAvailable)
        assertEquals(1, created.get())
    }

    @Test
    fun unavailableExecutorIsNotCachedAndIsRetried() {
        val created = AtomicInteger()
        var bootFinished = false
        RootSupport.executorFactory = {
            created.incrementAndGet()
            if (bootFinished) FileReadingExecutor() else UnavailableExecutor
        }
        assertFalse(RootSupport.isAvailable)
        assertEquals(null, RootSupport.runRootCommand("cat /x"))
        val probesWhileDown = created.get()
        assertTrue("must re-probe while PServer is down", probesWhileDown >= 2)

        bootFinished = true
        assertTrue(RootSupport.isAvailable)
        repeat(10) { RootSupport.runRootCommand("cat /x") }
        assertEquals("first success must be cached", probesWhileDown + 1, created.get())
    }

    @Test
    fun catQuotesThePath() {
        val exec = FileReadingExecutor()
        RootSupport.executorFactory = { exec }
        RootSupport.cat("/sys/it's odd/temp")
        assertEquals(listOf("cat '/sys/it'\\''s odd/temp' 2>/dev/null"), exec.commands)
    }

    private object UnavailableExecutor : RootExecutor {
        override val pServerAvailable = false
        override fun executeAsRoot(cmd: String): Result<String?> =
            Result.failure(IllegalStateException("PServer not available"))
    }
}
