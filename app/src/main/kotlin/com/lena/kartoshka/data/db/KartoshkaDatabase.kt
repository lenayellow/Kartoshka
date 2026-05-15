package com.lena.kartoshka.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lena.kartoshka.data.LoyaltyCard

@Database(
    entities = [ShoppingListEntity::class, ItemEntity::class, PurchaseHistoryEntity::class, LoyaltyCard::class, PendingOpEntity::class],
    version = 4,
    exportSchema = true
)
abstract class KartoshkaDatabase : RoomDatabase() {

    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun itemDao(): ItemDao
    abstract fun purchaseHistoryDao(): PurchaseHistoryDao
    abstract fun loyaltyCardDao(): LoyaltyCardDao
    abstract fun pendingOpDao(): PendingOpDao

    companion object {
        @Volatile private var INSTANCE: KartoshkaDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN image_path TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS loyalty_cards (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        barcode_value TEXT NOT NULL,
                        barcode_format INTEGER NOT NULL,
                        color INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_ops (
                        id TEXT NOT NULL PRIMARY KEY,
                        op_type TEXT NOT NULL,
                        payload_json TEXT NOT NULL,
                        list_id TEXT,
                        created_at INTEGER NOT NULL,
                        retry_count INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): KartoshkaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KartoshkaDatabase::class.java,
                    "kartoshka.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
            }
    }
}
