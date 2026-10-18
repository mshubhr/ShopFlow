package com.shopflow.data.remote

interface ShopFlowApi {
    suspend fun getProducts(
        limit: Int,
        skip: Int
    ): ProductResponse

    suspend fun searchProducts(
        query: String,
        limit: Int = 0,
        skip: Int = 0
    ): ProductResponse
}
