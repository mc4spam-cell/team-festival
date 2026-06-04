package com.mc.mateamhf.data

import android.content.Context
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.domain.FestivalDay
import com.mc.mateamhf.domain.RunningOrder
import com.mc.mateamhf.domain.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class RunningOrderRepository(private val appContext: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var cached: RunningOrder? = null

    suspend fun load(): RunningOrder = cached ?: withContext(Dispatchers.IO) {
        cached ?: parseAsset().also { cached = it }
    }

    private fun parseAsset(): RunningOrder {
        val raw = appContext.assets.open("running_order.json").bufferedReader().use { it.readText() }
        val dto = json.decodeFromString<RunningOrderDto>(raw)
        val concertsByDay = dto.concerts.groupBy { it.festivalDay }
        val days = dto.days.map { d ->
            val concerts = (concertsByDay[d.id] ?: emptyList()).map { c ->
                Concert(
                    id = c.id,
                    artist = c.artist,
                    stage = Stage.valueOf(c.stage),
                    festivalDay = c.festivalDay,
                    start = Instant.parse(c.start),
                    end = Instant.parse(c.end),
                    appleMusicPlaylist = c.appleMusicPlaylist,
                    appleMusicArtistId = c.appleMusicArtistId,
                    spotifyArtistId = c.spotifyArtistId,
                    deezerArtistId = c.deezerArtistId,
                    instagramHandle = c.instagramHandle,
                )
            }.sortedBy { it.start }
            FestivalDay(id = d.id, label = d.label, concerts = concerts)
        }.sortedBy { it.id }
        return RunningOrder(festival = dto.festival, days = days)
    }
}

@Serializable
private data class RunningOrderDto(
    val festival: String,
    val timezone: String,
    val days: List<DayDto>,
    val concerts: List<ConcertDto>,
)

@Serializable
private data class DayDto(
    val id: Int,
    val label: String,
    val date: String,
)

@Serializable
private data class ConcertDto(
    val id: String,
    val artist: String,
    val stage: String,
    val festivalDay: Int,
    val start: String,
    val end: String,
    val appleMusicPlaylist: String? = null,
    val appleMusicArtistId: Long? = null,
    val spotifyArtistId: String? = null,
    val deezerArtistId: Long? = null,
    val instagramHandle: String? = null,
)
