package com.mc.mateamhf.ui.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
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

@Composable
fun PseudoDialog(
    initialValue: String? = null,
    onDismiss: (() -> Unit)? = null,
    onSubmit: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initialValue.orEmpty()) }
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = { Text("Ton pseudo") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text("Visible par tes amis quand tu partages tes picks.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.trimStart() },
                    singleLine = true,
                    label = { Text("Pseudo") },
                    placeholder = { Text("ex. Marc") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.trim().isNotEmpty(),
                onClick = { onSubmit(value.trim()) },
            ) { Text("Valider") }
        },
        dismissButton = onDismiss?.let { dismiss ->
            { TextButton(onClick = dismiss) { Text("Annuler") } }
        },
    )
}
