package com.shopflow.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val rating: Double,
    val category: String,
    val brand: String?,
    val stock: Int,
    val thumbnail: String
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: Int,
    val title: String,
    val price: Double,
    val thumbnail: String,
    val quantity: Int
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId")
    fun observeById(productId: Int): Flow<ProductEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(products: List<ProductEntity>) {
        clear()
        insertAll(products)
    }
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId")
    suspend fun findById(productId: Int): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun delete(productId: Int)
}

@Database(entities = [ProductEntity::class, CartItemEntity::class], version = 1, exportSchema = false)
abstract class ShopFlowDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
}
