package com.mc.mateamhf.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.domain.ConcertWithState
import com.mc.mateamhf.domain.Priority
import com.mc.mateamhf.domain.Rating
import com.mc.mateamhf.ui.timeline.formatRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcertSheet(
    cws: ConcertWithState,
    friends: List<String>,
    onDismiss: () -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onRatingChange: (Rating?) -> Unit,
) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val concert = cws.concert

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {

            Text(
                text = concert.artist,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${concert.stage.displayName} · ${formatRange(concert.start, concert.end)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            Text("Priorité", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = p == cws.priority,
                        onClick = { onPriorityChange(p) },
                        label = { Text(p.label) },
                    )
                }
            }

            if (friends.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "👥 ${friends.joinToString(", ")} — P1",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("Note", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Rating.entries.forEach { r ->
                    FilterChip(
                        selected = r == cws.rating,
                        onClick = { onRatingChange(if (r == cws.rating) null else r) },
                        label = { Text(r.label) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Écouter / Découvrir", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            ServiceButtonRow(concert, ctx = ctx)

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ServiceButtonRow(concert: Concert, ctx: android.content.Context) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ServiceButton(
                label = "Apple Music",
                modifier = Modifier.weight(1f),
                onClick = { openInAppleMusic(ctx, concert) },
            )
            ServiceButton(
                label = "Spotify",
                modifier = Modifier.weight(1f),
                onClick = { openInSpotify(ctx, concert) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ServiceButton(
                label = "Deezer",
                modifier = Modifier.weight(1f),
                onClick = { openInDeezer(ctx, concert) },
            )
            if (concert.instagramHandle != null) {
                ServiceButton(
                    label = "Instagram",
                    modifier = Modifier.weight(1f),
                    onClick = { openInInstagram(ctx, concert) },
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ServiceButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Text(label, maxLines = 1)
    }
}
