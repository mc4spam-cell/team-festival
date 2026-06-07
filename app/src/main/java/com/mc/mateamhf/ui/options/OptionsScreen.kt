package com.mc.mateamhf.ui.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mc.mateamhf.data.groups.Group
import com.mc.mateamhf.data.prefs.UserPrefs
import com.mc.mateamhf.data.providers.ProviderId
import com.mc.mateamhf.domain.FestivalMeta
import kotlinx.coroutines.launch

@Composable
fun OptionsScreen(
    userPrefs: UserPrefs,
    allFestivals: List<FestivalMeta>,
    currentGroup: Group?,
    myUid: String?,
    onToggleFestival: (festivalId: String, enabled: Boolean) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val enabled by userPrefs.enabledProviders.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val music = ProviderId.entries.filter { it.category == ProviderId.Category.MUSIC }
    val social = ProviderId.entries.filter { it.category == ProviderId.Category.SOCIAL }

    val groupFestivalIds = currentGroup?.festivalIds.orEmpty().toSet()
    val isOwner = currentGroup != null && myUid != null && currentGroup.ownerUid == myUid

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Options", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            SectionTitle("Festivals de cette team")
            if (currentGroup != null) {
                Text(
                    text = if (isOwner)
                        "Coche les festivals que ta team suit. Le sélecteur sur la timeline n'apparaît qu'à partir de 2 festivals."
                    else
                        "Seul ${currentGroup.ownerUid.take(6)}… (le créateur) peut modifier ces choix.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        items(allFestivals, key = { "fest:${it.id}" }) { f ->
            FestivalToggle(
                festival = f,
                checked = groupFestivalIds.contains(f.id),
                enabled = isOwner,
                onToggle = { newValue -> onToggleFestival(f.id, newValue) },
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Liens proposés sur la fiche concert")
            Text(
                "Choisis les services à afficher. Si on n'a pas l'identifiant de l'artiste, on retombe sur une recherche.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            SectionTitle("Musique")
        }
        items(music, key = { "p:${it.name}" }) { p ->
            ProviderToggle(
                provider = p,
                checked = enabled.contains(p),
                onToggle = { scope.launch { userPrefs.toggleProvider(p) } },
            )
        }
        item {
            Spacer(Modifier.height(16.dp))
            SectionTitle("Réseaux sociaux")
        }
        items(social, key = { "p:${it.name}" }) { p ->
            ProviderToggle(
                provider = p,
                checked = enabled.contains(p),
                onToggle = { scope.launch { userPrefs.toggleProvider(p) } },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun FestivalToggle(
    festival: FestivalMeta,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = festival.shortName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${festival.dates} · ${festival.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, enabled = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ProviderToggle(provider: ProviderId, checked: Boolean, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = provider.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}
