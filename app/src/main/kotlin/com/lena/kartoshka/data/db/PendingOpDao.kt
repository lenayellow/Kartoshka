package com.lena.kartoshka.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingOpDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(op: PendingOpEntity)

    @Query("SELECT * FROM pending_ops ORDER BY created_at ASC")
    suspend fun getAll(): List<PendingOpEntity>

    @Query("DELETE FROM pending_ops WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE pending_ops SET retry_count = retry_count + 1 WHERE id = :id")
    suspend fun incrementRetry(id: String)

    @Query("DELETE FROM pending_ops WHERE retry_count >= :max")
    suspend fun deleteExhausted(max: Int)

    @Query("DELETE FROM pending_ops WHERE list_id IN (:listIds)")
    suspend fun deleteByListIds(listIds: List<String>)
}
