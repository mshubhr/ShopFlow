package com.shopflow.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorShopFlowApiTest {

    private val sampleJsonResponse = """
        {
          "products": [
            {
              "id": 1,
              "title": "Ktor Wireless Earbuds",
              "description": "High fidelity earbuds powered by custom Ktor backend",
              "price": 79.99,
              "rating": 4.8,
              "category": "electronics",
              "brand": "KtorTech",
              "stock": 20,
              "thumbnail": "https://example.com/earbuds.png"
            }
          ],
          "total": 1,
          "skip": 0,
          "limit": 24
        }
    """.trimIndent()

    @Test
    fun getProducts_callsEndpointAndDeserializesResponse() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/products", request.url.encodedPath)
            assertEquals("10", request.url.parameters["limit"])
            assertEquals("0", request.url.parameters["skip"])

            respond(
                content = sampleJsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorShopFlowApi.createDefaultHttpClient(mockEngine)
        val api = KtorShopFlowApi(client, baseUrl = "http://test.local")

        val response = api.getProducts(limit = 10, skip = 0)

        assertEquals(1, response.total)
        assertEquals(1, response.products.size)
        val product = response.products[0]
        assertEquals(1, product.id)
        assertEquals("Ktor Wireless Earbuds", product.title)
        assertEquals(79.99, product.price, 0.001)
        assertEquals("electronics", product.category)
    }

    @Test
    fun searchProducts_callsSearchEndpointWithQueryParam() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/products/search", request.url.encodedPath)
            assertEquals("earbuds", request.url.parameters["q"])

            respond(
                content = sampleJsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorShopFlowApi.createDefaultHttpClient(mockEngine)
        val api = KtorShopFlowApi(client, baseUrl = "http://test.local")

        val response = api.searchProducts(query = "earbuds", limit = 10, skip = 0)

        assertEquals(1, response.total)
        assertTrue(response.products.any { it.title.contains("Earbuds") })
    }

    @Test
    fun login_postsCredentialsAndReturnsAuthResponse() = runTest {
        val authJsonResponse = """
            {
              "token": "mock-test-token-12345",
              "user": {
                "id": "user_1",
                "username": "demo_user",
                "email": "demo@shopflow.com",
                "fullName": "Alex Taylor",
                "memberSince": "2024-01-15"
              },
              "expiresInSeconds": 2592000
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("/auth/login", request.url.encodedPath)
            respond(
                content = authJsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorShopFlowApi.createDefaultHttpClient(mockEngine)
        val api = KtorShopFlowApi(client, baseUrl = "http://test.local")

        val response = api.login(LoginRequest("demo_user", "Password123!"))
        assertEquals("mock-test-token-12345", response.token)
        assertEquals("user_1", response.user.id)
        assertEquals("Alex Taylor", response.user.fullName)
    }

    @Test
    fun getProfile_sendsBearerTokenAndReturnsProfile() = runTest {
        val profileJsonResponse = """
            {
              "id": "user_1",
              "username": "demo_user",
              "email": "demo@shopflow.com",
              "fullName": "Alex Taylor",
              "bio": "Online shopping fan",
              "phone": "+1 555-1234",
              "address": {
                "street": "123 Main St",
                "city": "Springfield",
                "state": "OR",
                "postalCode": "97477",
                "country": "USA"
              },
              "memberSince": "2024-01-15"
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("/auth/me", request.url.encodedPath)
            assertEquals("Bearer sample-token", request.headers[HttpHeaders.Authorization])
            respond(
                content = profileJsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorShopFlowApi.createDefaultHttpClient(mockEngine)
        val api = KtorShopFlowApi(client, baseUrl = "http://test.local")

        val profile = api.getProfile("sample-token")
        assertEquals("user_1", profile.id)
        assertEquals("Alex Taylor", profile.fullName)
        assertEquals("Springfield", profile.address?.city)
    }

    @Test
    fun updateProfile_sendsPutRequestAndReturnsUpdatedProfile() = runTest {
        val updatedJsonResponse = """
            {
              "id": "user_1",
              "username": "demo_user",
              "email": "demo@shopflow.com",
              "fullName": "Alexander Taylor",
              "bio": "Updated bio",
              "memberSince": "2024-01-15"
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("/users/profile", request.url.encodedPath)
            assertEquals("Bearer sample-token", request.headers[HttpHeaders.Authorization])
            respond(
                content = updatedJsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorShopFlowApi.createDefaultHttpClient(mockEngine)
        val api = KtorShopFlowApi(client, baseUrl = "http://test.local")

        val updated = api.updateProfile(
            "sample-token", UpdateProfileRequest(fullName = "Alexander Taylor", bio = "Updated bio")
        )
        assertEquals("Alexander Taylor", updated.fullName)
        assertEquals("Updated bio", updated.bio)
    }

    @Test
    fun getCart_sendsBearerTokenAndReturnsCartResponse() = runTest {
        val cartJsonResponse = """
            {
              "userId": "user_1",
              "items": [
                {
                  "productId": 1,
                  "title": "Wireless Earbuds",
                  "price": 79.99,
                  "thumbnail": "https://example.com/earbuds.png",
                  "quantity": 2
                }
              ],
              "totalItems": 2,
              "totalPrice": 159.98
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("/cart", request.url.encodedPath)
            assertEquals("Bearer sample-token", request.headers[HttpHeaders.Authorization])
            respond(
                content = cartJsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorShopFlowApi.createDefaultHttpClient(mockEngine)
        val api = KtorShopFlowApi(client, baseUrl = "http://test.local")

        val cart = api.getCart(token = "sample-token")
        assertEquals("user_1", cart.userId)
        assertEquals(1, cart.items.size)
        assertEquals(2, cart.totalItems)
        assertEquals(159.98, cart.totalPrice, 0.001)
    }

    @Test
    fun addToCart_postsItemAndReturnsUpdatedCart() = runTest {
        val cartJsonResponse = """
            {
              "userId": "user_1",
              "items": [
                {
                  "productId": 5,
                  "title": "Smart Watch",
                  "price": 199.99,
                  "thumbnail": "https://example.com/watch.png",
                  "quantity": 1
                }
              ],
              "totalItems": 1,
              "totalPrice": 199.99
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("/cart/items", request.url.encodedPath)
            assertEquals("Bearer sample-token", request.headers[HttpHeaders.Authorization])
            respond(
                content = cartJsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorShopFlowApi.createDefaultHttpClient(mockEngine)
        val api = KtorShopFlowApi(client, baseUrl = "http://test.local")

        val cart = api.addToCart(AddToCartRequest(productId = 5, quantity = 1), token = "sample-token")
        assertEquals(1, cart.totalItems)
        assertEquals(5, cart.items[0].productId)
    }
}
