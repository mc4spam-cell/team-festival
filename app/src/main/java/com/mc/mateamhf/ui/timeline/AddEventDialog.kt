package com.mc.mateamhf.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mc.mateamhf.domain.FestivalDay

private val EVENT_SUBJECTS = listOf("Repas", "Café", "Bière", "RDV")

/** Half-hour slots from 08:00 to 03:30 (next morning) — covers a long festival day. */
private val START_SLOTS: List<String> = buildList {
    // 08:00 → 23:30
    for (h in 8..23) {
        add("%02d:00".format(h))
        add("%02d:30".format(h))
    }
    // 00:00 → 03:30 (next-morning end-of-festival)
    for (h in 0..3) {
        add("%02d:00".format(h))
        add("%02d:30".format(h))
    }
}

/** end = start + 30min, with wrap-around past midnight. */
private fun endAfterHalfHour(start: String): String {
    val (hh, mm) = start.split(":").map { it.toInt() }
    val total = (hh * 60 + mm + 30) % (24 * 60)
    return "%02d:%02d".format(total / 60, total % 60)
}

/**
 * Compact form: pick a subject from a dropdown and a 30-min start slot.
 * End time is implicit (start + 30min), location is optional free-text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    day: FestivalDay,
    onDismiss: () -> Unit,
    onConfirm: (title: String, location: String?, dayDate: String, startHHMM: String, endHHMM: String) -> Unit,
) {
    var subject by remember { mutableStateOf(EVENT_SUBJECTS.first()) }
    var subjectOpen by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("13:00") }
    var startOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel événement team") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Pour ${day.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                // Subject dropdown
                ExposedDropdownMenuBox(
                    expanded = subjectOpen,
                    onExpandedChange = { subjectOpen = it },
                ) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sujet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = subjectOpen,
                        onDismissRequest = { subjectOpen = false },
                    ) {
                        EVENT_SUBJECTS.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    subject = s
                                    subjectOpen = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { if (it.length <= 80) location = it },
                    label = { Text("Lieu (optionnel)") },
                    placeholder = { Text("Bar du camping") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                // Start time dropdown (30-min slots)
                ExposedDropdownMenuBox(
                    expanded = startOpen,
                    onExpandedChange = { startOpen = it },
                ) {
                    OutlinedTextField(
                        value = "$start → ${endAfterHalfHour(start)}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Créneau (30 min)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = startOpen,
                        onDismissRequest = { startOpen = false },
                    ) {
                        START_SLOTS.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("$s → ${endAfterHalfHour(s)}") },
                                onClick = {
                                    start = s
                                    startOpen = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        subject,
                        location.takeIf { it.isNotBlank() },
                        day.date,
                        start,
                        endAfterHalfHour(start),
                    )
                },
            ) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
