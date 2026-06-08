package com.mc.mateamhf.data.playlist

import android.net.Uri

/**
 * What each streaming-service provider implementation must offer for the playlist
 * generation flow to work. Provider implementations are stateless — token lifetime
 * is managed by [TokenStore].
 */
interface PlaylistProvider {

    val service: PlaylistService

    /** True when the provider is configured (e.g. has a client_id). */
    val isConfigured: Boolean

    /**
     * Build the authorize URL the user opens in the browser. Must round-trip the
     * [PkceChallenge.codeChallenge] when the provider uses PKCE.
     */
    fun authorizeUri(pkce: PkceChallenge, state: String): Uri

    /** Trade the authorization [code] for an access token. */
    suspend fun exchangeCode(code: String, pkce: PkceChallenge): OAuthToken

    /**
     * For services that support it, get the artist's top tracks (by play count) on
     * the user's market. We ask for [limit] — providers may return fewer.
     *
     * @param providerArtistId the id that lives in this provider's namespace
     *                         (Spotify id, Deezer id, etc.)
     */
    suspend fun topTracksForArtist(
        providerArtistId: String,
        accessToken: String,
        limit: Int = 3,
    ): List<Track>

    /** Create an empty playlist named [name] on the user's account. */
    suspend fun createPlaylist(name: String, description: String, accessToken: String): CreatedPlaylist

    /** Append [trackIds] to an existing playlist created earlier. */
    suspend fun addTracksToPlaylist(playlistId: String, trackIds: List<String>, accessToken: String)
}
