package com.mc.mateamhf.domain

enum class Priority(val value: Int, val label: String) {
    NONE(0, "—"),
    P1(1, "P1"),
    P2(2, "P2"),
    P3(3, "P3");

    companion object {
        fun of(value: Int): Priority = entries.firstOrNull { it.value == value } ?: NONE
    }
}
