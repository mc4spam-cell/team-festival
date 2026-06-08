package com.mc.mateamhf.data.playlist

import android.net.Uri
import com.mc.mateamhf.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Spotify Web API integration. Uses Authorization Code with PKCE — no client secret
 * required, which makes it safe to ship in a mobile app.
 *
 * Endpoints used:
 *   - https://accounts.spotify.com/authorize             (auth URL)
 *   - https://accounts.spotify.com/api/token             (code → token)
 *   - https://api.spotify.com/v1/me                      (resolve current user id)
 *   - https://api.spotify.com/v1/artists/{id}/top-tracks (3 per artist)
 *   - https://api.spotify.com/v1/users/{uid}/playlists   (create empty playlist)
 *   - https://api.spotify.com/v1/playlists/{pid}/tracks  (add tracks)
 */
class SpotifyProvider(
    private val http: OkHttpClient,
    private val clientId: String = BuildConfig.SPOTIFY_CLIENT_ID,
    private val redirectUri: String = REDIRECT_URI,
) : PlaylistProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override val service: PlaylistService = PlaylistService.SPOTIFY

    override val isConfigured: Boolean
        get() = clientId.isNotBlank()

    override fun authorizeUri(pkce: PkceChallenge, state: String): Uri =
        Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("code_challenge_method", pkce.codeChallengeMethod)
            .appendQueryParameter("code_challenge", pkce.codeChallenge)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("state", state)
            .build()

    override suspend fun exchangeCode(code: String, pkce: PkceChallenge): OAuthToken = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .add("client_id", clientId)
            .add("code_verifier", pkce.codeVerifier)
            .build()
        val req = Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .post(body)
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IllegalStateException("Spotify token exchange failed: ${resp.code} $text")
            parseToken(text)
        }
    }

    private fun parseToken(jsonText: String): OAuthToken {
        val obj = json.parseToJsonElement(jsonText).jsonObject
        val accessToken = obj["access_token"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No access_token in Spotify response: $jsonText")
        val expiresIn = obj["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
        return OAuthToken(
            accessToken = accessToken,
            tokenType = obj["token_type"]?.jsonPrimitive?.content ?: "Bearer",
            expiresAt = (System.currentTimeMillis() / 1000) + expiresIn,
            refreshToken = obj["refresh_token"]?.jsonPrimitive?.content,
            scope = obj["scope"]?.jsonPrimitive?.content,
        )
    }

    /** Resolve the current user's Spotify id — needed to create a playlist under their account. */
    suspend fun currentUserId(accessToken: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.spotify.com/v1/me")
            .header("Authorization", "Bearer $accessToken")
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IllegalStateException("Spotify /me failed: ${resp.code} $text")
            json.parseToJsonElement(text).jsonObject["id"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("No id in /me response: $text")
        }
    }

    override suspend fun topTracksForArtist(
        providerArtistId: String,
        accessToken: String,
        limit: Int,
    ): List<Track> = withContext(Dispatchers.IO) {
        // Use the user's market via `from_token` so we get tracks they can actually play.
        val url = "https://api.spotify.com/v1/artists/$providerArtistId/top-tracks?market=from_token"
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) return@withContext emptyList()
            val tracksJson = json.parseToJsonElement(text).jsonObject["tracks"]?.jsonArray ?: return@withContext emptyList()
            tracksJson.take(limit).mapNotNull { el ->
                val obj = el.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val title = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val artist = obj["artists"]?.jsonArray?.firstOrNull()?.jsonObject?.get("name")?.jsonPrimitive?.content.orEmpty()
                Track(id = id, title = title, artist = artist)
            }
        }
    }

    override suspend fun createPlaylist(name: String, description: String, accessToken: String): CreatedPlaylist =
        withContext(Dispatchers.IO) {
            val userId = currentUserId(accessToken)
            val body = buildJsonObject {
                put("name", JsonPrimitive(name))
                put("public", JsonPrimitive(false))
                put("description", JsonPrimitive(description))
            }.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/users/$userId/playlists")
                .header("Authorization", "Bearer $accessToken")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw IllegalStateException("Spotify create playlist failed: ${resp.code} $text")
                parseCreatedPlaylist(text)
            }
        }

    private fun parseCreatedPlaylist(text: String): CreatedPlaylist {
        val obj = json.parseToJsonElement(text).jsonObject
        val id = obj["id"]!!.jsonPrimitive.content
        val name = obj["name"]?.jsonPrimitive?.content.orEmpty()
        val external = obj["external_urls"]?.jsonObject?.get("spotify")?.jsonPrimitive?.content
            ?: "https://open.spotify.com/playlist/$id"
        return CreatedPlaylist(
            id = id,
            name = name,
            webUrl = external,
            deepLink = "spotify:playlist:$id",
            trackCount = 0,
        )
    }

    override suspend fun addTracksToPlaylist(
        playlistId: String,
        trackIds: List<String>,
        accessToken: String,
    ) = withContext(Dispatchers.IO) {
        // Spotify API caps at 100 URIs per call — chunk if longer.
        trackIds.chunked(100).forEach { batch ->
            val uris = batch.map { "spotify:track:$it" }
            val body = buildJsonObject {
                put("uris", kotlinx.serialization.json.JsonArray(uris.map { JsonPrimitive(it) }))
            }.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/playlists/$playlistId/tracks")
                .header("Authorization", "Bearer $accessToken")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val text = resp.body?.string().orEmpty()
                    throw IllegalStateException("Spotify add tracks failed: ${resp.code} $text")
                }
            }
        }
    }

    companion object {
        const val REDIRECT_URI = "com.mc.teamfestival://oauth/spotify"
        private const val SCOPES = "playlist-modify-private playlist-modify-public"
    }
}
