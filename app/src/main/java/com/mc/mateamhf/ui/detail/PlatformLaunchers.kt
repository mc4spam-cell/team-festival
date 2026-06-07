package com.mc.mateamhf.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.mc.mateamhf.data.providers.ProviderId
import com.mc.mateamhf.domain.Concert

/**
 * Open this provider for the given concert. Prefers the installed app (via `setPackage`) and
 * gracefully falls back to whatever can handle the URL if the app is missing.
 */
fun openForProvider(context: Context, provider: ProviderId, concert: Concert) {
    val url = provider.urlFor(concert)
    val uri = url.toUri()
    val pkg = provider.androidPackage
    if (pkg != null) {
        val targeted = Intent(Intent.ACTION_VIEW, uri).setPackage(pkg)
        try {
            context.startActivity(targeted)
            return
        } catch (_: ActivityNotFoundException) {
            // App not installed — fall through to a generic web open
        }
    }
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}
