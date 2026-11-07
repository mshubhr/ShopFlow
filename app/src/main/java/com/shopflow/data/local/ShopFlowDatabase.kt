package com.shopflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProductEntity::class, CartItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ShopFlowDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cart_items_new (
                        userId TEXT NOT NULL,
                        productId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        price REAL NOT NULL,
                        thumbnail TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        PRIMARY KEY(userId, productId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO cart_items_new (userId, productId, title, price, thumbnail, quantity)
                    SELECT 'guest', productId, title, price, thumbnail, quantity FROM cart_items
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE cart_items")
                db.execSQL("ALTER TABLE cart_items_new RENAME TO cart_items")
            }
        }
    }
}
