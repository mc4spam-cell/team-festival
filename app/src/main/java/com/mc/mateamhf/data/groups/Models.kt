package com.mc.mateamhf.data.groups

data class UserProfile(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val groupIds: List<String> = emptyList(),
)

data class Group(
    val id: String,
    val name: String,
    val ownerUid: String,
    val joinCode: String,
    val memberCount: Int = 0,
    /** Festivals this team follows. Legacy docs without this field fall back to [DEFAULT_FESTIVAL_ID]. */
    val festivalIds: List<String> = emptyList(),
)

data class GroupMember(
    val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val role: String, // "owner" | "member"
)

data class GroupPick(
    val uid: String,
    val displayName: String,
    val p1Artists: List<String>,
)
