package com.mc.mateamhf.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mc.mateamhf.MaTeamHFApp
import com.mc.mateamhf.data.auth.AuthRepository
import com.mc.mateamhf.data.groups.Group
import com.mc.mateamhf.data.groups.GroupRepository
import com.mc.mateamhf.data.groups.UserProfile
import com.mc.mateamhf.data.groups.UserProfileRepository
import com.mc.mateamhf.data.prefs.UserPrefs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModel(
    private val authRepo: AuthRepository,
    private val userProfileRepo: UserProfileRepository,
    private val groupRepo: GroupRepository,
    private val userPrefs: UserPrefs,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data object NoGroups : State
        data class HasGroups(
            val groups: List<Group>,
            val currentGroupId: String?,
        ) : State {
            val currentGroup: Group? = groups.firstOrNull { it.id == currentGroupId }
        }
    }

    private val _toasts = Channel<String>(Channel.BUFFERED)
    val toasts: Flow<String> = _toasts.receiveAsFlow()

    private val profileFlow: Flow<UserProfile?> = authRepo.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else userProfileRepo.observe(user.uid)
        }

    private val groupsFlow: Flow<List<Group>> = profileFlow
        .flatMapLatest { profile ->
            val ids = profile?.groupIds.orEmpty()
            if (ids.isEmpty()) flowOf(emptyList())
            else combine(ids.map { groupRepo.observeGroup(it) }) { arr ->
                arr.toList().filterNotNull()
            }
        }

    val state: StateFlow<State> = combine(
        authRepo.currentUser,
        groupsFlow,
        userPrefs.currentGroupId,
    ) { user, groups, persisted ->
        when {
            user == null -> State.Loading
            groups.isEmpty() -> State.NoGroups
            else -> {
                val currentId = persisted?.takeIf { id -> groups.any { it.id == id } }
                    ?: groups.firstOrNull()?.id
                State.HasGroups(groups, currentId)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    init {
        // Idempotent profile upsert on every sign-in so the user doc always exists.
        viewModelScope.launch {
            authRepo.currentUser.collect { user ->
                if (user != null) runCatching { userProfileRepo.ensureProfile(user) }
            }
        }
        // Persist whatever group the derived state ended up choosing — keeps
        // TimelineViewModel's read of userPrefs.currentGroupId in sync with the UI.
        viewModelScope.launch {
            state.collect { s ->
                if (s is State.HasGroups && s.currentGroupId != null) {
                    userPrefs.setCurrentGroupId(s.currentGroupId)
                }
            }
        }
    }

    fun selectGroup(groupId: String) {
        viewModelScope.launch { userPrefs.setCurrentGroupId(groupId) }
    }

    fun createGroup(name: String) {
        if (name.isBlank()) {
            viewModelScope.launch { _toasts.send("Nom de team requis") }
            return
        }
        val user = authRepo.currentUserOrNull ?: return
        viewModelScope.launch {
            when (val r = groupRepo.createGroup(user, name)) {
                is GroupRepository.CreateResult.Success -> {
                    userPrefs.setCurrentGroupId(r.group.id)
                    _toasts.send("Team créée — code ${r.group.joinCode}")
                }
                is GroupRepository.CreateResult.Failure -> _toasts.send("Création KO : ${r.message}")
            }
        }
    }

    fun joinGroup(code: String) {
        if (code.isBlank()) {
            viewModelScope.launch { _toasts.send("Code requis") }
            return
        }
        val user = authRepo.currentUserOrNull ?: return
        viewModelScope.launch {
            when (val r = groupRepo.joinGroup(user, code)) {
                is GroupRepository.JoinResult.Success -> {
                    userPrefs.setCurrentGroupId(r.group.id)
                    _toasts.send("Rejoint « ${r.group.name} »")
                }
                GroupRepository.JoinResult.NotFound -> _toasts.send("Code introuvable")
                is GroupRepository.JoinResult.Failure -> _toasts.send("Erreur : ${r.message}")
            }
        }
    }

    companion object {
        fun factory(app: MaTeamHFApp): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GroupsViewModel(
                    authRepo = app.authRepository,
                    userProfileRepo = app.userProfileRepository,
                    groupRepo = app.groupRepository,
                    userPrefs = app.userPrefs,
                )
            }
        }
    }
}
