package com.mc.mateamhf.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.mc.mateamhf.domain.Concert
import java.net.URLEncoder

private const val APPLE_MUSIC_PKG = "com.apple.android.music"
private const val SPOTIFY_PKG = "com.spotify.music"
private const val DEEZER_PKG = "deezer.android.app"
private const val INSTAGRAM_PKG = "com.instagram.android"

fun openInAppleMusic(context: Context, concert: Concert) {
    val url = concert.appleMusicPlaylist
        ?: concert.appleMusicArtistId?.let { "https://music.apple.com/fr/artist/$it" }
        ?: "https://music.apple.com/fr/search?term=${enc(concert.artist)}"
    open(context, url, APPLE_MUSIC_PKG)
}

fun openInSpotify(context: Context, concert: Concert) {
    val url = concert.spotifyArtistId?.let { "https://open.spotify.com/artist/$it" }
        ?: "https://open.spotify.com/search/${enc(concert.artist)}"
    open(context, url, SPOTIFY_PKG)
}

fun openInDeezer(context: Context, concert: Concert) {
    val url = concert.deezerArtistId?.let { "https://www.deezer.com/artist/$it" }
        ?: "https://www.deezer.com/search/${enc(concert.artist)}"
    open(context, url, DEEZER_PKG)
}

fun openInInstagram(context: Context, concert: Concert) {
    val handle = concert.instagramHandle ?: return
    open(context, "https://www.instagram.com/$handle/", INSTAGRAM_PKG)
}

private fun open(context: Context, url: String, preferredPackage: String) {
    val uri = url.toUri()
    val targeted = Intent(Intent.ACTION_VIEW, uri).setPackage(preferredPackage)
    try {
        context.startActivity(targeted)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
