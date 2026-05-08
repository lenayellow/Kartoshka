package com.lena.kartoshka.data.sort

import kotlinx.coroutines.flow.Flow

// Replace LocalSortRepository with a CloudSortRepository to add multi-user sync.
// The UI layer only depends on this interface and needs no changes.
interface SortRepository {
    fun observeCategoryOrder(): Flow<List<String>>
    fun observeHiddenCategories(): Flow<Set<String>>
    suspend fun saveCategoryOrder(ids: List<String>)
    suspend fun saveHiddenCategories(ids: Set<String>)
}
