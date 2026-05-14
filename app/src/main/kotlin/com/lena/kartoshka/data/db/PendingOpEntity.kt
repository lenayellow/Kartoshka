package com.lena.kartoshka.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey val id: String,
    val op_type: String,
    val payload_json: String,
    val list_id: String?,
    val created_at: Long,
    val retry_count: Int = 0
) {
    companion object {
        const val INSERT_ITEM        = "INSERT_ITEM"
        const val UPDATE_ITEM        = "UPDATE_ITEM"
        const val DELETE_ITEM        = "DELETE_ITEM"
        const val INSERT_LIST        = "INSERT_LIST"
        const val UPDATE_LIST        = "UPDATE_LIST"
        const val DELETE_LIST        = "DELETE_LIST"
        const val MOVE_ITEM          = "MOVE_ITEM"
        const val INSERT_ITEMS_BATCH = "INSERT_ITEMS_BATCH"
    }
}
