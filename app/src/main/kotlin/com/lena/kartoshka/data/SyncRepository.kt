package com.lena.kartoshka.data

import com.lena.kartoshka.data.db.ItemEntity
import com.lena.kartoshka.data.db.KartoshkaDatabase
import com.lena.kartoshka.data.db.ShoppingListEntity
import com.lena.kartoshka.network.ApiService

class SyncRepository(
    private val api: ApiService,
    private val db: KartoshkaDatabase
) {

    suspend fun syncLists(): Result<Unit> = runCatching {
        val serverLists = api.getLists()
        val serverIds = serverLists.map { it.list_id }.toSet()

        // Скачиваем серверные списки в локальную БД
        serverLists.forEach { apiList ->
            db.shoppingListDao().insert(
                ShoppingListEntity(
                    id = apiList.list_id,
                    name = apiList.title,
                    colorValue = apiList.color_value,
                    position = apiList.position
                )
            )
        }

        // Загружаем локальные списки которых нет на сервере
        val localLists = db.shoppingListDao().getAll()
        localLists.filter { it.id !in serverIds }.forEach { local ->
            runCatching {
                api.createList(
                    com.lena.kartoshka.network.CreateListRequest(
                        list_id = local.id,
                        title = local.name,
                        color_value = local.colorValue,
                        position = local.position
                    )
                )
            }
        }
    }

    suspend fun syncItems(listId: String): Result<Unit> = runCatching {
        val items = api.getItems(listId)
        val entities = items
            .filter { !it.is_deleted }
            .map { api ->
                ItemEntity(
                    id = api.item_id,
                    listId = api.list_id,
                    name = api.name,
                    tags = api.tags ?: "",
                    note = api.note ?: "",
                    categoryId = api.category_id?.ifBlank { null }
                )
            }
        db.itemDao().insertAll(entities)
    }
}
