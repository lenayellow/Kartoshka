package com.lena.kartoshka.data

import androidx.compose.ui.graphics.Color

data class ShoppingList(
    val id: String,
    val name: String,
    val itemCount: Int,
    val color: Color
)
