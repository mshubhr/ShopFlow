package com.shopflow.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class KtorShopFlowApi(
    private val client: HttpClient, private val baseUrl: String = DEFAULT_EMULATOR_BASE_URL
) : ShopFlowApi, ShopFlowAuthApi {

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

    override suspend fun login(request: LoginRequest): AuthResponse {
        return client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun register(request: RegisterRequest): AuthResponse {
        return client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun logout(token: String): AuthMessageResponse {
        return client.post("$baseUrl/auth/logout") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    override suspend fun getProfile(token: String): UserProfileDto {
        return client.get("$baseUrl/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    override suspend fun updateProfile(
        token: String, request: UpdateProfileRequest
    ): UserProfileDto {
        return client.put("$baseUrl/users/profile") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun changePassword(
        token: String, request: ChangePasswordRequest
    ): AuthMessageResponse {
        return client.post("$baseUrl/users/profile/change-password") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    companion object {
        const val DEFAULT_EMULATOR_BASE_URL = "http://10.0.2.2:8080"

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
