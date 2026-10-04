package com.shopflow.data

import androidx.compose.runtime.Immutable

@Immutable
data class CartItem(
    val productId: Int,
    val title: String,
    val price: Double,
    val thumbnail: String,
    val quantity: Int
)