package com.mc.mateamhf.domain

enum class Rating(val label: String) {
    MINUS_MINUS("--"),
    MINUS("-"),
    PLUS("+"),
    PLUS_PLUS("++");

    companion object {
        fun fromStorage(value: String?): Rating? = value?.let { v -> entries.firstOrNull { it.name == v } }
    }
}
