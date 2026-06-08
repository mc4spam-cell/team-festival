package com.mc.mateamhf

import android.app.Application
import com.mc.mateamhf.data.ConcertStateRepository
import com.mc.mateamhf.data.FestivalRepository
import com.mc.mateamhf.data.RunningOrderRepository
import com.mc.mateamhf.data.auth.AuthRepository
import com.mc.mateamhf.data.db.AppDatabase
import com.mc.mateamhf.data.events.TeamEventRepository
import com.mc.mateamhf.data.groups.GroupPicksRepository
import com.mc.mateamhf.data.groups.GroupRepository
import com.mc.mateamhf.data.groups.UserProfileRepository
import com.mc.mateamhf.data.picks.PicksBackup
import com.mc.mateamhf.data.playlist.PlaylistGenerator
import com.mc.mateamhf.data.playlist.TokenStore
import com.mc.mateamhf.data.prefs.UserPrefs
import com.mc.mateamhf.data.sync.FriendsSyncRepository
import com.mc.mateamhf.notification.NotificationChannels
import com.mc.mateamhf.notification.ReminderScheduler

class MaTeamHFApp : Application() {

    val runningOrderRepository: RunningOrderRepository by lazy {
        RunningOrderRepository(applicationContext)
    }

    val concertStateRepository: ConcertStateRepository by lazy {
        ConcertStateRepository(AppDatabase.get(applicationContext).concertStateDao())
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(applicationContext)
    }

    val picksBackup: PicksBackup by lazy {
        PicksBackup(applicationContext, runningOrderRepository, concertStateRepository)
    }

    val userPrefs: UserPrefs by lazy { UserPrefs(applicationContext) }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            appContext = applicationContext,
            webClientId = getString(R.string.default_web_client_id),
        )
    }

    val userProfileRepository: UserProfileRepository by lazy { UserProfileRepository() }
    val groupRepository: GroupRepository by lazy { GroupRepository() }
    val groupPicksRepository: GroupPicksRepository by lazy { GroupPicksRepository() }
    val festivalRepository: FestivalRepository by lazy { FestivalRepository(applicationContext) }
    val teamEventRepository: TeamEventRepository by lazy { TeamEventRepository() }

    /** Shared OkHttp client for outgoing HTTP (playlist providers). Connection pool reused. */
    val httpClient: okhttp3.OkHttpClient by lazy { okhttp3.OkHttpClient() }
    val playlistTokenStore: TokenStore by lazy { TokenStore(applicationContext) }
    val playlistGenerator: PlaylistGenerator by lazy {
        PlaylistGenerator(applicationContext, httpClient, playlistTokenStore)
    }

    val friendsSyncRepository: FriendsSyncRepository by lazy {
        FriendsSyncRepository(
            runningOrderRepo = runningOrderRepository,
            stateRepo = concertStateRepository,
            friendDao = AppDatabase.get(applicationContext).friendPickDao(),
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
    }
}
