package com.shopflow.data.local

import androidx.room.Entity

@Entity(
    tableName = "cart_items", primaryKeys = ["userId", "productId"]
)
data class CartItemEntity(
    val userId: String = DEFAULT_USER_ID,
    val productId: Int,
    val title: String,
    val price: Double,
    val thumbnail: String,
    val quantity: Int
) {
    companion object {
        const val DEFAULT_USER_ID = "guest"
    }
}
