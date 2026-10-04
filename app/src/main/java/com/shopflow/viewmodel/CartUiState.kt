package com.shopflow.viewmodel

import androidx.compose.runtime.Immutable
import com.shopflow.data.CartItem

@Immutable
data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val stockByProductId: Map<Int, Int> = emptyMap()
) {
    val itemCount: Int get() = items.sumOf(CartItem::quantity)
    val total: Double get() = items.sumOf { it.price * it.quantity }

    fun quantityFor(productId: Int): Int = items.firstOrNull { it.productId == productId }?.quantity ?: 0

    fun canIncrease(productId: Int): Boolean = quantityFor(productId) < (stockByProductId[productId] ?: 0)
}
