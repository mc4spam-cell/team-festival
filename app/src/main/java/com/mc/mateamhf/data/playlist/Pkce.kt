package com.mc.mateamhf.data.playlist

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * PKCE = Proof Key for Code Exchange (RFC 7636). Lets us safely do the OAuth
 * authorization-code flow on a mobile client without embedding a client secret.
 *
 * Flow:
 *   1. Generate a high-entropy random `codeVerifier`.
 *   2. Send its SHA-256 → base64url as `code_challenge` in the /authorize URL.
 *   3. Keep the `codeVerifier` locally.
 *   4. After redirect, send the `codeVerifier` to /token to prove we are the same client.
 */
data class PkceChallenge(val codeVerifier: String, val codeChallenge: String) {
    val codeChallengeMethod: String = "S256"
}

object Pkce {

    private const val VERIFIER_BYTES = 64  // 64 bytes ≈ 86 chars after base64url, well within RFC range

    fun generate(): PkceChallenge {
        val verifier = randomBase64Url(VERIFIER_BYTES)
        val challenge = sha256Base64Url(verifier)
        return PkceChallenge(codeVerifier = verifier, codeChallenge = challenge)
    }

    private fun randomBase64Url(byteLen: Int): String {
        val buf = ByteArray(byteLen)
        SecureRandom().nextBytes(buf)
        return Base64.encodeToString(
            buf,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
    }

    private fun sha256Base64Url(input: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(
            hash,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
    }
}
