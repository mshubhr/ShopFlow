package com.shopflow.server.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerCartItemDto(
    val productId: Int,
    val title: String,
    val price: Double,
    val thumbnail: String,
    val quantity: Int
)

@Serializable
data class ServerCartResponse(
    val userId: String,
    val items: List<ServerCartItemDto>,
    val totalItems: Int,
    val totalPrice: Double
)

@Serializable
data class AddToCartRequest(
    val productId: Int,
    val quantity: Int = 1
)

@Serializable
data class UpdateCartItemRequest(
    val quantity: Int
)
