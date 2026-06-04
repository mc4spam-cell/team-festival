package com.mc.mateamhf.domain

import java.text.Normalizer

/** Fold artist name to a canonical comparison key: NFKD + strip diacritics + lowercase + alphanumerics only. */
fun artistKey(name: String): String =
    Normalizer.normalize(name, Normalizer.Form.NFKD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()
        .replace("[^a-z0-9]+".toRegex(), "")
