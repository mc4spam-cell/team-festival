package com.mc.mateamhf.data.playlist

/** Which streaming service a playlist lives on. */
enum class PlaylistService(val displayName: String) {
    SPOTIFY("Spotify"),
    DEEZER("Deezer"),
}

/** A single track on a streaming service. ID format depends on the service. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
)

/** OAuth token + metadata. Refresh handled separately. */
data class OAuthToken(
    val accessToken: String,
    val tokenType: String,
    /** UNIX epoch seconds when accessToken stops being usable. */
    val expiresAt: Long,
    val refreshToken: String? = null,
    val scope: String? = null,
)

/** Result of a playlist creation. */
data class CreatedPlaylist(
    val id: String,
    val name: String,
    val webUrl: String,
    val deepLink: String?,
    val trackCount: Int,
)

/** What gets reported to the UI as the generator runs. */
sealed interface PlaylistGenStatus {
    data object Idle : PlaylistGenStatus
    data class AwaitingAuth(val service: PlaylistService) : PlaylistGenStatus
    data class Fetching(
        val service: PlaylistService,
        val artistsDone: Int,
        val artistsTotal: Int,
    ) : PlaylistGenStatus
    data class Creating(val service: PlaylistService) : PlaylistGenStatus
    data class Success(
        val service: PlaylistService,
        val playlists: List<CreatedPlaylist>,
    ) : PlaylistGenStatus
    data class Error(val message: String) : PlaylistGenStatus
}
