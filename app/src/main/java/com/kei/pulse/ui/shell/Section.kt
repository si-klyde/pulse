package com.kei.pulse.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The six destinations on the rail, in rail order. Frequency-sorted: what you touch per session on top,
 * per-title setup in the middle, [SYSTEM] pinned to the bottom behind a divider.
 */
enum class Section(val label: String, val icon: ImageVector) {
    POWER("Power", Icons.Rounded.Bolt),
    FAN("Fan", Icons.Rounded.Air),
    PER_GAME("Per game", Icons.Rounded.GridView),
    OVERLAY("Overlay", Icons.Rounded.Monitor),
    LIGHTS("Lights", Icons.Rounded.LightMode),
    SYSTEM("System", Icons.Rounded.Settings),
    ;

    companion object {
        val primary: List<Section> = listOf(POWER, FAN, PER_GAME, OVERLAY, LIGHTS)
    }
}
