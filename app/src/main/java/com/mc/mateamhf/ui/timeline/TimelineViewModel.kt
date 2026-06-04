package com.mc.mateamhf.ui.timeline

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mc.mateamhf.MaTeamHFApp
import com.mc.mateamhf.data.ConcertStateRepository
import com.mc.mateamhf.data.RunningOrderRepository
import com.mc.mateamhf.data.auth.AuthRepository
import com.mc.mateamhf.data.groups.GroupPick
import com.mc.mateamhf.data.groups.GroupPicksRepository
import com.mc.mateamhf.data.picks.PicksBackup
import com.mc.mateamhf.data.prefs.UserPrefs
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.domain.ConcertWithState
import com.mc.mateamhf.domain.Priority
import com.mc.mateamhf.domain.Rating
import com.mc.mateamhf.domain.artistKey
import com.mc.mateamhf.notification.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val runningOrderRepo: RunningOrderRepository,
    private val stateRepo: ConcertStateRepository,
    private val scheduler: ReminderScheduler,
    private val picksBackup: PicksBackup,
    private val userPrefs: UserPrefs,
    private val authRepo: AuthRepository,
    private val groupPicksRepo: GroupPicksRepository,
) : ViewModel() {

    private val _toasts = Channel<String>(Channel.BUFFERED)
    val toasts: Flow<String> = _toasts.receiveAsFlow()

    /** Live picks of every member of the current group (including self). */
    private val currentGroupPicks: Flow<List<GroupPick>> = combine(
        userPrefs.currentGroupId,
        authRepo.currentUser,
    ) { groupId, user -> groupId to user?.uid }
        .flatMapLatest { (groupId, _) ->
            if (groupId == null) flowOf(emptyList())
            else groupPicksRepo.observe(groupId)
        }

    /** Map from artistKey → list of friend display names who marked this artist as P1 (excluding self). */
    val friendsByArtist: StateFlow<Map<String, List<String>>> = combine(
        currentGroupPicks,
        authRepo.currentUser,
    ) { picks, user ->
        val myUid = user?.uid
        val out = mutableMapOf<String, MutableList<String>>()
        picks.forEach { pick ->
            if (pick.uid == myUid) return@forEach
            pick.p1Artists.forEach { artist ->
                val key = artistKey(artist)
                out.getOrPut(key) { mutableListOf() }.add(pick.displayName)
            }
        }
        out.mapValues { (_, v) -> v.sorted() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val state: StateFlow<UiState> = flow {
        val ro = runningOrderRepo.load()
        emitAll(
            combine(stateRepo.observeStates(), friendsByArtist) { stateMap, friendsMap ->
                UiState.Loaded(
                    days = ro.days.map { day ->
                        val cws = day.concerts.map { c ->
                            val s = stateMap[c.id]
                            ConcertWithState(
                                concert = c,
                                priority = Priority.of(s?.priority ?: 0),
                                rating = Rating.fromStorage(s?.rating),
                            )
                        }
                        DayUi(
                            id = day.id,
                            label = day.label,
                            bounds = computeBounds(day.concerts),
                            concerts = cws,
                        )
                    },
                    friendsByArtist = friendsMap,
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun setPriority(concert: Concert, priority: Priority) {
        viewModelScope.launch {
            stateRepo.setPriority(concert.id, priority)
            if (priority == Priority.P1) scheduler.schedule(concert) else scheduler.cancel(concert.id)
            schedulePush()
        }
    }

    fun setRating(concertId: String, rating: Rating?) {
        viewModelScope.launch { stateRepo.setRating(concertId, rating) }
    }

    fun exportPicks(uri: Uri) {
        viewModelScope.launch {
            val msg = runCatching { picksBackup.export(uri) }
                .fold(
                    onSuccess = { "${it.written} picks sauvegardés" },
                    onFailure = { "Erreur de sauvegarde : ${it.message}" },
                )
            _toasts.send(msg)
        }
    }

    fun importPicks(uri: Uri) {
        viewModelScope.launch {
            val msg = runCatching { picksBackup.import(uri) }
                .fold(
                    onSuccess = { r ->
                        if (r.unmatched == 0) "${r.matched} picks restaurés"
                        else "${r.matched} restaurés, ${r.unmatched} non retrouvés (ex. ${r.unmatchedExamples.joinToString()})"
                    },
                    onFailure = { "Erreur de restauration : ${it.message}" },
                )
            _toasts.send(msg)
            schedulePush(immediate = true)
        }
    }

    private var pushJob: Job? = null

    /** Compute the current P1 artist list and push to the current group's Firestore doc. */
    private fun schedulePush(immediate: Boolean = false) {
        pushJob?.cancel()
        pushJob = viewModelScope.launch {
            if (!immediate) delay(1500)
            val groupId = userPrefs.currentGroupId.first() ?: return@launch
            val user = authRepo.currentUserOrNull ?: return@launch
            val ro = runningOrderRepo.load()
            val states = stateRepo.observeStates().first()
            val concertsById = ro.days.flatMap { it.concerts }.associateBy { it.id }
            val p1Artists = states.values
                .asSequence()
                .filter { it.priority == Priority.P1.value }
                .mapNotNull { concertsById[it.concertId]?.artist }
                .distinct()
                .sorted()
                .toList()
            runCatching { groupPicksRepo.setMyP1Artists(groupId, user, p1Artists) }
                .onFailure { _toasts.send("Sync KO : ${it.message}") }
        }
    }

    private fun computeBounds(concerts: List<Concert>): Pair<Instant, Instant> {
        val earliest = concerts.minOf { it.start }
        val latest = concerts.maxOf { it.end }
        val startSec = earliest.epochSeconds - (earliest.epochSeconds.mod(1800L))
        val endRem = latest.epochSeconds.mod(1800L)
        val endSec = if (endRem == 0L) latest.epochSeconds else latest.epochSeconds + (1800L - endRem)
        return Instant.fromEpochSeconds(startSec) to Instant.fromEpochSeconds(endSec)
    }

    companion object {
        fun factory(app: MaTeamHFApp): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TimelineViewModel(
                    runningOrderRepo = app.runningOrderRepository,
                    stateRepo = app.concertStateRepository,
                    scheduler = app.reminderScheduler,
                    picksBackup = app.picksBackup,
                    userPrefs = app.userPrefs,
                    authRepo = app.authRepository,
                    groupPicksRepo = app.groupPicksRepository,
                )
            }
        }
    }
}

sealed interface UiState {
    data object Loading : UiState
    data class Loaded(
        val days: List<DayUi>,
        val friendsByArtist: Map<String, List<String>> = emptyMap(),
    ) : UiState
}

data class DayUi(
    val id: Int,
    val label: String,
    val bounds: Pair<Instant, Instant>,
    val concerts: List<ConcertWithState>,
)
