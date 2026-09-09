package com.kei.pulse.ui.theme

import androidx.compose.ui.graphics.Color

// RP6 design: true black housing (zero-power pixels on the AMOLED), white ink at three luminances, and
// colour ONLY where it means something, the meter ramp for heat/load/battery in overlay/MeterColors.
val Housing  = Color(0xFF000000) // app background
val Rule     = Color(0xFF1C1C1C) // hairlines
val Rule2    = Color(0xFF2A2A2A) // control outlines, track backgrounds
val Raised   = Color(0xFF141414) // the one raised surface (target band, pressed state)

val Ink      = Color(0xFFF4F2EE) // primary text, selected fills
val Ink2     = Color(0xFFA8A6A1) // secondary text
val Ink3     = Color(0xFF7A7A7A) // labels, units
val Ink4     = Color(0xFF4A4A4A) // faint: axis marks, disabled

val OnInk    = Color(0xFF000000) // text on an Ink fill

val Brick    = Color(0xFFD96B5C) // error / destructive (also the meter "hot")
val BrickDeep = Color(0xFF2A1512)
