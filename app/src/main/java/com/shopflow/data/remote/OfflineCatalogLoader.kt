package com.shopflow.data.remote

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OfflineCatalogLoader(private val context: Context) {
    suspend fun load(): List<ProductDto> = withContext(Dispatchers.IO) {
        context.assets.open("offline_catalog.json").bufferedReader().use { reader ->
            Gson().fromJson(reader, ProductResponse::class.java).products
        }
    }
}
