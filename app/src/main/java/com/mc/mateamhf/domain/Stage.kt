package com.mc.mateamhf.domain

import androidx.compose.ui.graphics.Color

/**
 * A festival stage. Used to be a hardcoded enum (Hellfest-only); now a data class so each
 * festival can ship its own stages via its JSON `stages` block. Equality is structural, so
 * two Stage instances with the same id are interchangeable for grouping/filtering.
 */
data class Stage(
    val id: String,
    val name: String,
    val color: Color,
) {
    /** Legacy alias kept for screens that read `stage.displayName`. */
    val displayName: String get() = name
}
