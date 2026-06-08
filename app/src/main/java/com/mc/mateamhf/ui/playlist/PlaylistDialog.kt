package com.mc.mateamhf.ui.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.mc.mateamhf.data.playlist.PlaylistGenStatus
import com.mc.mateamhf.data.playlist.PlaylistService

/**
 * Dialog driven by [status]. Shows:
 *  - Idle: provider chooser (Spotify only for now, Deezer disabled with note)
 *  - AwaitingAuth: hint about the browser tab
 *  - Fetching/Creating: progress bar
 *  - Success: list of created playlists with "Open in Spotify" + "Open web" actions
 *  - Error: message + Reset button
 */
@Composable
fun PlaylistDialog(
    status: PlaylistGenStatus,
    onGenerate: (PlaylistService) -> Unit,
    onSignOut: (PlaylistService) -> Unit,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Générer mes playlists") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                when (status) {
                    PlaylistGenStatus.Idle -> {
                        Text(
                            "On t'envoie 2 playlists — une « Mes priorités 1 », une « Mes priorités 1 & 2 » — avec les 3 morceaux les plus écoutés de chaque artiste.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onGenerate(PlaylistService.SPOTIFY) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Spotify") }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Deezer arrive bientôt (nécessite un petit relais serveur que MC ajoute après les tests internes).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is PlaylistGenStatus.AwaitingAuth -> {
                        Text(
                            "Ton navigateur s'est ouvert pour te connecter à ${status.service.displayName}. Reviens dans l'app après avoir validé.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is PlaylistGenStatus.Fetching -> {
                        Text(
                            "Top tracks ${status.artistsDone} / ${status.artistsTotal}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { if (status.artistsTotal > 0) status.artistsDone.toFloat() / status.artistsTotal else 0f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is PlaylistGenStatus.Creating -> {
                        Text("Création des playlists sur ${status.service.displayName}…")
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is PlaylistGenStatus.Success -> {
                        Text("${status.playlists.size} playlist(s) créée(s) sur ${status.service.displayName} 🎉")
                        Spacer(Modifier.height(8.dp))
                        status.playlists.forEach { p ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Text(p.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${p.trackCount} morceaux",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (p.deepLink != null) {
                                        TextButton(onClick = { uriHandler.openUri(p.deepLink) }) {
                                            Text("Ouvrir dans l'app")
                                        }
                                    }
                                    TextButton(onClick = { uriHandler.openUri(p.webUrl) }) {
                                        Text("Voir sur le web")
                                    }
                                }
                            }
                        }
                    }
                    is PlaylistGenStatus.Error -> {
                        Text(
                            status.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (status is PlaylistGenStatus.Success) {
                TextButton(onClick = onDismiss) { Text("Terminé") }
            } else {
                TextButton(onClick = onDismiss) { Text("Fermer") }
            }
        },
        dismissButton = {
            // Subtle "sign out" action so users can revoke + re-auth on next attempt
            if (status is PlaylistGenStatus.Success || status is PlaylistGenStatus.Error) {
                TextButton(onClick = { onSignOut(PlaylistService.SPOTIFY) }) {
                    Text("Déconnecter Spotify")
                }
            }
        },
    )
}
