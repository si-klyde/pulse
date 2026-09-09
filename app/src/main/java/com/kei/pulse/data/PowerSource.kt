package com.kei.pulse.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Whether the device is on external power. The sysfs `status` node on these devices keeps saying
 * "Discharging" on AC while `current_now` reads 0, so battery-derived draw is meaningless when plugged,
 * ask the framework instead of the node.
 */
object PowerSource {
    fun isPlugged(context: Context): Boolean {
        val intent = context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        return intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }
}
