package com.mc.mateamhf.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPrefs(private val appContext: Context) {

    private val pseudoKey = stringPreferencesKey("pseudo")
    private val currentGroupIdKey = stringPreferencesKey("current_group_id")

    val pseudo: Flow<String?> = appContext.dataStore.data.map { it[pseudoKey]?.takeIf { v -> v.isNotBlank() } }

    suspend fun setPseudo(value: String) {
        appContext.dataStore.edit { it[pseudoKey] = value.trim() }
    }

    /** Persisted choice of which group is currently selected in the UI. */
    val currentGroupId: Flow<String?> = appContext.dataStore.data
        .map { it[currentGroupIdKey]?.takeIf { v -> v.isNotBlank() } }

    suspend fun setCurrentGroupId(value: String?) {
        appContext.dataStore.edit { prefs ->
            if (value.isNullOrBlank()) prefs.remove(currentGroupIdKey)
            else prefs[currentGroupIdKey] = value
        }
    }
}
