package com.kei.pulse.data

import com.kei.pulse.model.CpuPolicyInfo
import com.kei.pulse.root.RootExecutor
import com.kei.pulse.root.RootSupport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SystemTuningShellSafetyTest {

    private class RecordingExecutor : RootExecutor {
        override val pServerAvailable = true
        val commands = mutableListOf<String>()
        override fun executeAsRoot(cmd: String): Result<String?> {
            commands += cmd
            return Result.success(null)
        }
    }

    private val policy = CpuPolicyInfo(
        id = 0,
        policyPath = "/p0",
        scalingMaxPath = "/p0/scaling_max_freq",
        currentMaxFreq = 1_000_000,
        selectableMaxFreq = 1_000_000,
        observedMaxFreq = 1_000_000,
        minFreq = 300_000,
        supportedFrequencies = listOf(300_000, 1_000_000),
    )

    @Before
    fun setUp() = RootSupport.resetForTest()

    @After
    fun tearDown() = RootSupport.resetForTest()

    @Test
    fun rawGovernorIsQuotedWhenEchoed() {
        val exec = RecordingExecutor()
        RootSupport.executorFactory = { exec }
        GovernorController().setGovernorRaw(listOf(policy), "sched'; reboot; '")
        assertEquals(
            listOf("chmod 666 /p0/scaling_governor; echo 'sched'\\''; reboot; '\\''' > /p0/scaling_governor; chmod 644 /p0/scaling_governor"),
            exec.commands,
        )
    }
}
