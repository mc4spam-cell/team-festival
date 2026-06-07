package com.mc.mateamhf.data.providers

import com.mc.mateamhf.domain.Concert
import java.net.URLEncoder

/** All content services the app knows about. Persisted via DataStore by enum name — do NOT rename. */
enum class ProviderId(
    val displayName: String,
    val category: Category,
    /** Android package name for native-app routing; null means web-only. */
    val androidPackage: String?,
) {
    APPLE_MUSIC("Apple Music", Category.MUSIC, "com.apple.android.music"),
    SPOTIFY("Spotify", Category.MUSIC, "com.spotify.music"),
    DEEZER("Deezer", Category.MUSIC, "deezer.android.app"),
    QOBUZ("Qobuz", Category.MUSIC, "com.qobuz.music"),
    TIDAL("Tidal", Category.MUSIC, "com.aspiro.tidal"),
    INSTAGRAM("Instagram", Category.SOCIAL, "com.instagram.android"),
    FACEBOOK("Facebook", Category.SOCIAL, "com.facebook.katana"),
    X("X", Category.SOCIAL, "com.twitter.android"),
    TIKTOK("TikTok", Category.SOCIAL, "com.zhiliaoapp.musically");

    enum class Category { MUSIC, SOCIAL }

    /**
     * Build the URL we should open for [concert] on this provider. Prefers an artist-page deep link
     * when we have the ID, falls back to a search URL on the web (which Android will route into the
     * installed app via App Links when applicable).
     */
    fun urlFor(concert: Concert): String = when (this) {
        APPLE_MUSIC -> concert.appleMusicArtistId?.let { "https://music.apple.com/fr/artist/$it" }
            ?: "https://music.apple.com/fr/search?term=${enc(concert.artist)}"
        SPOTIFY -> concert.spotifyArtistId?.let { "https://open.spotify.com/artist/$it" }
            ?: "https://open.spotify.com/search/${enc(concert.artist)}"
        DEEZER -> concert.deezerArtistId?.let { "https://www.deezer.com/artist/$it" }
            ?: "https://www.deezer.com/search/${enc(concert.artist)}"
        QOBUZ -> concert.qobuzArtistId?.let { "https://www.qobuz.com/fr-fr/interpreter/-/$it" }
            ?: "https://www.qobuz.com/fr-fr/search?q=${enc(concert.artist)}"
        TIDAL -> concert.tidalArtistId?.let { "https://tidal.com/browse/artist/$it" }
            ?: "https://tidal.com/browse/search?q=${enc(concert.artist)}"
        INSTAGRAM -> concert.instagramHandle?.let { "https://www.instagram.com/$it/" }
            ?: "https://www.instagram.com/explore/tags/${enc(concert.artist.replace(" ", ""))}"
        FACEBOOK -> concert.facebookHandle?.let { "https://www.facebook.com/$it" }
            ?: "https://www.facebook.com/search/top?q=${enc(concert.artist)}"
        X -> concert.twitterHandle?.let { "https://x.com/$it" }
            ?: "https://x.com/search?q=${enc(concert.artist)}"
        TIKTOK -> concert.tiktokHandle?.let { "https://www.tiktok.com/@$it" }
            ?: "https://www.tiktok.com/search?q=${enc(concert.artist)}"
    }
}

private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

/** First-time default — single-tap experience without overwhelming the user with 9 buttons. */
val DEFAULT_ENABLED_PROVIDERS: Set<ProviderId> = setOf(ProviderId.SPOTIFY, ProviderId.INSTAGRAM)
