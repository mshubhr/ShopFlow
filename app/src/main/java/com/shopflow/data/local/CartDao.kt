package com.shopflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE userId = :userId ORDER BY title COLLATE NOCASE")
    fun observeAll(userId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE userId = 'guest' ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId")
    suspend fun findById(userId: String, productId: Int): CartItemEntity?

    @Query("SELECT * FROM cart_items WHERE userId = 'guest' AND productId = :productId")
    suspend fun findById(productId: Int): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE userId = :userId AND productId = :productId")
    suspend fun delete(userId: String, productId: Int)

    @Query("DELETE FROM cart_items WHERE userId = 'guest' AND productId = :productId")
    suspend fun delete(productId: Int)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clear(userId: String)

    @Query("SELECT COUNT(*) FROM cart_items WHERE userId = :userId")
    suspend fun count(userId: String): Int
}
