package com.lena.kartoshka.data

data class Item(
    val id: String,
    val name: String,
    val tags: Set<ItemTag> = emptySet(),
    val note: String = "",
    val categoryId: String? = null,
    val imagePath: String? = null
)
