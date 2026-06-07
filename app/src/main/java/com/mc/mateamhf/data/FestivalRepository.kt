package com.mc.mateamhf.data

import android.content.Context
import com.mc.mateamhf.domain.FestivalMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Lists known festivals shipped in `assets/festivals/index.json`. */
class FestivalRepository(private val appContext: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var cached: List<FestivalMeta>? = null

    suspend fun list(): List<FestivalMeta> = cached ?: withContext(Dispatchers.IO) {
        cached ?: parse().also { cached = it }
    }

    private fun parse(): List<FestivalMeta> {
        val raw = appContext.assets.open("festivals/index.json").bufferedReader().use { it.readText() }
        return json.decodeFromString<FestivalsIndexDto>(raw).festivals
    }
}

@Serializable
private data class FestivalsIndexDto(val festivals: List<FestivalMeta>)
