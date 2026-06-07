package com.mc.mateamhf.data.groups

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mc.mateamhf.domain.DEFAULT_FESTIVAL_ID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GroupRepository {

    private val db = Firebase.firestore
    private fun groupDoc(id: String) = db.collection("groups").document(id)
    private fun joinCodeDoc(code: String) = db.collection("joinCodes").document(code)
    private fun userDoc(uid: String) = db.collection("users").document(uid)

    sealed class CreateResult {
        data class Success(val group: Group) : CreateResult()
        data class Failure(val message: String) : CreateResult()
    }

    sealed class JoinResult {
        data class Success(val group: Group) : JoinResult()
        data object NotFound : JoinResult()
        data class Failure(val message: String) : JoinResult()
    }

    /** Create a group, auto-generate a unique join code, add creator as owner member. */
    suspend fun createGroup(user: FirebaseUser, name: String): CreateResult {
        repeat(8) {
            val code = JoinCode.generate()
            try {
                // Reserve join code first — fails atomically if already taken
                val groupRef = db.collection("groups").document()
                joinCodeDoc(code).set(mapOf(
                    "groupId" to groupRef.id,
                    "createdAt" to FieldValue.serverTimestamp(),
                )).await()
                groupRef.set(mapOf(
                    "name" to name.trim(),
                    "ownerUid" to user.uid,
                    "joinCode" to code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "memberCount" to 1,
                    "festivalIds" to listOf(DEFAULT_FESTIVAL_ID),
                )).await()
                groupRef.collection("members").document(user.uid).set(mapOf(
                    "displayName" to (user.displayName ?: user.email ?: "Anonyme"),
                    "photoUrl" to user.photoUrl?.toString(),
                    "role" to "owner",
                    "joinedAt" to FieldValue.serverTimestamp(),
                )).await()
                userDoc(user.uid).set(
                    mapOf("groupIds" to FieldValue.arrayUnion(groupRef.id)),
                    com.google.firebase.firestore.SetOptions.merge(),
                ).await()
                return CreateResult.Success(
                    Group(
                        id = groupRef.id,
                        name = name.trim(),
                        ownerUid = user.uid,
                        joinCode = code,
                        memberCount = 1,
                        festivalIds = listOf(DEFAULT_FESTIVAL_ID),
                    )
                )
            } catch (e: Exception) {
                // Likely a code collision on the joinCodes/create — retry with a new code
                if (it == 7) return CreateResult.Failure(e.message ?: e::class.simpleName.orEmpty())
            }
        }
        return CreateResult.Failure("Trop de collisions de code, réessaie")
    }

    /** Join an existing group by code. Idempotent — re-joining is a no-op success. */
    suspend fun joinGroup(user: FirebaseUser, rawCode: String): JoinResult {
        val code = JoinCode.normalize(rawCode)
        if (code.length != 9) return JoinResult.NotFound // "XXXX-XXXX" = 9 chars
        return try {
            val codeSnap = joinCodeDoc(code).get().await()
            val groupId = codeSnap.getString("groupId") ?: return JoinResult.NotFound
            val groupSnap = groupDoc(groupId).get().await()
            if (!groupSnap.exists()) return JoinResult.NotFound
            val group = groupSnap.toGroup()
            groupDoc(groupId).collection("members").document(user.uid).set(mapOf(
                "displayName" to (user.displayName ?: user.email ?: "Anonyme"),
                "photoUrl" to user.photoUrl?.toString(),
                "role" to "member",
                "joinedAt" to FieldValue.serverTimestamp(),
            ), com.google.firebase.firestore.SetOptions.merge()).await()
            groupDoc(groupId).update("memberCount", FieldValue.increment(1)).await()
            userDoc(user.uid).set(
                mapOf("groupIds" to FieldValue.arrayUnion(groupId)),
                com.google.firebase.firestore.SetOptions.merge(),
            ).await()
            JoinResult.Success(group)
        } catch (e: Exception) {
            JoinResult.Failure(e.message ?: e::class.simpleName.orEmpty())
        }
    }

    /** Observe a single group's metadata. */
    fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val reg = groupDoc(groupId).addSnapshotListener(MetadataChanges.EXCLUDE) { snap, err ->
            if (err != null || snap == null || !snap.exists()) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap.toGroup())
        }
        awaitClose { reg.remove() }
    }

    /** Observe all members of a group. */
    fun observeMembers(groupId: String): Flow<List<GroupMember>> = callbackFlow {
        val reg = groupDoc(groupId).collection("members")
            .addSnapshotListener(MetadataChanges.EXCLUDE) { snap, err ->
                if (err != null || snap == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap.documents.mapNotNull { it.toMember() })
            }
        awaitClose { reg.remove() }
    }

    private fun DocumentSnapshot.toGroup(): Group {
        val rawFestivalIds = get("festivalIds") as? List<*>
        val festivalIds = rawFestivalIds?.filterIsInstance<String>()?.takeIf { it.isNotEmpty() }
            ?: listOf(DEFAULT_FESTIVAL_ID) // legacy docs created before multi-festival
        return Group(
            id = id,
            name = getString("name").orEmpty(),
            ownerUid = getString("ownerUid").orEmpty(),
            joinCode = getString("joinCode").orEmpty(),
            memberCount = getLong("memberCount")?.toInt() ?: 0,
            festivalIds = festivalIds,
        )
    }

    /** Add a festival to an existing group. Owner-only (enforced by Firestore rules). */
    suspend fun addFestivalToGroup(groupId: String, festivalId: String) {
        groupDoc(groupId).update("festivalIds", FieldValue.arrayUnion(festivalId)).await()
    }

    /** Remove a festival from a group. Owner-only (enforced by Firestore rules). */
    suspend fun removeFestivalFromGroup(groupId: String, festivalId: String) {
        groupDoc(groupId).update("festivalIds", FieldValue.arrayRemove(festivalId)).await()
    }

    private fun DocumentSnapshot.toMember(): GroupMember? {
        val name = getString("displayName") ?: return null
        return GroupMember(
            uid = id,
            displayName = name,
            photoUrl = getString("photoUrl"),
            role = getString("role") ?: "member",
        )
    }
}
