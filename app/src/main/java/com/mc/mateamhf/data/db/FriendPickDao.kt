package com.mc.mateamhf.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendPickDao {

    @Query("SELECT * FROM friend_pick")
    fun observeAll(): Flow<List<FriendPickEntity>>

    @Query("DELETE FROM friend_pick WHERE friendName = :friendName")
    suspend fun deleteByFriend(friendName: String)

    @Query("DELETE FROM friend_pick")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(picks: List<FriendPickEntity>)

    @Transaction
    suspend fun replaceForFriend(friendName: String, picks: List<FriendPickEntity>) {
        deleteByFriend(friendName)
        if (picks.isNotEmpty()) insertAll(picks)
    }

    @Transaction
    suspend fun replaceAll(picks: List<FriendPickEntity>) {
        deleteAll()
        if (picks.isNotEmpty()) insertAll(picks)
    }
}
