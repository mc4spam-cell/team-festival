package com.mc.mateamhf.data.playlist

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tokenDataStore by preferencesDataStore(name = "playlist_tokens")

/**
 * Persists OAuth tokens per [PlaylistService] in a dedicated DataStore. The tokens are
 * scoped to the user's Spotify/Deezer account and only the access token + expiry are
 * needed for the read+write operations the playlist generator performs.
 *
 * NOT encrypted on disk. The risk surface is limited: tokens carry only the scopes we
 * requested (playlist-modify-private + playlist-modify-public for Spotify), they expire
 * within ~1 hour, and they live only on the user's own device. Acceptable for the
 * personal-use scope of this app.
 */
class TokenStore(private val appContext: Context) {

    private fun keyAccess(s: PlaylistService) = stringPreferencesKey("${s.name}_access")
    private fun keyExpires(s: PlaylistService) = longPreferencesKey("${s.name}_expires")
    private fun keyRefresh(s: PlaylistService) = stringPreferencesKey("${s.name}_refresh")
    private fun keyScope(s: PlaylistService) = stringPreferencesKey("${s.name}_scope")
    private fun keyType(s: PlaylistService) = stringPreferencesKey("${s.name}_type")

    suspend fun save(service: PlaylistService, token: OAuthToken) {
        appContext.tokenDataStore.edit { p ->
            p[keyAccess(service)] = token.accessToken
            p[keyExpires(service)] = token.expiresAt
            p[keyType(service)] = token.tokenType
            token.refreshToken?.let { p[keyRefresh(service)] = it } ?: p.remove(keyRefresh(service))
            token.scope?.let { p[keyScope(service)] = it } ?: p.remove(keyScope(service))
        }
    }

    suspend fun load(service: PlaylistService): OAuthToken? = appContext.tokenDataStore.data
        .map { p ->
            val at = p[keyAccess(service)] ?: return@map null
            OAuthToken(
                accessToken = at,
                tokenType = p[keyType(service)] ?: "Bearer",
                expiresAt = p[keyExpires(service)] ?: 0L,
                refreshToken = p[keyRefresh(service)],
                scope = p[keyScope(service)],
            )
        }
        .first()

    suspend fun clear(service: PlaylistService) {
        appContext.tokenDataStore.edit { p ->
            p.remove(keyAccess(service))
            p.remove(keyExpires(service))
            p.remove(keyType(service))
            p.remove(keyRefresh(service))
            p.remove(keyScope(service))
        }
    }
}
