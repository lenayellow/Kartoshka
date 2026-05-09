package com.lena.kartoshka.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_history")
data class PurchaseHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "item_name") val itemName: String,
    @ColumnInfo(name = "list_id") val listId: String,
    @ColumnInfo(name = "purchased_at") val purchasedAt: Long
)
