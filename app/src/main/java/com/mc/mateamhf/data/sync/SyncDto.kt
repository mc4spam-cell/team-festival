package com.mc.mateamhf.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class RemoteState(
    val festival: String = "Hellfest Open Air 2026",
    val users: Map<String, RemoteUser> = emptyMap(),
)

@Serializable
data class RemoteUser(
    val updatedAt: String = "",
    val p1Artists: List<String> = emptyList(),
)

@Serializable
data class PushBody(
    val user: String,
    val p1Artists: List<String>,
)
