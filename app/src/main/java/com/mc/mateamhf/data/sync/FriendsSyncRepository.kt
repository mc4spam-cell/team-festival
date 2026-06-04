package com.mc.mateamhf.data.sync

import com.mc.mateamhf.BuildConfig
import com.mc.mateamhf.data.ConcertStateRepository
import com.mc.mateamhf.data.RunningOrderRepository
import com.mc.mateamhf.data.db.FriendPickDao
import com.mc.mateamhf.data.db.FriendPickEntity
import com.mc.mateamhf.domain.Priority
import com.mc.mateamhf.domain.artistKey
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class FriendsSyncRepository(
    private val runningOrderRepo: RunningOrderRepository,
    private val stateRepo: ConcertStateRepository,
    private val friendDao: FriendPickDao,
    private val syncUrl: String = BuildConfig.SYNC_URL,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val isConfigured: Boolean get() = syncUrl.isNotBlank()

    /** GET remote state, replace `friend_pick` table excluding our own pseudo. */
    suspend fun pull(myPseudo: String?): SyncResult = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext SyncResult.NotConfigured
        runCatching {
            val req = Request.Builder().url(syncUrl).get().build()
            val body = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
            val state = json.decodeFromString<RemoteState>(body)
            val rows = state.users
                .filterKeys { name -> myPseudo == null || !name.equals(myPseudo, ignoreCase = true) }
                .flatMap { (name, user) ->
                    user.p1Artists.map { artist ->
                        FriendPickEntity(
                            friendName = name,
                            artistKey = artistKey(artist),
                            artistDisplay = artist,
                        )
                    }
                }
            friendDao.replaceAll(rows)
            SyncResult.Pulled(friends = state.users.keys.size, p1s = rows.size)
        }.getOrElse { SyncResult.Failed(it.message ?: it::class.simpleName ?: "unknown") }
    }

    /** Read our P1 artists from local state and push to remote. Then mirror response. */
    suspend fun push(myPseudo: String): SyncResult = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext SyncResult.NotConfigured
        if (myPseudo.isBlank()) return@withContext SyncResult.Failed("no pseudo")
        runCatching {
            val concertsById = runningOrderRepo.load().days.flatMap { it.concerts }.associateBy { it.id }
            val states = stateRepo.observeStates().first()
            val myP1Artists = states.values
                .filter { it.priority == Priority.P1.value }
                .mapNotNull { concertsById[it.concertId]?.artist }
                .distinct()
                .sorted()

            val payload = PushBody(user = myPseudo, p1Artists = myP1Artists)
            val req = Request.Builder()
                .url(syncUrl)
                .post(json.encodeToString(payload).toRequestBody(jsonMediaType))
                .build()
            val body = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
            val state = json.decodeFromString<RemoteState>(body)
            val rows = state.users
                .filterKeys { !it.equals(myPseudo, ignoreCase = true) }
                .flatMap { (name, user) ->
                    user.p1Artists.map { artist ->
                        FriendPickEntity(
                            friendName = name,
                            artistKey = artistKey(artist),
                            artistDisplay = artist,
                        )
                    }
                }
            friendDao.replaceAll(rows)
            SyncResult.Pushed(myP1Count = myP1Artists.size, friends = state.users.keys.size - 1)
        }.getOrElse { SyncResult.Failed(it.message ?: it::class.simpleName ?: "unknown") }
    }
}

sealed interface SyncResult {
    data object NotConfigured : SyncResult
    data class Pulled(val friends: Int, val p1s: Int) : SyncResult
    data class Pushed(val myP1Count: Int, val friends: Int) : SyncResult
    data class Failed(val message: String) : SyncResult
}
