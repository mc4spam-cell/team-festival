package com.mc.mateamhf.domain

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * Metadata about a festival the app can show. Lives in `assets/festivals/index.json` and
 * — once we open the running order — also at `groups/{id}.festivalIds` in Firestore.
 */
@Serializable
data class FestivalMeta(
    val id: String,
    val name: String,
    val shortName: String,
    val dates: String,
    val location: String,
    /** Hex `#RRGGBB`. Parsed lazily via [accentColor]. */
    val color: String,
) {
    fun accentColor(): Color = try {
        Color(android.graphics.Color.parseColor(color))
    } catch (_: IllegalArgumentException) {
        Color(0xFFC9302C) // Hellfest red fallback
    }
}

/** Hardcoded id of the only festival shipped with the app at launch. */
const val DEFAULT_FESTIVAL_ID: String = "hellfest-2026"
