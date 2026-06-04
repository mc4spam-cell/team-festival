package com.mc.mateamhf.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.domain.Stage
import com.mc.mateamhf.domain.artistKey
import com.mc.mateamhf.ui.timeline.UiState
import com.mc.mateamhf.ui.timeline.formatHm
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

@Composable
fun FriendsScreen(
    state: UiState.Loaded,
    onConcertClick: (Concert) -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Clock.System.now()
        }
    }

    val concertsByStage: Map<Stage, List<Concert>> = state.days
        .flatMap { it.concerts.map { c -> c.concert } }
        .groupBy { it.stage }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Section(
            title = "Maintenant",
            stages = Stage.entries,
            concertFor = { stage ->
                concertsByStage[stage].orEmpty().sortedBy { it.start }
                    .firstOrNull { now in it.start..it.end }
            },
            friendsByArtist = state.friendsByArtist,
            onConcertClick = onConcertClick,
        )

        Spacer(Modifier.height(24.dp))

        Section(
            title = "Prochain",
            stages = Stage.entries,
            concertFor = { stage ->
                concertsByStage[stage].orEmpty().sortedBy { it.start }
                    .firstOrNull { it.start > now }
            },
            friendsByArtist = state.friendsByArtist,
            onConcertClick = onConcertClick,
        )

        Spacer(Modifier.height(24.dp))
        if (state.friendsByArtist.isEmpty()) {
            Text(
                text = "Aucun ami n'a encore partagé ses P1. Le menu ⋮ ▸ Rafraîchir va chercher la dernière version.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(
    title: String,
    stages: List<Stage>,
    concertFor: (Stage) -> Concert?,
    friendsByArtist: Map<String, List<String>>,
    onConcertClick: (Concert) -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    stages.forEach { stage ->
        StageRow(
            stage = stage,
            concert = concertFor(stage),
            friendsByArtist = friendsByArtist,
            onConcertClick = onConcertClick,
        )
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
    }
}

@Composable
private fun StageRow(
    stage: Stage,
    concert: Concert?,
    friendsByArtist: Map<String, List<String>>,
    onConcertClick: (Concert) -> Unit,
) {
    val friends = concert?.let { friendsByArtist[artistKey(it.artist)].orEmpty() }.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (concert != null) Modifier.clickable { onConcertClick(concert) } else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(stage.color),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = stage.displayName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
            if (concert == null) {
                Text(
                    text = "—",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "${concert.artist}  ·  ${formatHm(concert.start)}–${formatHm(concert.end)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (friends.isEmpty()) "(personne en P1)" else "👥 ${friends.joinToString(", ")}",
                    fontSize = 13.sp,
                    color = if (friends.isEmpty())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

