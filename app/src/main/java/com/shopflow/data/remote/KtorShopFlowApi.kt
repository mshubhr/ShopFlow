package com.shopflow.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class KtorShopFlowApi(
    private val client: HttpClient,
    private val baseUrl: String = DEFAULT_EMULATOR_BASE_URL
) : ShopFlowApi {

    override suspend fun getProducts(limit: Int, skip: Int): ProductResponse {
        return client.get("$baseUrl/products") {
            parameter("limit", limit)
            parameter("skip", skip)
        }.body()
    }

    override suspend fun searchProducts(query: String, limit: Int, skip: Int): ProductResponse {
        return client.get("$baseUrl/products/search") {
            parameter("q", query)
            if (limit > 0) parameter("limit", limit)
            if (skip > 0) parameter("skip", skip)
        }.body()
    }

    companion object {
        const val DEFAULT_EMULATOR_BASE_URL = "http://10.0.2.2:8080"
        const val DEFAULT_LOCAL_BASE_URL = "http://127.0.0.1:8080"

        fun createDefaultHttpClient(engine: HttpClientEngine? = null): HttpClient {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            }

            return if (engine != null) {
                HttpClient(engine) {
                    install(ContentNegotiation) {
                        json(jsonConfig)
                    }
                }
            } else {
                HttpClient(OkHttp) {
                    install(ContentNegotiation) {
                        json(jsonConfig)
                    }
                    engine {
                        config {
                            connectTimeout(10, TimeUnit.SECONDS)
                            readTimeout(15, TimeUnit.SECONDS)
                        }
                    }
                }
            }
        }
    }
}
