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
    val qobuzArtistId: String?,
    val tidalArtistId: String?,
    val instagramHandle: String?,
    val facebookHandle: String?,
    val twitterHandle: String?,
    val tiktokHandle: String?,
)

data class FestivalDay(
    val id: Int,
    val label: String,
    /** YYYY-MM-DD — needed to build ISO datetimes for team events on that day. */
    val date: String,
    val concerts: List<Concert>,
)

data class RunningOrder(
    val festival: String,
    /** Stages declared for this festival — varies per festival, so it lives on the running order. */
    val stages: List<Stage>,
    val days: List<FestivalDay>,
)
