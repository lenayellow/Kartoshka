package com.lena.kartoshka.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lena.kartoshka.data.Item
import com.lena.kartoshka.data.ItemTag

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(
        entity = ShoppingListEntity::class,
        parentColumns = ["id"],
        childColumns = ["list_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("list_id")]
)
data class ItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "list_id") val listId: String,
    val name: String,
    val tags: String = "",
    val note: String = "",
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    @ColumnInfo(name = "image_path") val imagePath: String? = null
) {
    fun toItem() = Item(
        id = id,
        name = name,
        tags = tags.toTagSet(),
        note = note,
        categoryId = categoryId,
        imagePath = imagePath
    )
}

fun Item.toEntity(listId: String) = ItemEntity(
    id = id,
    listId = listId,
    name = name,
    tags = tags.joinToString(",") { it.name },
    note = note,
    categoryId = categoryId,
    imagePath = imagePath
)

private fun String.toTagSet(): Set<ItemTag> =
    split(",").filter { it.isNotBlank() }
        .mapNotNull { runCatching { ItemTag.valueOf(it) }.getOrNull() }
        .toSet()
