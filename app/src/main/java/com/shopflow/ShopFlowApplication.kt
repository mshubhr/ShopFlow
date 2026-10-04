package com.shopflow

import android.app.Application
import androidx.room.Room
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.shopflow.data.repository.ProductRepository
import com.shopflow.data.local.ShopFlowDatabase
import com.shopflow.data.remote.DummyJsonApi
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ShopFlowApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context).memoryCache {
            MemoryCache.Builder().maxSizePercent(context, 0.25).build()
        }.diskCache {
            DiskCache.Builder().directory(cacheDir.resolve("image_cache").toOkioPath())
                .maxSizeBytes(100L * 1024 * 1024).build()
        }.crossfade(true).build()
    }

    val repository: ProductRepository by lazy {
        val database = Room.databaseBuilder(this, ShopFlowDatabase::class.java, "shopflow.db").build()
        val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS).build()
        val api = Retrofit.Builder().baseUrl("https://dummyjson.com/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(DummyJsonApi::class.java)
        ProductRepository(api, database.productDao(), database.cartDao())
    }
}