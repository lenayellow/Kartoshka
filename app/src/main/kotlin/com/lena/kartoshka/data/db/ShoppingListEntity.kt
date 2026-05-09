package com.lena.kartoshka.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "color_value") val colorValue: Long,
    val position: Int = 0
)
