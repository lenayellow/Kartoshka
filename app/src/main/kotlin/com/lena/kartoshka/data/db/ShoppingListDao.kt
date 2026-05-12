package com.lena.kartoshka.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Query("""
        SELECT s.id, s.name, s.color_value, s.position, COUNT(i.id) as item_count
        FROM shopping_lists s
        LEFT JOIN items i ON i.list_id = s.id
        GROUP BY s.id
        ORDER BY s.position ASC, s.rowid ASC
    """)
    fun observeAll(): Flow<List<ShoppingListWithCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: ShoppingListEntity)

    @Query("UPDATE shopping_lists SET name = :name, color_value = :colorValue WHERE id = :id")
    suspend fun update(id: String, name: String, colorValue: Long)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM shopping_lists")
    suspend fun getAll(): List<ShoppingListEntity>

    @Query("SELECT COUNT(*) FROM shopping_lists")
    suspend fun count(): Int

    @Query("SELECT MAX(position) FROM shopping_lists")
    suspend fun maxPosition(): Int?
}
