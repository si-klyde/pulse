package com.kei.pulse.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Square by default: the design is hairlines and inverted fills, not cards. extraLarge stays 12 dp for the
// in-game overlay card and the tile sheet, which float over other apps and read better with a radius.
val PulseShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(12.dp),
)
