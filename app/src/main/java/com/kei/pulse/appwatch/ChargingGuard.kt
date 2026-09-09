package com.kei.pulse.appwatch

/**
 * Pure policy for "always charge while the screen is off".
 *
 * The vendor's charging separation writes `usb_charge_now = 0` on screen-on (battery bypassed) and `1` on
 * screen-off (battery charges). It misses the screen-off write when its process was killed under memory
 * pressure or when plugging in woke the device, the battery then sits uncharged with the screen off. This
 * guard only ever ENABLES charging, and only while the screen is off; screen-on behaviour stays the vendor's.
 */
object ChargingGuard {
    enum class Action { NONE, ENABLE_CHARGE }

    const val CHECK_INTERVAL_MS = 60_000L

    fun decide(enabled: Boolean, screenOn: Boolean, plugged: Boolean, usbChargeNow: Int?): Action =
        if (enabled && !screenOn && plugged && usbChargeNow == 0) Action.ENABLE_CHARGE else Action.NONE

    /** Check right after the screen goes off, then once per [CHECK_INTERVAL_MS] while it stays off. */
    fun shouldCheck(nowMs: Long, lastCheckMs: Long, justWentOff: Boolean): Boolean =
        justWentOff || nowMs - lastCheckMs >= CHECK_INTERVAL_MS
}
