package com.shopflow.viewmodel

import androidx.compose.runtime.Immutable
import com.shopflow.data.CartItem

@Immutable
data class CartUiState(val items: List<CartItem> = emptyList()) {
    val itemCount: Int get() = items.sumOf(CartItem::quantity)
    val total: Double get() = items.sumOf { it.price * it.quantity }
}