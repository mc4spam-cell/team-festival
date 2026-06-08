package com.mc.mateamhf.data.playlist

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.mc.mateamhf.domain.ConcertWithState
import com.mc.mateamhf.domain.Priority
import com.mc.mateamhf.domain.artistKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import java.util.UUID

/**
 * Orchestrates the playlist generation pipeline for one [PlaylistService]:
 *
 *  1. If we have no valid access token → launch Chrome Custom Tab on the
 *     provider's /authorize URL and suspend until [OAuthRedirectBus] fires.
 *  2. Exchange the auth code for an access token. Persist via [TokenStore].
 *  3. For each unique artist in [concertsWithState] at priority ≤ [P2], hit
 *     `top-tracks?limit=3` and collect tracks.
 *  4. Create two playlists on the user's account:
 *       - "Mes priorités 1" (P1 only)
 *       - "Mes priorités 1 & 2" (P1 + P2)
 *  5. Push tracks into each. Report progress via [status].
 */
class PlaylistGenerator(
    private val appContext: Context,
    private val http: OkHttpClient,
    private val tokenStore: TokenStore,
) {

    private val _status = MutableStateFlow<PlaylistGenStatus>(PlaylistGenStatus.Idle)
    val status: StateFlow<PlaylistGenStatus> = _status

    /**
     * Run the full pipeline. Returns the created playlists on success or throws on failure.
     *
     * @param festivalName e.g. "Hellfest Open Air 2026" — used in the playlist title.
     * @param concertsWithState ALL concerts of the active festival, joined with the user's picks.
     */
    suspend fun generate(
        service: PlaylistService,
        festivalName: String,
        concertsWithState: List<ConcertWithState>,
    ): List<CreatedPlaylist> {
        check(service == PlaylistService.SPOTIFY) {
            "Only Spotify is wired so far — Deezer needs a backend proxy for its OAuth secret."
        }
        val provider: PlaylistProvider = SpotifyProvider(http)
        if (!provider.isConfigured) {
            throw IllegalStateException(
                "Spotify client_id manquant. Ajoute teamfestival.spotifyClientId=<id> dans ~/.gradle/gradle.properties + rebuild.",
            )
        }

        // 1. Auth: reuse stored token if still valid, else interactive flow
        val now = System.currentTimeMillis() / 1000
        val cached = tokenStore.load(service)
        val token = if (cached != null && cached.expiresAt > now + 30) {
            cached
        } else {
            _status.value = PlaylistGenStatus.AwaitingAuth(service)
            val pkce = Pkce.generate()
            val state = UUID.randomUUID().toString()
            val uri = provider.authorizeUri(pkce, state)
            launchCustomTab(uri)

            when (val result = OAuthRedirectBus.flow.first()) {
                is OAuthRedirectBus.Result.Code -> {
                    val t = provider.exchangeCode(result.code, pkce)
                    tokenStore.save(service, t)
                    t
                }
                is OAuthRedirectBus.Result.Error -> throw IllegalStateException("OAuth refusé : ${result.description}")
            }
        }

        // 2. Pick artists at P1 and at P1+P2. Each set is dedup'd by artistKey.
        val p1Artists = uniquePickedArtists(concertsWithState, Priority.P1)
        val p1p2Artists = uniquePickedArtists(concertsWithState, Priority.P1, Priority.P2)

        // Only keep artists we have a Spotify id for — the others get skipped silently.
        val p1WithIds = p1Artists.mapNotNull { c -> c.concert.spotifyArtistId?.let { id -> id to c.concert.artist } }
        val p1p2WithIds = p1p2Artists.mapNotNull { c -> c.concert.spotifyArtistId?.let { id -> id to c.concert.artist } }

        if (p1WithIds.isEmpty() && p1p2WithIds.isEmpty()) {
            throw IllegalStateException(
                "Aucun pick avec un identifiant Spotify. Marque des P1 ou P2 sur des artistes connus de Spotify.",
            )
        }

        // 3. Fetch top tracks per artist sequentially (Spotify is fine with this rate)
        _status.value = PlaylistGenStatus.Fetching(service, 0, p1p2WithIds.size)
        val tracksByArtist = mutableMapOf<String, List<Track>>()
        p1p2WithIds.forEachIndexed { i, (artistId, _) ->
            val tracks = runCatching { provider.topTracksForArtist(artistId, token.accessToken) }
                .getOrDefault(emptyList())
            tracksByArtist[artistId] = tracks
            _status.value = PlaylistGenStatus.Fetching(service, i + 1, p1p2WithIds.size)
        }

        // 4. Create playlists and push tracks
        _status.value = PlaylistGenStatus.Creating(service)
        val date = java.time.LocalDate.now()
        val created = mutableListOf<CreatedPlaylist>()

        if (p1WithIds.isNotEmpty()) {
            val tracks = p1WithIds.flatMap { (id, _) -> tracksByArtist[id].orEmpty().map { it.id } }
            if (tracks.isNotEmpty()) {
                val playlist = provider.createPlaylist(
                    name = "Team Festival — $festivalName — Mes priorités 1 ($date)",
                    description = "Top 3 par artiste — P1 — généré par l'app Team Festival.",
                    accessToken = token.accessToken,
                )
                provider.addTracksToPlaylist(playlist.id, tracks, token.accessToken)
                created += playlist.copy(trackCount = tracks.size)
            }
        }

        if (p1p2WithIds.isNotEmpty()) {
            val tracks = p1p2WithIds.flatMap { (id, _) -> tracksByArtist[id].orEmpty().map { it.id } }
            if (tracks.isNotEmpty()) {
                val playlist = provider.createPlaylist(
                    name = "Team Festival — $festivalName — Mes priorités 1 & 2 ($date)",
                    description = "Top 3 par artiste — P1 + P2 — généré par l'app Team Festival.",
                    accessToken = token.accessToken,
                )
                provider.addTracksToPlaylist(playlist.id, tracks, token.accessToken)
                created += playlist.copy(trackCount = tracks.size)
            }
        }

        _status.value = PlaylistGenStatus.Success(service, created)
        return created
    }

    private fun uniquePickedArtists(
        concertsWithState: List<ConcertWithState>,
        vararg priorities: Priority,
    ): List<ConcertWithState> =
        concertsWithState
            .filter { it.priority in priorities }
            .distinctBy { artistKey(it.concert.artist) }

    private fun launchCustomTab(uri: Uri) {
        val intent = CustomTabsIntent.Builder().build()
        intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.launchUrl(appContext, uri)
    }

    /** Reset status when the user closes the dialog so a re-open starts clean. */
    fun resetStatus() {
        _status.value = PlaylistGenStatus.Idle
    }

    /** Drop the cached token so the next generation re-auths. Useful after a 401. */
    suspend fun signOut(service: PlaylistService) {
        tokenStore.clear(service)
    }
}
