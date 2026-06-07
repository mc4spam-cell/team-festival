package com.mc.mateamhf.domain

import kotlinx.datetime.Instant

/** Team-created event that sits outside the official running order (e.g. lunch meetup). */
data class TeamEvent(
    val id: String,
    val creatorUid: String,
    val creatorName: String,
    val title: String,
    val location: String?,
    val start: Instant,
    val end: Instant,
    val festivalId: String,
)
