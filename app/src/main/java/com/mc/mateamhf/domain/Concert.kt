package com.mc.mateamhf.domain

import kotlinx.datetime.Instant

data class Concert(
    val id: String,
    val artist: String,
    val stage: Stage,
    val festivalDay: Int,
    val start: Instant,
    val end: Instant,
    val appleMusicPlaylist: String?,
    val appleMusicArtistId: Long?,
    val spotifyArtistId: String?,
    val deezerArtistId: Long?,
    val instagramHandle: String?,
)

data class FestivalDay(
    val id: Int,
    val label: String,
    val concerts: List<Concert>,
)

data class RunningOrder(
    val festival: String,
    val days: List<FestivalDay>,
)
