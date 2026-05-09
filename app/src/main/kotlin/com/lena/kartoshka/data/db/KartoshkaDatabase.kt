package com.lena.kartoshka.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ShoppingListEntity::class, ItemEntity::class, PurchaseHistoryEntity::class],
    version = 1
)
abstract class KartoshkaDatabase : RoomDatabase() {

    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun itemDao(): ItemDao
    abstract fun purchaseHistoryDao(): PurchaseHistoryDao

    companion object {
        @Volatile private var INSTANCE: KartoshkaDatabase? = null

        fun get(context: Context): KartoshkaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KartoshkaDatabase::class.java,
                    "kartoshka.db"
                ).build().also { INSTANCE = it }
            }
    }
}
