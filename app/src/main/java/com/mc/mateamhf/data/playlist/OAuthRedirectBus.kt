package com.mc.mateamhf.data.playlist

import android.net.Uri
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Single global bus that carries OAuth redirect results from MainActivity (where
 * the custom-tab redirect lands) back to whatever coroutine is currently waiting
 * on completion of the authorization step.
 *
 * Singleton on purpose — the OAuth flow can only be in one state at a time and
 * Compose can't easily share a viewmodel-scoped channel with an Activity callback.
 */
object OAuthRedirectBus {

    sealed interface Result {
        data class Code(val service: PlaylistService, val code: String, val state: String?) : Result
        data class Error(val service: PlaylistService, val description: String) : Result
    }

    private val channel = Channel<Result>(Channel.BUFFERED)
    val flow: Flow<Result> = channel.receiveAsFlow()

    /** Called from MainActivity.onNewIntent when an OAuth redirect (com.mc.teamfestival scheme) lands. */
    fun handleRedirect(uri: Uri) {
        val service = when (uri.host) {
            "oauth.spotify", "spotify" -> PlaylistService.SPOTIFY
            "oauth.deezer", "deezer" -> PlaylistService.DEEZER
            else -> {
                // Fallback: path segment 0 might carry the service name
                when (uri.lastPathSegment) {
                    "spotify" -> PlaylistService.SPOTIFY
                    "deezer" -> PlaylistService.DEEZER
                    else -> return
                }
            }
        }
        val error = uri.getQueryParameter("error")
        if (error != null) {
            channel.trySend(
                Result.Error(service, uri.getQueryParameter("error_description") ?: error),
            )
            return
        }
        val code = uri.getQueryParameter("code") ?: return
        val state = uri.getQueryParameter("state")
        channel.trySend(Result.Code(service, code, state))
    }
}
