package com.kei.pulse.data

import com.kei.pulse.root.RootExecutor
import com.kei.pulse.root.RootSupport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The RGB restore path echoes values read back from `Settings.System` into a root shell. Any app holding
 * WRITE_SETTINGS can plant those values, so they must never be able to break out of the command.
 */
class RgbControllerShellSafetyTest {

    private class RecordingExecutor(private val colorValue: String, private val brightnessValue: String) : RootExecutor {
        override val pServerAvailable = true
        val puts = mutableListOf<String>()

        override fun executeAsRoot(cmd: String): Result<String?> {
            if (cmd.startsWith("settings put")) puts += cmd
            return Result.success(
                when {
                    cmd.startsWith("settings get system joystick_led_light_picker_color") -> colorValue
                    cmd.startsWith("settings get system led_light_brightness_percent") -> brightnessValue
                    else -> null
                },
            )
        }
    }

    @Before
    fun setUp() = RootSupport.resetForTest()

    @After
    fun tearDown() = RootSupport.resetForTest()

    @Test
    fun restoreQuotesCapturedColorAndBrightness() {
        val exec = RecordingExecutor("#ff112233,#ff445566", "0.08")
        RootSupport.executorFactory = { exec }
        val rgb = RgbController()
        rgb.setColor(0, 255, 0)
        rgb.off()
        val restore = exec.puts.last()
        assertTrue(restore, restore.contains("joystick_led_light_picker_color '#ff112233,#ff445566'"))
        assertTrue(restore, restore.contains("led_light_brightness_percent '0.08'"))
    }

    @Test
    fun hostileCapturedValuesAreNeverRestored() {
        val exec = RecordingExecutor("'; touch /data/pwned; '", "1'; reboot; '")
        RootSupport.executorFactory = { exec }
        val rgb = RgbController()
        rgb.setColor(0, 255, 0)
        exec.puts.clear()
        rgb.off()
        assertEquals("no restore command for values that are not a colour/brightness", emptyList<String>(), exec.puts)
    }

    @Test
    fun colorValueWithQuoteIsRejectedNotEscaped() {
        // Even a "valid-looking" colour with a stray quote must fail the whitelist rather than be echoed.
        val exec = RecordingExecutor("#ff112233,#ff44'5566", "0.5")
        RootSupport.executorFactory = { exec }
        val rgb = RgbController()
        rgb.setColor(0, 255, 0)
        exec.puts.clear()
        rgb.off()
        val restore = exec.puts.singleOrNull() ?: ""
        assertTrue(restore, !restore.contains("joystick_led_light_picker_color"))
        assertTrue(restore, restore.contains("led_light_brightness_percent '0.5'"))
    }
}
