package com.mc.mateamhf.data

import com.mc.mateamhf.data.db.ConcertStateDao
import com.mc.mateamhf.data.db.ConcertStateEntity
import com.mc.mateamhf.domain.Priority
import com.mc.mateamhf.domain.Rating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConcertStateRepository(private val dao: ConcertStateDao) {

    fun observeStates(): Flow<Map<String, ConcertStateEntity>> =
        dao.observeAll().map { list -> list.associateBy { it.concertId } }

    suspend fun setPriority(concertId: String, priority: Priority) {
        if (dao.updatePriority(concertId, priority.value) == 0) {
            dao.upsert(ConcertStateEntity(concertId = concertId, priority = priority.value))
        }
    }

    suspend fun setRating(concertId: String, rating: Rating?) {
        val stored = rating?.name
        if (dao.updateRating(concertId, stored) == 0) {
            dao.upsert(ConcertStateEntity(concertId = concertId, rating = stored))
        }
    }
}
