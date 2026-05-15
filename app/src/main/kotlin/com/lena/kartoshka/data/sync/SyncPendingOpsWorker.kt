package com.lena.kartoshka.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.lena.kartoshka.analytics.Analytics
import com.lena.kartoshka.data.PendingOpPayload
import com.lena.kartoshka.data.TokenStore
import com.lena.kartoshka.data.db.KartoshkaDatabase
import com.lena.kartoshka.data.db.PendingOpEntity
import com.lena.kartoshka.network.ApiClient
import com.lena.kartoshka.network.CreateItemRequest
import com.lena.kartoshka.network.CreateListRequest
import com.lena.kartoshka.network.UpdateItemRequest
import com.lena.kartoshka.network.UpdateListRequest
import com.lena.kartoshka.network.isRetryable
import com.lena.kartoshka.network.toNetworkError
import java.util.concurrent.TimeUnit

class SyncPendingOpsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db   = KartoshkaDatabase.get(context)
    private val gson = Gson()

    override suspend fun doWork(): Result {
        if (!ApiClient.isInitialized) ApiClient.init(TokenStore(applicationContext))
        val api = ApiClient.api

        val ops = db.pendingOpDao().getAll()
        if (ops.isEmpty()) return Result.success()

        var needRetry = false

        for (op in ops) {
            if (op.retry_count >= 10) {
                db.pendingOpDao().deleteById(op.id)
                Analytics.trackEvent("pending_op_exhausted", mapOf("op_type" to op.op_type))
                continue
            }

            val p = gson.fromJson(op.payload_json, PendingOpPayload::class.java)

            try {
                when (op.op_type) {
                    PendingOpEntity.INSERT_ITEM ->
                        api.createItem(p.listId!!, CreateItemRequest(
                            p.itemName!!, p.itemTags ?: "", p.itemNote ?: "", p.itemCategoryId ?: ""))

                    PendingOpEntity.UPDATE_ITEM ->
                        api.updateItem(p.listId!!, p.itemId!!, UpdateItemRequest(
                            p.itemName!!, p.itemTags ?: "", p.itemNote ?: "", p.itemCategoryId ?: ""))

                    PendingOpEntity.DELETE_ITEM ->
                        api.deleteItem(p.listId!!, p.itemId!!)

                    PendingOpEntity.INSERT_LIST ->
                        api.createList(CreateListRequest(
                            p.listId!!, p.listName!!, p.listColor!!, p.listPosition ?: 0))

                    PendingOpEntity.UPDATE_LIST ->
                        api.updateList(p.listId!!, UpdateListRequest(p.listName!!, p.listColor!!))

                    PendingOpEntity.DELETE_LIST ->
                        api.deleteList(p.listId!!)

                    PendingOpEntity.MOVE_ITEM -> {
                        p.fromListId?.let { api.deleteItem(it, p.itemId!!) }
                        api.createItem(p.toListId!!, CreateItemRequest(
                            p.itemName!!, p.itemTags ?: "", p.itemNote ?: "", p.itemCategoryId ?: ""))
                    }

                    PendingOpEntity.INSERT_ITEMS_BATCH ->
                        p.items!!.forEach { item ->
                            api.createItem(p.listId!!, CreateItemRequest(
                                item.name, item.tags, item.note, item.categoryId ?: ""))
                        }
                }
                db.pendingOpDao().deleteById(op.id)

            } catch (e: Exception) {
                val err = e.toNetworkError()
                if (err.isRetryable()) {
                    db.pendingOpDao().incrementRetry(op.id)
                    needRetry = true
                } else {
                    db.pendingOpDao().deleteById(op.id)
                    Analytics.trackError(e,
                        "SyncWorker: permanent error op=${op.op_type} ${err::class.simpleName}")
                }
            }
        }

        return if (needRetry) Result.retry() else Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncPendingOpsWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("sync_pending_ops", ExistingWorkPolicy.KEEP, request)
        }
    }
}
