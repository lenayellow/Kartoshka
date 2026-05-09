package com.lena.kartoshka.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lastUsedDataStore by preferencesDataStore(name = "last_used_prefs")

class LastUsedRepository(private val context: Context) {

    private val KEY_LIST_ID = stringPreferencesKey("last_list_id")

    fun observeLastListId(): Flow<String> =
        context.lastUsedDataStore.data.map { prefs ->
            prefs[KEY_LIST_ID] ?: ""
        }

    suspend fun saveLastListId(listId: String) {
        context.lastUsedDataStore.edit { prefs ->
            prefs[KEY_LIST_ID] = listId
        }
    }
}
