package com.shopflow.server

import com.shopflow.server.data.CatalogSeed
import com.shopflow.server.models.ServerProductResponse
import com.shopflow.server.models.ServerStatus
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::shopFlowModule)
        .start(wait = true)
}

fun Application.shopFlowModule() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CORS) {
        anyHost()
        allowHeader("*")
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
    }

    val products = CatalogSeed.products

    routing {
        get("/") {
            call.respond(
                ServerStatus(
                    totalProducts = products.size
                )
            )
        }

        get("/products") {
            val limitParam = call.request.queryParameters["limit"]?.toIntOrNull() ?: 24
            val skipParam = call.request.queryParameters["skip"]?.toIntOrNull() ?: 0

            val pagedProducts = if (limitParam <= 0) {
                products.drop(skipParam)
            } else {
                products.drop(skipParam).take(limitParam)
            }

            call.respond(
                ServerProductResponse(
                    products = pagedProducts,
                    total = products.size,
                    skip = skipParam,
                    limit = if (limitParam <= 0) pagedProducts.size else limitParam
                )
            )
        }

        get("/products/search") {
            val query = call.request.queryParameters["q"]?.trim().orEmpty()
            val limitParam = call.request.queryParameters["limit"]?.toIntOrNull() ?: 0
            val skipParam = call.request.queryParameters["skip"]?.toIntOrNull() ?: 0

            val filtered = if (query.isBlank()) {
                products
            } else {
                products.filter { p ->
                    p.title.contains(query, ignoreCase = true) ||
                    p.category.contains(query, ignoreCase = true) ||
                    p.brand?.contains(query, ignoreCase = true) == true ||
                    p.description.contains(query, ignoreCase = true)
                }
            }

            val paged = if (limitParam > 0) {
                filtered.drop(skipParam).take(limitParam)
            } else {
                filtered.drop(skipParam)
            }

            call.respond(
                ServerProductResponse(
                    products = paged,
                    total = filtered.size,
                    skip = skipParam,
                    limit = if (limitParam <= 0) paged.size else limitParam
                )
            )
        }

        get("/products/categories") {
            val categories = products.map { it.category }.distinct().sorted()
            call.respond(categories)
        }

        get("/products/category/{category}") {
            val category = call.parameters["category"].orEmpty()
            val filtered = products.filter { it.category.equals(category, ignoreCase = true) }
            call.respond(
                ServerProductResponse(
                    products = filtered,
                    total = filtered.size,
                    skip = 0,
                    limit = filtered.size
                )
            )
        }

        get("/products/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val product = products.find { it.id == id }
            if (product != null) {
                call.respond(product)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Product not found"))
            }
        }
    }
}
