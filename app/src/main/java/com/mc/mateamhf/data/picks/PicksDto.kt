package com.mc.mateamhf.data.picks

import kotlinx.serialization.Serializable

@Serializable
data class PicksFile(
    val festival: String = "Hellfest Open Air 2026",
    val exportedAt: String,
    val picks: List<PickDto>,
)

@Serializable
data class PickDto(
    val artist: String,
    val priority: Int = 0,
    val rating: String? = null,
)
