package com.lena.kartoshka.data

import androidx.compose.ui.graphics.Color

data class ShoppingList(
    val id: String,
    val name: String,
    val itemCount: Int,
    val color: Color
)

val sampleLists: List<ShoppingList> = listOf(
    ShoppingList("1", "Home", 1, Color(0xFF5B7178)),
    ShoppingList("2", "St.P", 6, Color(0xFFDDA68B)),
    ShoppingList("3", "Holiday Home", 7, Color(0xFF4F8579)),
    ShoppingList("4", "Moscow", 7, Color(0xFF5B7178)),
    ShoppingList("5", "Икеа", 12, Color(0xFF6B8E92))
)
