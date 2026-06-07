package com.mc.mateamhf.ui.timeline

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mc.mateamhf.MaTeamHFApp
import com.mc.mateamhf.data.ConcertStateRepository
import com.mc.mateamhf.data.FestivalRepository
import com.mc.mateamhf.data.RunningOrderRepository
import com.mc.mateamhf.data.auth.AuthRepository
import com.mc.mateamhf.data.events.TeamEventRepository
import com.mc.mateamhf.data.groups.Group
import com.mc.mateamhf.data.groups.GroupPick
import com.mc.mateamhf.data.groups.GroupPicksRepository
import com.mc.mateamhf.data.groups.GroupRepository
import com.mc.mateamhf.data.picks.PicksBackup
import com.mc.mateamhf.data.prefs.UserPrefs
import com.mc.mateamhf.data.providers.ProviderId
import com.mc.mateamhf.data.prefs.UserPrefs as UserPrefsType
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.domain.ConcertWithState
import com.mc.mateamhf.domain.DEFAULT_FESTIVAL_ID
import com.mc.mateamhf.domain.FestivalMeta
import com.mc.mateamhf.domain.Priority
import com.mc.mateamhf.domain.Rating
import com.mc.mateamhf.domain.Stage
import com.mc.mateamhf.domain.TeamEvent
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val runningOrderRepo: RunningOrderRepository,
    private val stateRepo: ConcertStateRepository,
    private val scheduler: ReminderScheduler,
    private val picksBackup: PicksBackup,
    private val userPrefs: UserPrefs,
    private val authRepo: AuthRepository,
    private val groupPicksRepo: GroupPicksRepository,
    private val groupRepo: GroupRepository,
    private val festivalRepo: FestivalRepository,
    private val teamEventRepo: TeamEventRepository,
) : ViewModel() {

    private val _toasts = Channel<String>(Channel.BUFFERED)
    val toasts: Flow<String> = _toasts.receiveAsFlow()

    /** All festivals known to the app (from assets/festivals/index.json). */
    val allFestivals: StateFlow<List<FestivalMeta>> = kotlinx.coroutines.flow.flow {
        emit(runCatching { festivalRepo.list() }.getOrDefault(emptyList()))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Live observation of the currently selected group (full doc, including festivalIds). Public — Options uses it. */
    val currentGroup: StateFlow<Group?> = userPrefs.currentGroupId
        .flatMapLatest { id -> if (id == null) flowOf(null) else groupRepo.observeGroup(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The festival to show. Picks the user's explicit choice if it's still in the group,
     * otherwise the first festival of the group, otherwise the default.
     */
    val activeFestival: StateFlow<String> = combine(
        currentGroup,
        userPrefs.activeFestivalId,
    ) { group, explicit ->
        val allowed = group?.festivalIds.orEmpty().ifEmpty { listOf(DEFAULT_FESTIVAL_ID) }
        when {
            explicit != null && allowed.contains(explicit) -> explicit
            else -> allowed.first()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_FESTIVAL_ID)

    /** Festivals available for the current group (resolved meta). */
    val groupFestivals: StateFlow<List<FestivalMeta>> = combine(
        currentGroup,
        allFestivals,
    ) { group, all ->
        val ids = group?.festivalIds.orEmpty()
        if (ids.isEmpty()) all.filter { it.id == DEFAULT_FESTIVAL_ID }
        else all.filter { ids.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val enabledProviders: StateFlow<Set<ProviderId>> = userPrefs.enabledProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Direct accessor used by the Options tab — keeps it simple, no separate VM. */
    val userPrefsForOptions: UserPrefsType get() = userPrefs

    val myUid: StateFlow<String?> = authRepo.currentUser
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Live picks of every member of the current group (including self). */
    private val currentGroupPicks: Flow<List<GroupPick>> = userPrefs.currentGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList()) else groupPicksRepo.observe(groupId)
        }

    /** Live team events for the current group + active festival. */
    private val teamEventsFlow: Flow<List<TeamEvent>> = combine(
        userPrefs.currentGroupId,
        activeFestival,
    ) { gid, fid -> gid to fid }
        .distinctUntilChanged()
        .flatMapLatest { (gid, fid) -> teamEventRepo.observe(gid, fid) }

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

    /** Running order loaded reactively for the active festival. */
    private val runningOrder = activeFestival
        .map { fid -> runCatching { runningOrderRepo.load(fid) }.getOrNull() }
        .distinctUntilChanged()

    val state: StateFlow<UiState> = combine(
        runningOrder,
        stateRepo.observeStates(),
        friendsByArtist,
        teamEventsFlow,
    ) { ro, stateMap, friendsMap, events ->
        if (ro == null) return@combine UiState.Loading
        // Group events by the Paris-local calendar date so a 13h00 lunch lands on the right day
        // (the old "in concert bounds" check kicked early-day events out as orphans).
        val eventsByDayDate: Map<String, List<TeamEvent>> = events.groupBy { ev ->
            val ldt = ev.start.toLocalDateTime(com.mc.mateamhf.ui.timeline.ParisTz)
            "${ldt.year}-${"%02d".format(ldt.monthNumber)}-${"%02d".format(ldt.dayOfMonth)}"
        }
        UiState.Loaded(
            stages = ro.stages,
            days = ro.days.map { day ->
                val cws = day.concerts.map { c ->
                    val s = stateMap[c.id]
                    ConcertWithState(
                        concert = c,
                        priority = Priority.of(s?.priority ?: 0),
                        rating = Rating.fromStorage(s?.rating),
                    )
                }
                val dayEvents = eventsByDayDate[day.date].orEmpty()
                DayUi(
                    id = day.id,
                    label = day.label,
                    date = day.date,
                    bounds = computeBoundsCovering(day.concerts, dayEvents),
                    concerts = cws,
                    teamEvents = dayEvents,
                )
            },
            friendsByArtist = friendsMap,
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

    fun selectFestival(festivalId: String) {
        viewModelScope.launch { userPrefs.setActiveFestivalId(festivalId) }
    }

    fun toggleFestivalInGroup(festivalId: String, enabled: Boolean) {
        viewModelScope.launch {
            val groupId = userPrefs.currentGroupId.first() ?: return@launch
            runCatching {
                if (enabled) groupRepo.addFestivalToGroup(groupId, festivalId)
                else groupRepo.removeFestivalFromGroup(groupId, festivalId)
            }.onFailure { _toasts.send("Modif KO : ${it.message}") }
        }
    }

    fun createTeamEvent(
        title: String,
        location: String?,
        dayDate: String,
        startHHMM: String,
        endHHMM: String,
    ) {
        viewModelScope.launch {
            val groupId = userPrefs.currentGroupId.first() ?: return@launch
            val user = authRepo.currentUserOrNull ?: return@launch
            val festivalId = activeFestival.first()
            val startIso = "${dayDate}T${startHHMM}:00+02:00"
            val endIso = "${dayDate}T${endHHMM}:00+02:00"
            runCatching {
                teamEventRepo.create(
                    groupId = groupId,
                    user = user,
                    title = title,
                    location = location,
                    startIso = startIso,
                    endIso = endIso,
                    festivalId = festivalId,
                )
            }.onFailure { _toasts.send("Création KO : ${it.message}") }
        }
    }

    fun deleteTeamEvent(eventId: String) {
        viewModelScope.launch {
            val groupId = userPrefs.currentGroupId.first() ?: return@launch
            runCatching { teamEventRepo.delete(groupId, eventId) }
                .onFailure { _toasts.send("Suppression KO : ${it.message}") }
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
            val ro = runningOrderRepo.load(activeFestival.first())
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

    /**
     * Like [computeBounds] but also pulls in [events] so the visual scroll-range covers them too.
     * Concerts alone determine bounds when there are no events; events can stretch the day earlier
     * or later than the concert schedule.
     */
    private fun computeBoundsCovering(concerts: List<Concert>, events: List<TeamEvent>): Pair<Instant, Instant> {
        val concertStarts = concerts.map { it.start }
        val concertEnds = concerts.map { it.end }
        val eventStarts = events.map { it.start }
        val eventEnds = events.map { it.end }
        val starts = concertStarts + eventStarts
        val ends = concertEnds + eventEnds
        if (starts.isEmpty() || ends.isEmpty()) return computeBounds(concerts)
        val earliest = starts.min()
        val latest = ends.max()
        val startSec = earliest.epochSeconds - (earliest.epochSeconds.mod(1800L))
        val endRem = latest.epochSeconds.mod(1800L)
        val endSec = if (endRem == 0L) latest.epochSeconds else latest.epochSeconds + (1800L - endRem)
        return Instant.fromEpochSeconds(startSec) to Instant.fromEpochSeconds(endSec)
    }

    private fun computeBounds(concerts: List<Concert>): Pair<Instant, Instant> {
        if (concerts.isEmpty()) {
            val now = Instant.fromEpochSeconds(0)
            return now to now
        }
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
                    groupRepo = app.groupRepository,
                    festivalRepo = app.festivalRepository,
                    teamEventRepo = app.teamEventRepository,
                )
            }
        }
    }
}

sealed interface UiState {
    data object Loading : UiState
    data class Loaded(
        val days: List<DayUi>,
        val stages: List<Stage> = emptyList(),
        val friendsByArtist: Map<String, List<String>> = emptyMap(),
    ) : UiState
}

data class DayUi(
    val id: Int,
    val label: String,
    val date: String,
    val bounds: Pair<Instant, Instant>,
    val concerts: List<ConcertWithState>,
    val teamEvents: List<TeamEvent> = emptyList(),
)
