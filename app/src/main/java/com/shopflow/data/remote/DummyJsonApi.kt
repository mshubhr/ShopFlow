package com.shopflow.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface DummyJsonApi : ShopFlowApi {
    @GET("products")
    override suspend fun getProducts(
        @Query("limit") limit: Int,
        @Query("skip") skip: Int
    ): ProductResponse

    @GET("products/search")
    override suspend fun searchProducts(
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("skip") skip: Int
    ): ProductResponse
}
