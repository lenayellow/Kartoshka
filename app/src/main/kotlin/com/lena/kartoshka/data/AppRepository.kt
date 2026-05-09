package com.lena.kartoshka.data

import androidx.compose.ui.graphics.Color
import com.lena.kartoshka.data.db.ItemEntity
import com.lena.kartoshka.data.db.KartoshkaDatabase
import com.lena.kartoshka.data.db.PurchaseHistoryEntity
import com.lena.kartoshka.data.db.ShoppingListEntity
import com.lena.kartoshka.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepository(private val db: KartoshkaDatabase) {

    // --- Lists ---

    fun observeLists(): Flow<List<ShoppingList>> =
        db.shoppingListDao().observeAll().map { list -> list.map { it.toShoppingList() } }

    suspend fun insertList(list: ShoppingList) {
        val position = (db.shoppingListDao().maxPosition() ?: -1) + 1
        db.shoppingListDao().insert(
            ShoppingListEntity(list.id, list.name, list.color.value.toLong(), position)
        )
    }

    suspend fun updateList(list: ShoppingList) {
        db.shoppingListDao().update(list.id, list.name, list.color.value.toLong())
    }

    suspend fun deleteList(id: String) {
        db.shoppingListDao().deleteById(id)
    }

    // --- Items ---

    fun observeItems(listId: String): Flow<List<Item>> =
        db.itemDao().observeByListId(listId).map { list -> list.map { it.toItem() } }

    suspend fun insertItem(item: Item, listId: String) {
        db.itemDao().insert(item.toEntity(listId))
    }

    suspend fun insertItems(listId: String, items: List<Item>) {
        db.itemDao().insertAll(items.map { it.toEntity(listId) })
    }

    suspend fun updateItem(item: Item, listId: String) {
        db.itemDao().update(item.toEntity(listId))
    }

    suspend fun deleteItem(itemId: String) {
        db.itemDao().deleteById(itemId)
    }

    suspend fun moveItem(item: Item, toListId: String) {
        db.itemDao().deleteById(item.id)
        db.itemDao().insert(item.toEntity(toListId))
    }

    suspend fun recordPurchase(item: Item, listId: String) {
        db.purchaseHistoryDao().insert(
            PurchaseHistoryEntity(
                itemName = item.name,
                listId = listId,
                purchasedAt = System.currentTimeMillis()
            )
        )
    }

    // --- Seed sample data on first launch ---

    suspend fun seedIfEmpty() {
        if (db.shoppingListDao().count() > 0) return

        val seedLists = listOf(
            ShoppingList("1", "Home", 0, Color(0xFF5B7178)),
            ShoppingList("2", "St.P", 0, Color(0xFFDDA68B)),
            ShoppingList("3", "Holiday Home", 0, Color(0xFF4F8579)),
            ShoppingList("4", "Moscow", 0, Color(0xFF5B7178)),
            ShoppingList("5", "Икеа", 0, Color(0xFF6B8E92))
        )
        val seedItems = mapOf(
            "1" to listOf(Item("1_1", "Молоко")),
            "2" to listOf(
                Item("2_1", "Хлеб"), Item("2_2", "Масло"), Item("2_3", "Яйца"),
                Item("2_4", "Сыр"), Item("2_5", "Йогурт"), Item("2_6", "Апельсины")
            ),
            "3" to listOf(
                Item("3_1", "Уголь"), Item("3_2", "Дрова"), Item("3_3", "Шашлык"),
                Item("3_4", "Овощи"), Item("3_5", "Кетчуп"), Item("3_6", "Пиво"), Item("3_7", "Вода")
            ),
            "4" to listOf(
                Item("4_1", "Кофе"), Item("4_2", "Чай"), Item("4_3", "Печенье"),
                Item("4_4", "Фрукты"), Item("4_5", "Салфетки"), Item("4_6", "Зубная паста"),
                Item("4_7", "Шампунь")
            ),
            "5" to listOf(
                Item("5_1", "Рамки"), Item("5_2", "Ящик"), Item("5_3", "Подушки"),
                Item("5_4", "Лампа"), Item("5_5", "Коврик"), Item("5_6", "Горшки"),
                Item("5_7", "Зеркало"), Item("5_8", "Вешалка"), Item("5_9", "Органайзер"),
                Item("5_10", "Бокалы"), Item("5_11", "Тарелки"), Item("5_12", "Полотенца")
            )
        )

        seedLists.forEach { insertList(it) }
        seedItems.forEach { (listId, items) -> insertItems(listId, items) }
    }
}
