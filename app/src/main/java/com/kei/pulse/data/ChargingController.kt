package com.kei.pulse.data

import com.kei.pulse.root.RootSupport

/**
 * The RP6's charging controls, all vendor-owned:
 *  - `usb_charge_now` (qcom-battery sysfs): 1 = battery charges, 0 = bypassed. The vendor toggles it with the
 *    screen when charging separation is on; the [com.kei.pulse.appwatch.ChargingGuard] repairs missed writes.
 *  - `is_charging_separation` / `percent_80_charge_limit` (Settings.System): the vendor's own toggles; a
 *    service of theirs applies them to the hardware, same pattern as `fan_mode`.
 * Verified on the RP6 (2026-09-09): flipping the separation key switches `status` Charging/Discharging within
 * a second. Self-gates on [isSupported] wherever the node is absent.
 */
class ChargingController {

    fun isSupported(): Boolean = readUsbChargeNow() != null

    fun readUsbChargeNow(): Int? = RootSupport.cat(USB_CHARGE_NOW_PATH)?.toIntOrNull()

    fun setUsbChargeNow(charge: Boolean) {
        RootSupport.runRootCommand("echo ${if (charge) 1 else 0} > $USB_CHARGE_NOW_PATH")
    }

    fun setSeparation(enabled: Boolean) {
        RootSupport.runRootCommand("settings put system $KEY_SEPARATION ${if (enabled) 1 else 0}")
    }

    fun setChargeLimit80(enabled: Boolean) {
        RootSupport.runRootCommand("settings put system $KEY_LIMIT_80 ${if (enabled) 1 else 0}")
    }

    companion object {
        const val USB_CHARGE_NOW_PATH = "/sys/class/qcom-battery/usb_charge_now"
        const val KEY_SEPARATION = "is_charging_separation"
        const val KEY_LIMIT_80 = "percent_80_charge_limit"
    }
}
