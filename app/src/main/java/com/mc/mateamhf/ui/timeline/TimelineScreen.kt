package com.mc.mateamhf.ui.timeline

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.domain.artistKey
import com.mc.mateamhf.ui.detail.ConcertSheet
import com.mc.mateamhf.ui.friends.FriendsScreen
import com.mc.mateamhf.ui.myro.MyRunningOrderScreen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    splashDone: Boolean = true,
    currentGroupName: String? = null,
    onTitleClick: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportPicks) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importPicks) }

    LaunchedEffect(viewModel) {
        viewModel.toasts.collect { snackbarHostState.showSnackbar(it) }
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var currentTab by remember { mutableIntStateOf(0) }
    var selectedConcertId by remember { mutableStateOf<String?>(null) }

    val loaded = state as? UiState.Loaded
    val currentSelected = selectedConcertId?.let { id ->
        loaded?.days?.firstNotNullOfOrNull { day -> day.concerts.firstOrNull { it.concert.id == id } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentGroupName ?: "Ma Team HF",
                        modifier = Modifier.clickable { onTitleClick() },
                    )
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Changer de team") },
                            onClick = {
                                menuExpanded = false
                                onTitleClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Sauvegarder mes picks") },
                            onClick = {
                                menuExpanded = false
                                exportLauncher.launch("mateamhf_picks_${LocalDate.now()}.json")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Restaurer depuis un fichier") },
                            onClick = {
                                menuExpanded = false
                                importLauncher.launch(arrayOf("application/json"))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Se déconnecter") },
                            onClick = {
                                menuExpanded = false
                                onSignOut()
                            },
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text("Timeline") },
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("Amis") },
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.PlaylistAddCheck, contentDescription = null) },
                    label = { Text("Mon RO") },
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (val ui = state) {
                UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Loaded -> when (currentTab) {
                    0 -> LoadedTimeline(
                        ui = ui,
                        onConcertClick = { selectedConcertId = it.id },
                    )
                    1 -> FriendsScreen(
                        state = ui,
                        onConcertClick = { selectedConcertId = it.id },
                    )
                    else -> MyRunningOrderScreen(
                        state = ui,
                        onConcertClick = { selectedConcertId = it.id },
                    )
                }
            }
        }
    }

    currentSelected?.let { cws ->
        val friends = loaded?.friendsByArtist?.get(artistKey(cws.concert.artist)).orEmpty()
        ConcertSheet(
            cws = cws,
            friends = friends,
            onDismiss = { selectedConcertId = null },
            onPriorityChange = { viewModel.setPriority(cws.concert, it) },
            onRatingChange = { viewModel.setRating(cws.concert.id, it) },
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedTimeline(
    ui: UiState.Loaded,
    onConcertClick: (Concert) -> Unit,
) {
    var selectedDayIndex by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedDayIndex,
            edgePadding = 0.dp,
        ) {
            ui.days.forEachIndexed { i, d ->
                Tab(
                    selected = selectedDayIndex == i,
                    onClick = { selectedDayIndex = i },
                    text = { Text(d.label) },
                )
            }
        }
        DayTimeline(
            day = ui.days[selectedDayIndex],
            friendsByArtist = ui.friendsByArtist,
            onConcertClick = onConcertClick,
        )
    }
}
