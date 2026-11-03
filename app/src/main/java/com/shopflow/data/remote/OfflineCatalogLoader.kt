package com.shopflow.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class OfflineCatalogLoader(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun load(): List<ProductDto> = withContext(Dispatchers.IO) {
        context.assets.open("offline_catalog.json").bufferedReader().use { reader ->
            json.decodeFromString<ProductResponse>(reader.readText()).products
        }
    }
}
