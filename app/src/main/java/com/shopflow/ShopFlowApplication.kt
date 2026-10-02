package com.shopflow

import android.app.Application
import androidx.room.Room
import com.shopflow.data.ProductRepository
import com.shopflow.data.local.ShopFlowDatabase
import com.shopflow.data.remote.DummyJsonApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ShopFlowApplication : Application() {
    val repository: ProductRepository by lazy {
        val database = Room.databaseBuilder(this, ShopFlowDatabase::class.java, "shopflow.db").build()
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        val api = Retrofit.Builder()
            .baseUrl("https://dummyjson.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DummyJsonApi::class.java)
        ProductRepository(api, database.productDao(), database.cartDao())
    }
}
