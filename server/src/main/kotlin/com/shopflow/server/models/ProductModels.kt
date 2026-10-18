package com.shopflow.server.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerProductDto(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val rating: Double,
    val category: String,
    val brand: String? = null,
    val stock: Int,
    val thumbnail: String
)

@Serializable
data class ServerProductResponse(
    val products: List<ServerProductDto>,
    val total: Int,
    val skip: Int = 0,
    val limit: Int = products.size
)

@Serializable
data class ServerStatus(
    val status: String = "OK",
    val app: String = "ShopFlow Ktor API",
    val version: String = "1.0.0",
    val totalProducts: Int
)
