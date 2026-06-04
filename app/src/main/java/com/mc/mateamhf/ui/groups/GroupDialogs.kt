package com.mc.mateamhf.ui.groups

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.mc.mateamhf.data.groups.JoinCode

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une team") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                label = { Text("Nom de la team") },
                placeholder = { Text("Ex. Les Métalleux du 75") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name); onDismiss() }, enabled = name.isNotBlank()) {
                Text("Créer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rejoindre une team") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { input -> code = JoinCode.normalize(input) },
                singleLine = true,
                label = { Text("Code d'invitation") },
                placeholder = { Text("XXXX-XXXX") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(code); onDismiss() },
                enabled = code.length == 9, // "XXXX-XXXX"
            ) {
                Text("Rejoindre")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
