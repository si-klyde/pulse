package com.kei.pulse.data

import com.kei.pulse.root.RootExecutor
import com.kei.pulse.root.RootSupport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ChargingControllerTest {

    private class Exec(private val node: String?) : RootExecutor {
        override val pServerAvailable = true
        val commands = mutableListOf<String>()
        override fun executeAsRoot(cmd: String): Result<String?> {
            commands += cmd
            return Result.success(if (cmd.startsWith("cat ")) node else null)
        }
    }

    @Before fun setUp() = RootSupport.resetForTest()
    @After fun tearDown() = RootSupport.resetForTest()

    @Test
    fun readsTheVendorChargeNode() {
        RootSupport.executorFactory = { Exec("1") }
        assertEquals(1, ChargingController().readUsbChargeNow())
    }

    @Test
    fun missingNodeReadsNull() {
        RootSupport.executorFactory = { Exec(null) }
        assertEquals(null, ChargingController().readUsbChargeNow())
    }

    @Test
    fun enablingChargeWritesOneToTheNode() {
        val exec = Exec("0")
        RootSupport.executorFactory = { exec }
        ChargingController().setUsbChargeNow(true)
        assertEquals(listOf("echo 1 > ${ChargingController.USB_CHARGE_NOW_PATH}"), exec.commands)
    }

    @Test
    fun separationAndLimitAreVendorSettingsKeys() {
        val exec = Exec(null)
        RootSupport.executorFactory = { exec }
        ChargingController().setSeparation(false)
        ChargingController().setChargeLimit80(true)
        assertEquals(
            listOf("settings put system is_charging_separation 0", "settings put system percent_80_charge_limit 1"),
            exec.commands,
        )
    }
}
