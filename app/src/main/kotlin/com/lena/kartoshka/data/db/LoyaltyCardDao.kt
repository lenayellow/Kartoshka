package com.lena.kartoshka.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lena.kartoshka.data.LoyaltyCard
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyCardDao {

    @Query("SELECT * FROM loyalty_cards ORDER BY name ASC")
    fun observeAll(): Flow<List<LoyaltyCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: LoyaltyCard)

    @Query("DELETE FROM loyalty_cards WHERE id = :id")
    suspend fun deleteById(id: String)
}
