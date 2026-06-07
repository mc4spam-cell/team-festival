package com.mc.mateamhf.data.events

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mc.mateamhf.domain.TeamEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant

private const val TAG = "TeamEventRepo"

/** Personal events created by team members, scoped per festival. */
class TeamEventRepository {

    private val db = Firebase.firestore
    private fun eventsColl(groupId: String) =
        db.collection("groups").document(groupId).collection("events")

    /** Live stream of events for [groupId] filtered to [festivalId]. */
    fun observe(groupId: String?, festivalId: String?): Flow<List<TeamEvent>> {
        if (groupId == null || festivalId == null) return flowOf(emptyList())
        Log.d(TAG, "observe groupId=$groupId festivalId=$festivalId")
        return callbackFlow {
            // No orderBy — it pairs with whereEqualTo on a DIFFERENT field, which would require a
            // composite index. Sorting client-side is fine for the team-event volumes we expect.
            val reg = eventsColl(groupId)
                .whereEqualTo("festivalId", festivalId)
                .addSnapshotListener(MetadataChanges.EXCLUDE) { snap, err ->
                    if (err != null) {
                        Log.w(TAG, "observe error: ${err.message}", err)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snap == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val parsed = snap.documents.mapNotNull { doc ->
                        val title = doc.getString("title") ?: return@mapNotNull null
                        val start = doc.getString("start") ?: return@mapNotNull null
                        val end = doc.getString("end") ?: return@mapNotNull null
                        val fid = doc.getString("festivalId") ?: return@mapNotNull null
                        TeamEvent(
                            id = doc.id,
                            creatorUid = doc.getString("creatorUid").orEmpty(),
                            creatorName = doc.getString("creatorName") ?: "Anonyme",
                            title = title,
                            location = doc.getString("location"),
                            start = runCatching { Instant.parse(start) }.getOrNull() ?: return@mapNotNull null,
                            end = runCatching { Instant.parse(end) }.getOrNull() ?: return@mapNotNull null,
                            festivalId = fid,
                        )
                    }.sortedBy { it.start }
                    Log.d(TAG, "snapshot: ${snap.size()} docs → ${parsed.size} events for festival $festivalId")
                    trySend(parsed)
                }
            awaitClose { reg.remove() }
        }
    }

    suspend fun create(
        groupId: String,
        user: FirebaseUser,
        title: String,
        location: String?,
        startIso: String,
        endIso: String,
        festivalId: String,
    ) {
        Log.d(TAG, "create groupId=$groupId festivalId=$festivalId start=$startIso title=$title")
        val ref = eventsColl(groupId).add(mapOf(
            "creatorUid" to user.uid,
            "creatorName" to (user.displayName ?: user.email ?: "Anonyme"),
            "title" to title.trim(),
            "location" to location?.trim()?.takeIf { it.isNotEmpty() },
            "start" to startIso,
            "end" to endIso,
            "festivalId" to festivalId,
            "createdAt" to FieldValue.serverTimestamp(),
        )).await()
        Log.d(TAG, "created event id=${ref.id}")
    }

    /** Delete an event. Security rules restrict this to the creator. */
    suspend fun delete(groupId: String, eventId: String) {
        eventsColl(groupId).document(eventId).delete().await()
    }
}
