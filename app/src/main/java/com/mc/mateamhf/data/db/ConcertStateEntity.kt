package com.mc.mateamhf.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "concert_state")
data class ConcertStateEntity(
    @PrimaryKey val concertId: String,
    val priority: Int = 0,
    val rating: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
