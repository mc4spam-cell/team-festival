package com.mc.mateamhf.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mc.mateamhf.data.providers.ProviderId
import com.mc.mateamhf.data.providers.DEFAULT_ENABLED_PROVIDERS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPrefs(private val appContext: Context) {

    private val pseudoKey = stringPreferencesKey("pseudo")
    private val currentGroupIdKey = stringPreferencesKey("current_group_id")
    private val activeFestivalIdKey = stringPreferencesKey("active_festival_id")
    private val enabledProvidersKey = stringSetPreferencesKey("enabled_providers")

    val pseudo: Flow<String?> = appContext.dataStore.data.map { it[pseudoKey]?.takeIf { v -> v.isNotBlank() } }

    suspend fun setPseudo(value: String) {
        appContext.dataStore.edit { it[pseudoKey] = value.trim() }
    }

    val currentGroupId: Flow<String?> = appContext.dataStore.data
        .map { it[currentGroupIdKey]?.takeIf { v -> v.isNotBlank() } }

    suspend fun setCurrentGroupId(value: String?) {
        appContext.dataStore.edit { prefs ->
            if (value.isNullOrBlank()) prefs.remove(currentGroupIdKey)
            else prefs[currentGroupIdKey] = value
        }
    }

    /** Active festival to show in the timeline. Falls back to null = "first of group's list". */
    val activeFestivalId: Flow<String?> = appContext.dataStore.data
        .map { it[activeFestivalIdKey]?.takeIf { v -> v.isNotBlank() } }

    suspend fun setActiveFestivalId(value: String?) {
        appContext.dataStore.edit { prefs ->
            if (value.isNullOrBlank()) prefs.remove(activeFestivalIdKey)
            else prefs[activeFestivalIdKey] = value
        }
    }

    /** Set of provider ids the user wants to see in the Detail sheet. Defaults to Spotify + Instagram. */
    val enabledProviders: Flow<Set<ProviderId>> = appContext.dataStore.data
        .map { prefs ->
            val raw = prefs[enabledProvidersKey]
            if (raw == null) DEFAULT_ENABLED_PROVIDERS
            else raw.mapNotNull { id -> runCatching { ProviderId.valueOf(id) }.getOrNull() }.toSet()
                .ifEmpty { DEFAULT_ENABLED_PROVIDERS }
        }

    suspend fun setEnabledProviders(value: Set<ProviderId>) {
        appContext.dataStore.edit { it[enabledProvidersKey] = value.map { p -> p.name }.toSet() }
    }

    suspend fun toggleProvider(id: ProviderId) {
        appContext.dataStore.edit { prefs ->
            val raw = prefs[enabledProvidersKey] ?: DEFAULT_ENABLED_PROVIDERS.map { it.name }.toSet()
            val next = raw.toMutableSet()
            val key = id.name
            if (next.contains(key)) next.remove(key) else next.add(key)
            prefs[enabledProvidersKey] = next
        }
    }
}
