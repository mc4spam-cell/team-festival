package com.mc.mateamhf.data.picks

import android.content.Context
import android.net.Uri
import com.mc.mateamhf.data.ConcertStateRepository
import com.mc.mateamhf.data.RunningOrderRepository
import com.mc.mateamhf.domain.Priority
import com.mc.mateamhf.domain.Rating
import com.mc.mateamhf.domain.artistKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PicksBackup(
    private val context: Context,
    private val runningOrderRepo: RunningOrderRepository,
    private val stateRepo: ConcertStateRepository,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    data class ExportResult(val written: Int)
    data class ImportResult(val matched: Int, val unmatched: Int, val unmatchedExamples: List<String>)

    suspend fun export(uri: Uri): ExportResult = withContext(Dispatchers.IO) {
        val concertsById = runningOrderRepo.load().days
            .flatMap { it.concerts }
            .associateBy { it.id }
        val states = stateRepo.observeStates().first()

        // One pick per artist. If the same artist plays twice (shouldn't happen at Hellfest)
        // we keep the strongest signal: max non-zero priority + first non-null rating.
        val picksByArtist = mutableMapOf<String, PickDto>()
        for (state in states.values) {
            if (state.priority == 0 && state.rating == null) continue
            val concert = concertsById[state.concertId] ?: continue
            val artist = concert.artist
            val existing = picksByArtist[artist]
            picksByArtist[artist] = PickDto(
                artist = artist,
                priority = bestPriority(existing?.priority ?: 0, state.priority),
                rating = state.rating ?: existing?.rating,
            )
        }

        val picks = picksByArtist.values.sortedBy { it.artist.lowercase() }
        val payload = PicksFile(exportedAt = Clock.System.now().toString(), picks = picks)
        val bytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: error("Cannot open output stream for $uri")
        ExportResult(written = picks.size)
    }

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("Cannot read $uri")
        val payload = json.decodeFromString<PicksFile>(raw)

        // Index current concerts by normalized artist name (case + accent insensitive),
        // so "Skáld" matches "SKALD", "Bring Me The Horizon" matches "bring me the horizon".
        val concertsByArtistKey = runningOrderRepo.load().days
            .flatMap { it.concerts }
            .groupBy { artistKey(it.artist) }

        var matched = 0
        var unmatched = 0
        val examples = mutableListOf<String>()

        for (pick in payload.picks) {
            val matches = concertsByArtistKey[artistKey(pick.artist)]
            if (matches.isNullOrEmpty()) {
                unmatched++
                if (examples.size < 5) examples += pick.artist
                continue
            }
            for (concert in matches) {
                if (pick.priority != 0) {
                    stateRepo.setPriority(concert.id, Priority.of(pick.priority))
                }
                val rating = Rating.fromStorage(pick.rating)
                if (rating != null) {
                    stateRepo.setRating(concert.id, rating)
                }
            }
            matched++
        }
        ImportResult(matched, unmatched, examples)
    }

    private fun bestPriority(a: Int, b: Int): Int = when {
        a == 0 -> b
        b == 0 -> a
        else -> minOf(a, b) // 1 (highest) wins over 3 (lowest)
    }
}
