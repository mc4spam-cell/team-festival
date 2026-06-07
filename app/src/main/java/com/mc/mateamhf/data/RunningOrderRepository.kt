package com.mc.mateamhf.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.domain.DEFAULT_FESTIVAL_ID
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

    /** Per-festival cache. Festival running orders never change at runtime, so a simple map is fine. */
    private val cached: MutableMap<String, RunningOrder> = mutableMapOf()

    /** Legacy default — loads the single hardcoded festival. Kept for callers that don't know about festivals yet. */
    suspend fun load(): RunningOrder = load(DEFAULT_FESTIVAL_ID)

    suspend fun load(festivalId: String): RunningOrder {
        cached[festivalId]?.let { return it }
        return withContext(Dispatchers.IO) {
            cached.getOrPut(festivalId) { parseAsset(festivalId) }
        }
    }

    private fun parseAsset(festivalId: String): RunningOrder {
        val raw = appContext.assets.open("festivals/$festivalId.json")
            .bufferedReader().use { it.readText() }
        val dto = json.decodeFromString<RunningOrderDto>(raw)

        // Build the stage lookup — one Stage instance per id, reused across concerts.
        val stageById: Map<String, Stage> = dto.stages.associate { s ->
            s.id to Stage(id = s.id, name = s.name, color = parseHex(s.color))
        }

        val concertsByDay = dto.concerts
            // Drop concerts with no time (Incomplet festivals) — the timeline can't position them.
            .filter { it.start != null && it.end != null }
            // Drop concerts whose stage isn't declared in the festival's stages list.
            .filter { stageById.containsKey(it.stage) }
            .groupBy { it.festivalDay }

        val days = dto.days.map { d ->
            val concerts = (concertsByDay[d.id] ?: emptyList()).map { c ->
                Concert(
                    id = c.id,
                    artist = c.artist,
                    stage = stageById.getValue(c.stage),
                    festivalDay = c.festivalDay,
                    start = Instant.parse(c.start!!),
                    end = Instant.parse(c.end!!),
                    appleMusicPlaylist = c.appleMusicPlaylist,
                    appleMusicArtistId = c.appleMusicArtistId,
                    spotifyArtistId = c.spotifyArtistId,
                    deezerArtistId = c.deezerArtistId,
                    qobuzArtistId = c.qobuzArtistId,
                    tidalArtistId = c.tidalArtistId,
                    instagramHandle = c.instagramHandle,
                    facebookHandle = c.facebookHandle,
                    twitterHandle = c.twitterHandle,
                    tiktokHandle = c.tiktokHandle,
                )
            }.sortedBy { it.start }
            FestivalDay(id = d.id, label = d.label, date = d.date, concerts = concerts)
        }.sortedBy { it.id }

        return RunningOrder(
            festival = dto.festival,
            stages = stageById.values.toList(),
            days = days,
        )
    }

    private fun parseHex(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        Color(0xFF6E6E6E)
    }
}

@Serializable
private data class RunningOrderDto(
    val festival: String,
    val timezone: String,
    val stages: List<StageDto> = emptyList(),
    val days: List<DayDto>,
    val concerts: List<ConcertDto>,
)

@Serializable
private data class StageDto(
    val id: String,
    val name: String,
    val color: String,
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
    val start: String? = null,
    val end: String? = null,
    val appleMusicPlaylist: String? = null,
    val appleMusicArtistId: Long? = null,
    val spotifyArtistId: String? = null,
    val deezerArtistId: Long? = null,
    val qobuzArtistId: String? = null,
    val tidalArtistId: String? = null,
    val instagramHandle: String? = null,
    val facebookHandle: String? = null,
    val twitterHandle: String? = null,
    val tiktokHandle: String? = null,
)
