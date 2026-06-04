package com.mc.mateamhf.data.db

import androidx.room.Entity

@Entity(
    tableName = "friend_pick",
    primaryKeys = ["friendName", "artistKey"],
)
data class FriendPickEntity(
    val friendName: String,
    val artistKey: String,
    val artistDisplay: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
