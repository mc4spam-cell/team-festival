package com.mc.mateamhf.ui.myro

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.domain.ConcertWithState
import com.mc.mateamhf.domain.Priority
import com.mc.mateamhf.ui.timeline.UiState
import com.mc.mateamhf.ui.timeline.formatHm

@Composable
fun MyRunningOrderScreen(
    state: UiState.Loaded,
    onConcertClick: (Concert) -> Unit,
    modifier: Modifier = Modifier,
) {
    val daysWithPicks = state.days.map { day ->
        day to day.concerts
            .filter { it.priority != Priority.NONE || it.rating != null }
            .sortedBy { it.concert.start }
    }

    val hasAny = daysWithPicks.any { (_, picks) -> picks.isNotEmpty() }

    if (!hasAny) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Aucun concert marqué pour l'instant.\nDans la timeline, tape un concert et choisis une priorité (P1/P2/P3) ou une note.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        daysWithPicks.forEach { (day, picks) ->
            if (picks.isEmpty()) return@forEach
            item(key = "day-${day.id}") {
                DayHeader(label = day.label, count = picks.size)
            }

            // Detect time overlaps within this day's picks
            val overlapping: Set<String> = run {
                val ids = mutableSetOf<String>()
                for (i in picks.indices) {
                    for (j in i + 1 until picks.size) {
                        val a = picks[i].concert
                        val b = picks[j].concert
                        if (a.start < b.end && b.start < a.end) {
                            ids += a.id
                            ids += b.id
                        }
                    }
                }
                ids
            }

            items(picks, key = { it.concert.id }) { cws ->
                PickRow(
                    cws = cws,
                    isOverlapping = cws.concert.id in overlapping,
                    onClick = { onConcertClick(cws.concert) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
            }
            item(key = "spacer-${day.id}") { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun DayHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "· $count",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PickRow(cws: ConcertWithState, isOverlapping: Boolean, onClick: () -> Unit) {
    val concert = cws.concert
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PriorityBadge(cws.priority)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatHm(concert.start)} → ${formatHm(concert.end)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isOverlapping) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "⚠ conflit",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                cws.rating?.let { r ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = r.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = concert.artist,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(concert.stage.color),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = concert.stage.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: Priority) {
    val (label, bg) = when (priority) {
        Priority.P1 -> "P1" to Color(0xFFC62828)
        Priority.P2 -> "P2" to Color(0xFFEF6C00)
        Priority.P3 -> "P3" to Color(0xFF6D4C41)
        Priority.NONE -> "—" to Color(0xFF455A64)
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}
