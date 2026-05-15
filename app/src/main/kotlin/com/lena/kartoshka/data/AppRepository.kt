package com.lena.kartoshka.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.lena.kartoshka.analytics.Analytics
import com.lena.kartoshka.data.db.ItemEntity
import com.lena.kartoshka.data.db.KartoshkaDatabase
import com.lena.kartoshka.data.db.PendingOpEntity
import com.lena.kartoshka.data.db.PurchaseHistoryEntity
import com.lena.kartoshka.data.db.ShoppingListEntity
import com.lena.kartoshka.data.db.toEntity
import com.lena.kartoshka.data.sync.SyncPendingOpsWorker
import com.lena.kartoshka.network.ApiService
import com.lena.kartoshka.network.CreateInviteRequest
import com.lena.kartoshka.network.CreateItemRequest
import com.lena.kartoshka.network.CreateListRequest
import com.lena.kartoshka.network.UpdateItemRequest
import com.lena.kartoshka.network.UpdateListRequest
import com.lena.kartoshka.network.isRetryable
import com.lena.kartoshka.network.toNetworkError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class AppRepository(
    private val db: KartoshkaDatabase,
    private val api: ApiService? = null,
    private val context: Context? = null
) {
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val pendingOpDao = db.pendingOpDao()

    private suspend fun savePendingAndEnqueue(op: PendingOpEntity) {
        pendingOpDao.insert(op)
        context?.let { SyncPendingOpsWorker.enqueue(it) }
    }

    // --- Lists ---

    fun observeLists(): Flow<List<ShoppingList>> =
        db.shoppingListDao().observeAll().map { list -> list.map { it.toShoppingList() } }

    suspend fun insertList(list: ShoppingList) {
        val position = (db.shoppingListDao().maxPosition() ?: -1) + 1
        db.shoppingListDao().insert(
            ShoppingListEntity(list.id, list.name, list.color.value.toLong(), position)
        )
        api?.let { api -> bgScope.launch {
            try {
                api.createList(CreateListRequest(list.id, list.name, list.color.value.toLong(), position))
            } catch (e: Exception) {
                val err = e.toNetworkError()
                if (err.isRetryable()) {
                    savePendingAndEnqueue(PendingOpEntity(
                        id = UUID.randomUUID().toString(),
                        op_type = PendingOpEntity.INSERT_LIST,
                        payload_json = gson.toJson(PendingOpPayload(
                            listId = list.id, listName = list.name,
                            listColor = list.color.value.toLong(), listPosition = position
                        )),
                        list_id = list.id,
                        created_at = System.currentTimeMillis()
                    ))
                } else {
                    Analytics.trackError(e, "AppRepository.insertList: ${err::class.simpleName} code=${err.httpCode}")
                }
            }
        }}
    }

    suspend fun updateList(list: ShoppingList) {
        db.shoppingListDao().update(list.id, list.name, list.color.value.toLong())
        api?.let { api -> bgScope.launch {
            try {
                api.updateList(list.id, UpdateListRequest(list.name, list.color.value.toLong()))
            } catch (e: Exception) {
                val err = e.toNetworkError()
                if (err.isRetryable()) {
                    savePendingAndEnqueue(PendingOpEntity(
                        id = UUID.randomUUID().toString(),
                        op_type = PendingOpEntity.UPDATE_LIST,
                        payload_json = gson.toJson(PendingOpPayload(
                            listId = list.id, listName = list.name,
                            listColor = list.color.value.toLong()
                        )),
                        list_id = list.id,
                        created_at = System.currentTimeMillis()
                    ))
                } else {
                    Analytics.trackError(e, "AppRepository.updateList: ${err::class.simpleName} code=${err.httpCode}")
                }
            }
        }}
    }

    suspend fun deleteList(id: String) {
        db.shoppingListDao().deleteById(id)
        api?.let { api -> bgScope.launch {
            try {
                api.deleteList(id)
            } catch (e: Exception) {
                val err = e.toNetworkError()
                if (err.isRetryable()) {
                    savePendingAndEnqueue(PendingOpEntity(
                        id = UUID.randomUUID().toString(),
                        op_type = PendingOpEntity.DELETE_LIST,
                        payload_json = gson.toJson(PendingOpPayload(listId = id)),
                        list_id = id,
                        created_at = System.currentTimeMillis()
                    ))
                } else {
                    Analytics.trackError(e, "AppRepository.deleteList: ${err::class.simpleName} code=${err.httpCode}")
                }
            }
        }}
    }

    // --- Items ---

    fun observeItems(listId: String): Flow<List<Item>> =
        db.itemDao().observeByListId(listId).map { list -> list.map { it.toItem() } }

    suspend fun insertItem(item: Item, listId: String) {
        db.itemDao().insert(item.toEntity(listId))
        api?.let { api -> bgScope.launch {
            try {
                api.createItem(listId, CreateItemRequest(
                    name = item.name,
                    tags = item.tags.joinToString(",") { it.name },
                    note = item.note,
                    category_id = item.categoryId ?: ""
                ))
            } catch (e: Exception) {
                val err = e.toNetworkError()
                if (err.isRetryable()) {
                    savePendingAndEnqueue(PendingOpEntity(
                        id = UUID.randomUUID().toString(),
                        op_type = PendingOpEntity.INSERT_ITEM,
                        payload_json = gson.toJson(PendingOpPayload(
                            itemId = item.id, listId = listId,
                            itemName = item.name,
                            itemTags = item.tags.joinToString(",") { it.name },
                            itemNote = item.note,
                            itemCategoryId = item.categoryId,
                            itemImagePath = item.imagePath
                        )),
                        list_id = listId,
                        created_at = System.currentTimeMillis()
                    ))
                } else {
                    Analytics.trackError(e, "AppRepository.insertItem: ${err::class.simpleName} code=${err.httpCode}")
                }
            }
        }}
    }

    suspend fun insertItems(listId: String, items: List<Item>) {
        db.itemDao().insertAll(items.map { it.toEntity(listId) })
        api?.let { api -> bgScope.launch {
            items.forEach { item ->
                try {
                    api.createItem(listId, CreateItemRequest(
                        name = item.name,
                        tags = item.tags.joinToString(",") { it.name },
                        note = item.note,
                        category_id = item.categoryId ?: ""
                    ))
                } catch (e: Exception) {
                    val err = e.toNetworkError()
                    if (err.isRetryable()) {
                        savePendingAndEnqueue(PendingOpEntity(
                            id = UUID.randomUUID().toString(),
                            op_type = PendingOpEntity.INSERT_ITEM,
                            payload_json = gson.toJson(PendingOpPayload(
                                itemId = item.id, listId = listId,
                                itemName = item.name,
                                itemTags = item.tags.joinToString(",") { it.name },
                                itemNote = item.note,
                                itemCategoryId = item.categoryId,
                                itemImagePath = item.imagePath
                            )),
                            list_id = listId,
                            created_at = System.currentTimeMillis()
                        ))
                    } else {
                        Analytics.trackError(e, "AppRepository.insertItems: ${err::class.simpleName} code=${err.httpCode}")
                    }
                }
            }
        }}
    }

    suspend fun updateItem(item: Item, listId: String) {
        db.itemDao().update(item.toEntity(listId))
        api?.let { api -> bgScope.launch {
            try {
                api.updateItem(listId, item.id, UpdateItemRequest(
                    name = item.name,
                    tags = item.tags.joinToString(",") { it.name },
                    note = item.note,
                    category_id = item.categoryId ?: ""
                ))
            } catch (e: Exception) {
                val err = e.toNetworkError()
                if (err.isRetryable()) {
                    savePendingAndEnqueue(PendingOpEntity(
                        id = UUID.randomUUID().toString(),
                        op_type = PendingOpEntity.UPDATE_ITEM,
                        payload_json = gson.toJson(PendingOpPayload(
                            itemId = item.id, listId = listId,
                            itemName = item.name,
                            itemTags = item.tags.joinToString(",") { it.name },
                            itemNote = item.note,
                            itemCategoryId = item.categoryId,
                            itemImagePath = item.imagePath
                        )),
                        list_id = listId,
                        created_at = System.currentTimeMillis()
                    ))
                } else {
                    Analytics.trackError(e, "AppRepository.updateItem: ${err::class.simpleName} code=${err.httpCode}")
                }
            }
        }}
    }

    suspend fun deleteItem(itemId: String) {
        val entity = db.itemDao().getById(itemId)
        db.itemDao().deleteById(itemId)
        api?.let { api -> entity?.let { e -> bgScope.launch {
            try {
                api.deleteItem(e.listId, itemId)
            } catch (ex: Exception) {
                val err = ex.toNetworkError()
                if (err.isRetryable()) {
                    savePendingAndEnqueue(PendingOpEntity(
                        id = UUID.randomUUID().toString(),
                        op_type = PendingOpEntity.DELETE_ITEM,
                        payload_json = gson.toJson(PendingOpPayload(
                            itemId = itemId, listId = e.listId
                        )),
                        list_id = e.listId,
                        created_at = System.currentTimeMillis()
                    ))
                } else {
                    Analytics.trackError(ex, "AppRepository.deleteItem: ${err::class.simpleName} code=${err.httpCode}")
                }
            }
        }}}
    }

    suspend fun moveItem(item: Item, toListId: String) {
        val fromListId = db.itemDao().getById(item.id)?.listId
        db.itemDao().deleteById(item.id)
        db.itemDao().insert(item.toEntity(toListId))
        api?.let { api -> bgScope.launch {
            try {
                fromListId?.let { api.deleteItem(it, item.id) }
                api.createItem(toListId, CreateItemRequest(
                    item.name,
                    item.tags.joinToString(",") { it.name },
                    item.note,
                    item.categoryId ?: ""
                ))
            } catch (e: Exception) {
                val err = e.toNetworkError()
                if (err.isRetryable()) {
                    savePendingAndEnqueue(PendingOpEntity(
                        id = UUID.randomUUID().toString(),
                        op_type = PendingOpEntity.MOVE_ITEM,
                        payload_json = gson.toJson(PendingOpPayload(
                            itemId = item.id,
                            fromListId = fromListId,
                            toListId = toListId,
                            itemName = item.name,
                            itemTags = item.tags.joinToString(",") { it.name },
                            itemNote = item.note,
                            itemCategoryId = item.categoryId,
                            itemImagePath = item.imagePath
                        )),
                        list_id = toListId,
                        created_at = System.currentTimeMillis()
                    ))
                } else {
                    Analytics.trackError(e, "AppRepository.moveItem: ${err::class.simpleName} code=${err.httpCode}")
                }
            }
        }}
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

    // --- Loyalty cards ---

    fun observeLoyaltyCards(): Flow<List<LoyaltyCard>> =
        db.loyaltyCardDao().observeAll()

    suspend fun insertLoyaltyCard(card: LoyaltyCard) {
        db.loyaltyCardDao().insert(card)
    }

    suspend fun deleteLoyaltyCard(id: String) {
        db.loyaltyCardDao().deleteById(id)
    }

    // --- Members & invites ---

    suspend fun getMembers(listId: String): List<ListMember> =
        runCatching {
            api?.getMembers(listId)?.map {
                ListMember(it.user_id, it.name, it.email, it.avatar_url, it.role)
            }
        }.onFailure { e ->
            val err = e.toNetworkError()
            Analytics.trackError(e, "AppRepository.getMembers: ${err::class.simpleName} code=${err.httpCode}")
        }.getOrNull() ?: emptyList()

    suspend fun removeMember(listId: String, userId: String) {
        runCatching { api?.removeMember(listId, userId) }.onFailure { e ->
            val err = e.toNetworkError()
            Analytics.trackError(e, "AppRepository.removeMember: ${err::class.simpleName} code=${err.httpCode}")
        }
    }

    suspend fun createInvite(listId: String, email: String = ""): InviteResult? =
        runCatching {
            val resp = api?.createInvite(listId, CreateInviteRequest(email)) ?: return@runCatching null
            InviteResult(resp.web_link, resp.deep_link, resp.email_sent)
        }.onFailure { e ->
            val err = e.toNetworkError()
            Analytics.trackError(e, "AppRepository.createInvite: ${err::class.simpleName} code=${err.httpCode}")
        }.getOrNull()

    // --- Current user ---

    suspend fun getCurrentUser() = runCatching { api?.getMe() }.getOrNull()

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
