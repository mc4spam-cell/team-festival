package com.mc.mateamhf.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mc.mateamhf.domain.TeamEvent

/**
 * Bottom sheet showing the full info of a team event. Read-only by design — only the
 * creator gets a "Cancel event" action button (which prompts a confirmation dialog
 * before actually deleting from Firestore).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    event: TeamEvent,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onCancelEvent: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmingCancel by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
            Text(
                text = event.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatHm(event.start)} → ${formatHm(event.end)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!event.location.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("Lieu", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text("📍 ${event.location}", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.height(16.dp))
            Text("Créé par", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(event.creatorName, style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(24.dp))

            if (isMine) {
                OutlinedButton(
                    onClick = { confirmingCancel = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Annuler cet événement", color = Color(0xFFE53935))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "L'événement disparaîtra immédiatement pour tous les membres de la team.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Seul le créateur peut annuler son événement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmingCancel) {
        AlertDialog(
            onDismissRequest = { confirmingCancel = false },
            title = { Text("Annuler l'événement ?") },
            text = { Text("« ${event.title} » sera supprimé pour tous les membres. Action irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingCancel = false
                    onCancelEvent()
                }) { Text("Annuler l'événement", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingCancel = false }) { Text("Garder") }
            },
        )
    }
}
