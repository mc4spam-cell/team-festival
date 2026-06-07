package com.mc.mateamhf.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.mc.mateamhf.data.providers.ProviderId
import com.mc.mateamhf.domain.ConcertWithState
import com.mc.mateamhf.domain.Priority
import com.mc.mateamhf.domain.Rating
import com.mc.mateamhf.ui.timeline.formatRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcertSheet(
    cws: ConcertWithState,
    friends: List<String>,
    enabledProviders: Set<ProviderId>,
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

            val music = enabledProviders.filter { it.category == ProviderId.Category.MUSIC }
            val social = enabledProviders.filter { it.category == ProviderId.Category.SOCIAL }

            if (music.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Écouter", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                ProviderRow(music) { openForProvider(ctx, it, concert) }
            }
            if (social.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Réseaux", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                ProviderRow(social) { openForProvider(ctx, it, concert) }
            }

            if (music.isEmpty() && social.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Aucun fournisseur activé. Va dans l'onglet Options pour en cocher.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProviderRow(providers: List<ProviderId>, onClick: (ProviderId) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        providers.forEach { p ->
            FilledTonalButton(onClick = { onClick(p) }) {
                Text(p.displayName, maxLines = 1)
            }
        }
    }
}
