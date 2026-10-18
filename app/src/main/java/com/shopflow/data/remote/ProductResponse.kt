package com.shopflow.data.remote

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    @SerializedName("products") @SerialName("products") val products: List<ProductDto>,
    @SerializedName("total") @SerialName("total") val total: Int
)
