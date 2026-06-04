package com.mc.mateamhf.domain

import androidx.compose.ui.graphics.Color

enum class Stage(val displayName: String, val color: Color) {
    MAINSTAGE_01("Mainstage 01", Color(0xFF1B4FA0)),
    MAINSTAGE_02("Mainstage 02", Color(0xFF6E6E6E)),
    WARZONE     ("Warzone",      Color(0xFF9DBA2A)),
    VALLEY      ("Valley",       Color(0xFFCC7A1B)),
    TEMPLE      ("Temple",       Color(0xFF8E8E8E)),
    ALTAR       ("Altar",        Color(0xFFC9302C)),
}
