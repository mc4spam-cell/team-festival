package com.mc.mateamhf.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConcertStateDao {

    @Query("SELECT * FROM concert_state")
    fun observeAll(): Flow<List<ConcertStateEntity>>

    @Upsert
    suspend fun upsert(state: ConcertStateEntity)

    @Query("UPDATE concert_state SET priority = :priority, updatedAt = :now WHERE concertId = :id")
    suspend fun updatePriority(id: String, priority: Int, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE concert_state SET rating = :rating, updatedAt = :now WHERE concertId = :id")
    suspend fun updateRating(id: String, rating: String?, now: Long = System.currentTimeMillis()): Int
}
