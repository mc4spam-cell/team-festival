package com.mc.mateamhf.data.groups

import kotlin.random.Random

/** 8-char user-friendly code, formatted XXXX-XXXX. No 0/O/1/I/L to avoid confusion. */
object JoinCode {
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun generate(random: Random = Random.Default): String {
        val chars = (1..8).map { ALPHABET.random(random) }
        return chars.joinToString("").chunked(4).joinToString("-")
    }

    /** Normalize user-typed code: uppercase, strip whitespace, ensure single dash mid. */
    fun normalize(input: String): String {
        val cleaned = input.uppercase()
            .filter { it.isLetterOrDigit() }
            .take(8)
        return if (cleaned.length == 8) "${cleaned.substring(0, 4)}-${cleaned.substring(4, 8)}"
        else cleaned
    }
}
