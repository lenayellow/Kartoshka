package com.lena.kartoshka.data.db

import androidx.compose.ui.graphics.Color
import androidx.room.ColumnInfo
import com.lena.kartoshka.data.ShoppingList

data class ShoppingListWithCount(
    val id: String,
    val name: String,
    @ColumnInfo(name = "color_value") val colorValue: Long,
    @ColumnInfo(name = "item_count") val itemCount: Int,
    val position: Int
) {
    fun toShoppingList() = ShoppingList(
        id = id,
        name = name,
        itemCount = itemCount,
        color = Color(colorValue.toULong())
    )
}
