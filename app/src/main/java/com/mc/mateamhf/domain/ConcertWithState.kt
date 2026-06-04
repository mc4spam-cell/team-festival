package com.mc.mateamhf.domain

data class ConcertWithState(
    val concert: Concert,
    val priority: Priority,
    val rating: Rating?,
)
