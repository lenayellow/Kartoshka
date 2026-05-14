package com.lena.kartoshka.data

data class PendingOpPayload(
    val itemId: String? = null,
    val listId: String? = null,
    val toListId: String? = null,
    val fromListId: String? = null,
    val itemName: String? = null,
    val itemTags: String? = null,
    val itemNote: String? = null,
    val itemCategoryId: String? = null,
    val itemImagePath: String? = null,
    val items: List<ItemPayload>? = null,
    val listName: String? = null,
    val listColor: Long? = null,
    val listPosition: Int? = null
)

data class ItemPayload(
    val id: String,
    val name: String,
    val tags: String,
    val note: String,
    val categoryId: String?,
    val imagePath: String?
)
