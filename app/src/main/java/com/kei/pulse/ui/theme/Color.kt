package com.kei.pulse.ui.theme

import androidx.compose.ui.graphics.Color

// Quiet instrument. Warm graphite housing, off-white ink, one glass-blue accent.
// The semantic meter ramp (sage → brass → brick) lives in overlay/MeterColors and is deliberately
// separate from the accent so temperature/load never reads as "selected".
val InstrumentHousing = Color(0xFF1B1A18) // app background
val InstrumentBase    = Color(0xFF1F1E1B) // lowest raised
val InstrumentPanel   = Color(0xFF232220) // panels
val InstrumentRaised  = Color(0xFF2B2A27) // elevated panels
val InstrumentRaised2 = Color(0xFF333230)
val InstrumentRule    = Color(0xFF3A3833) // 1 dp rules / outline

val InstrumentAccent     = Color(0xFF8FB8CC) // glass blue
val InstrumentAccentDeep = Color(0xFF243239) // accent container
val InstrumentOnAccent   = Color(0xFF101418)

val InstrumentInk      = Color(0xFFECE8E0) // primary text
val InstrumentInkDim   = Color(0xFFA39E93) // secondary text
val InstrumentInkFaint = Color(0xFF6E6960)

val InstrumentBrick     = Color(0xFFD96B5C) // error / destructive (shared with meter "hot")
val InstrumentBrickDeep = Color(0xFF3A221E)
