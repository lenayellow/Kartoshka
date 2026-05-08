package com.lena.kartoshka.data.sort

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sortDataStore by preferencesDataStore(name = "sort_prefs")

class LocalSortRepository(private val context: Context) : SortRepository {

    private val KEY_ORDER = stringPreferencesKey("category_order")
    private val KEY_HIDDEN = stringPreferencesKey("hidden_categories")

    override fun observeCategoryOrder(): Flow<List<String>> =
        context.sortDataStore.data.map { prefs ->
            prefs[KEY_ORDER]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        }

    override fun observeHiddenCategories(): Flow<Set<String>> =
        context.sortDataStore.data.map { prefs ->
            prefs[KEY_HIDDEN]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        }

    override suspend fun saveCategoryOrder(ids: List<String>) {
        context.sortDataStore.edit { prefs ->
            prefs[KEY_ORDER] = ids.joinToString(",")
        }
    }

    override suspend fun saveHiddenCategories(ids: Set<String>) {
        context.sortDataStore.edit { prefs ->
            prefs[KEY_HIDDEN] = ids.joinToString(",")
        }
    }
}
