package com.shopflow.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
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