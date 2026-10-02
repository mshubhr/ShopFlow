package com.shopflow.data

data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val rating: Double,
    val category: String,
    val brand: String?,
    val stock: Int,
    val thumbnail: String
)

data class CartItem(
    val productId: Int,
    val title: String,
    val price: Double,
    val thumbnail: String,
    val quantity: Int
)
