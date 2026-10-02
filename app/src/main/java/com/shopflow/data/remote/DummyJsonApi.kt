package com.shopflow.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface DummyJsonApi {
    @GET("products")
    suspend fun getProducts(@Query("limit") limit: Int = 0): ProductResponse
}

data class ProductResponse(
    @SerializedName("products") val products: List<ProductDto>
)

data class ProductDto(
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
