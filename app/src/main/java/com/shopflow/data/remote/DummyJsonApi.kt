package com.shopflow.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface DummyJsonApi {
    @GET("products")
    suspend fun getProducts(@Query("limit") limit: Int = 0): ProductResponse
}