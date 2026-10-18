package com.shopflow

import android.app.Application
import androidx.room.Room
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.shopflow.data.local.ShopFlowDatabase
import com.shopflow.data.remote.KtorShopFlowApi
import com.shopflow.data.remote.OfflineCatalogLoader
import com.shopflow.data.repository.ProductRepository
import okio.Path.Companion.toOkioPath

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
        val database = Room.databaseBuilder(
            this, ShopFlowDatabase::class.java, "shopflow.db"
        ).build()

        val ktorClient = KtorShopFlowApi.createDefaultHttpClient()
        val api = KtorShopFlowApi(
            client = ktorClient,
            baseUrl = KtorShopFlowApi.DEFAULT_EMULATOR_BASE_URL
        )

        ProductRepository(api, database.productDao(), database.cartDao()) {
            OfflineCatalogLoader(this).load()
        }
    }
}
