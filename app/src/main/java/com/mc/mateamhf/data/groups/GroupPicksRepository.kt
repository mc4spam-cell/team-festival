package com.mc.mateamhf.data.groups

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Picks (P1 artists) per user within a group. */
class GroupPicksRepository {

    private val db = Firebase.firestore
    private fun picksColl(groupId: String) = db.collection("groups").document(groupId).collection("picks")

    /** Real-time stream of all members' picks in a group. */
    fun observe(groupId: String): Flow<List<GroupPick>> = callbackFlow {
        val reg = picksColl(groupId).addSnapshotListener(MetadataChanges.EXCLUDE) { snap, err ->
            if (err != null || snap == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snap.documents.mapNotNull { doc ->
                val displayName = doc.getString("displayName") ?: return@mapNotNull null
                val artists = (doc.get("p1Artists") as? List<*>)?.filterIsInstance<String>().orEmpty()
                GroupPick(uid = doc.id, displayName = displayName, p1Artists = artists)
            })
        }
        awaitClose { reg.remove() }
    }

    /** Idempotent upsert of the current user's P1 list in a group. */
    suspend fun setMyP1Artists(groupId: String, user: FirebaseUser, p1Artists: List<String>) {
        picksColl(groupId).document(user.uid).set(
            mapOf(
                "displayName" to (user.displayName ?: user.email ?: "Anonyme"),
                "p1Artists" to p1Artists,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }
}
