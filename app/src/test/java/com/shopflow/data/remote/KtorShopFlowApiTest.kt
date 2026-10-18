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
}
