package com.mc.mateamhf

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mc.mateamhf.ui.auth.AuthViewModel
import com.mc.mateamhf.ui.auth.LoginScreen
import com.mc.mateamhf.ui.common.LoadingScreen
import com.mc.mateamhf.ui.groups.CreateGroupDialog
import com.mc.mateamhf.ui.groups.GroupSwitcherSheet
import com.mc.mateamhf.ui.groups.GroupsViewModel
import com.mc.mateamhf.ui.groups.JoinGroupDialog
import com.mc.mateamhf.ui.groups.NoGroupScreen
import com.mc.mateamhf.ui.splash.SplashScreen
import com.mc.mateamhf.ui.theme.MaTeamHFTheme
import com.mc.mateamhf.ui.timeline.TimelineScreen
import com.mc.mateamhf.ui.timeline.TimelineViewModel
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 1800L

class MainActivity : ComponentActivity() {

    private val timelineViewModel: TimelineViewModel by viewModels {
        TimelineViewModel.factory(application as MaTeamHFApp)
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.factory(application as MaTeamHFApp)
    }

    private val groupsViewModel: GroupsViewModel by viewModels {
        GroupsViewModel.factory(application as MaTeamHFApp)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
        setContent {
            MaTeamHFTheme {
                var splashDone by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(SPLASH_DURATION_MS)
                    splashDone = true
                }

                val ctx = LocalContext.current
                LaunchedEffect(Unit) {
                    groupsViewModel.toasts.collect { msg ->
                        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                val authState by authViewModel.state.collectAsStateWithLifecycle()
                val authBusy by authViewModel.busy.collectAsStateWithLifecycle()
                val authError by authViewModel.error.collectAsStateWithLifecycle()
                val groupsState by groupsViewModel.state.collectAsStateWithLifecycle()

                var showCreate by remember { mutableStateOf(false) }
                var showJoin by remember { mutableStateOf(false) }
                var showSwitcher by remember { mutableStateOf(false) }

                Box(Modifier.fillMaxSize()) {
                    when (authState) {
                        AuthViewModel.State.Loading -> LoadingScreen("Connexion…")
                        AuthViewModel.State.SignedOut -> LoginScreen(
                            busy = authBusy,
                            error = authError,
                            onSignIn = { authViewModel.signIn(ctx) },
                            onDismissError = { authViewModel.clearError() },
                        )
                        is AuthViewModel.State.SignedIn -> when (val g = groupsState) {
                            GroupsViewModel.State.Loading -> LoadingScreen("Chargement des teams…")
                            GroupsViewModel.State.NoGroups -> NoGroupScreen(
                                onCreateClick = { showCreate = true },
                                onJoinClick = { showJoin = true },
                            )
                            is GroupsViewModel.State.HasGroups -> TimelineScreen(
                                viewModel = timelineViewModel,
                                splashDone = splashDone,
                                currentGroupName = g.currentGroup?.name,
                                onTitleClick = { showSwitcher = true },
                                onSignOut = { authViewModel.signOut() },
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = !splashDone,
                        enter = fadeIn(tween(0)),
                        exit = fadeOut(tween(400)),
                    ) {
                        SplashScreen()
                    }
                }

                if (showCreate) CreateGroupDialog(
                    onDismiss = { showCreate = false },
                    onConfirm = { groupsViewModel.createGroup(it) },
                )
                if (showJoin) JoinGroupDialog(
                    onDismiss = { showJoin = false },
                    onConfirm = { groupsViewModel.joinGroup(it) },
                )
                if (showSwitcher) {
                    val g = groupsState as? GroupsViewModel.State.HasGroups
                    if (g != null) GroupSwitcherSheet(
                        groups = g.groups,
                        currentGroupId = g.currentGroupId,
                        onSelect = { groupsViewModel.selectGroup(it.id) },
                        onCreate = { showCreate = true },
                        onJoin = { showJoin = true },
                        onDismiss = { showSwitcher = false },
                    )
                }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
