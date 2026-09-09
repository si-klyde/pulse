package com.kei.pulse.appwatch

import com.kei.pulse.appwatch.ChargingGuard.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The vendor writes usb_charge_now=1 on screen-off and 0 on screen-on, but misses the screen-off write when
 * its process was killed or the plug-in woke the device. The guard's only job: while the screen is off and the
 * device is plugged in, the battery must be charging. It never touches anything while the screen is on.
 */
class ChargingGuardTest {

    @Test
    fun screenOffPluggedNotChargingMeansEnableCharge() {
        assertEquals(Action.ENABLE_CHARGE, ChargingGuard.decide(enabled = true, screenOn = false, plugged = true, usbChargeNow = 0))
    }

    @Test
    fun alreadyChargingNeedsNothing() {
        assertEquals(Action.NONE, ChargingGuard.decide(enabled = true, screenOn = false, plugged = true, usbChargeNow = 1))
    }

    @Test
    fun screenOnIsTheVendorsBusiness() {
        assertEquals(Action.NONE, ChargingGuard.decide(enabled = true, screenOn = true, plugged = true, usbChargeNow = 0))
    }

    @Test
    fun unpluggedNeedsNothing() {
        assertEquals(Action.NONE, ChargingGuard.decide(enabled = true, screenOn = false, plugged = false, usbChargeNow = 0))
    }

    @Test
    fun unknownNodeStateIsLeftAlone() {
        assertEquals(Action.NONE, ChargingGuard.decide(enabled = true, screenOn = false, plugged = true, usbChargeNow = null))
    }

    @Test
    fun disabledGuardNeverActs() {
        assertEquals(Action.NONE, ChargingGuard.decide(enabled = false, screenOn = false, plugged = true, usbChargeNow = 0))
    }

    @Test
    fun checksOnScreenOffThenOncePerInterval() {
        assertTrue(ChargingGuard.shouldCheck(nowMs = 10_000, lastCheckMs = 9_000, justWentOff = true))
        assertFalse(ChargingGuard.shouldCheck(nowMs = 10_000, lastCheckMs = 9_000, justWentOff = false))
        assertTrue(ChargingGuard.shouldCheck(nowMs = 9_000 + ChargingGuard.CHECK_INTERVAL_MS, lastCheckMs = 9_000, justWentOff = false))
    }
}
