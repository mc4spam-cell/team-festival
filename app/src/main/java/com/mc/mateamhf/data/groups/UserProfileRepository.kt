package com.mc.mateamhf.data.groups

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserProfileRepository {
    private val db = Firebase.firestore
    private fun userDoc(uid: String) = db.collection("users").document(uid)

    /** Idempotent: create or update the user's profile doc on every sign-in. */
    suspend fun ensureProfile(user: FirebaseUser) {
        val payload = mapOf(
            "email" to user.email,
            "displayName" to (user.displayName ?: user.email?.substringBefore('@') ?: "Anonyme"),
            "photoUrl" to user.photoUrl?.toString(),
            "lastSeenAt" to FieldValue.serverTimestamp(),
        )
        userDoc(user.uid).set(payload, SetOptions.merge()).await()
    }

    /** Live profile (including its groupIds array). Emits null while loading. */
    fun observe(uid: String): Flow<UserProfile?> = callbackFlow {
        val reg = userDoc(uid).addSnapshotListener(MetadataChanges.EXCLUDE) { snap, err ->
            if (err != null || snap == null) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap.toProfile(uid))
        }
        awaitClose { reg.remove() }
    }

    private fun DocumentSnapshot.toProfile(uid: String): UserProfile = UserProfile(
        uid = uid,
        email = getString("email"),
        displayName = getString("displayName"),
        photoUrl = getString("photoUrl"),
        groupIds = (get("groupIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
    )
}
