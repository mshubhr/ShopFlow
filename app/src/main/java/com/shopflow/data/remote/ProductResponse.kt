package com.shopflow.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    @SerialName("products") val products: List<ProductDto>, @SerialName("total") val total: Int
)
