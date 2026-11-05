package com.shopflow.server

import com.shopflow.server.data.CatalogSeed
import com.shopflow.server.data.UserStore
import com.shopflow.server.models.AuthErrorResponse
import com.shopflow.server.models.AuthMessageResponse
import com.shopflow.server.models.ChangePasswordRequest
import com.shopflow.server.models.LoginRequest
import com.shopflow.server.models.RegisterRequest
import com.shopflow.server.models.ServerProductResponse
import com.shopflow.server.models.ServerStatus
import com.shopflow.server.models.UpdateProfileRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

private val globalUserStore = UserStore()

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(
        Netty, port = port, host = "0.0.0.0", module = Application::shopFlowModule
    ).start(wait = true)
}

fun ApplicationCall.extractAuthToken(): String? {
    val authHeader = request.headers[HttpHeaders.Authorization]
    if (authHeader != null && authHeader.startsWith("Bearer ", ignoreCase = true)) {
        return authHeader.substringAfter("Bearer ", "").trim().ifEmpty { null }
    }
    return request.headers["X-Auth-Token"]?.trim()?.ifEmpty { null }
}

fun Application.shopFlowModule(userStore: UserStore = globalUserStore) {
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
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
    }

    val products = CatalogSeed.products

    routing {
        get("/") {
            call.respond(
                ServerStatus(
                    totalProducts = products.size, totalUsers = userStore.totalUsers()
                )
            )
        }

        route("/auth") {
            post("/register") {
                val request = try {
                    call.receive<RegisterRequest>()
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("Invalid registration payload", e.message)
                    )
                    return@post
                }

                userStore.register(request).fold(onSuccess = { authResponse ->
                    call.respond(HttpStatusCode.Created, authResponse)
                }, onFailure = { error ->
                    val status = when (error) {
                        is IllegalStateException -> HttpStatusCode.Conflict
                        is IllegalArgumentException -> HttpStatusCode.BadRequest
                        else -> HttpStatusCode.BadRequest
                    }
                    call.respond(
                        status, AuthErrorResponse(error.message ?: "Registration failed")
                    )
                })
            }

            post("/login") {
                val request = try {
                    call.receive<LoginRequest>()
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("Invalid login payload", e.message)
                    )
                    return@post
                }

                userStore.login(request).fold(onSuccess = { authResponse ->
                    call.respond(HttpStatusCode.OK, authResponse)
                }, onFailure = { error ->
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthErrorResponse(error.message ?: "Authentication failed")
                    )
                })
            }

            post("/logout") {
                val token = call.extractAuthToken()
                if (token != null) {
                    userStore.logout(token)
                }
                call.respond(HttpStatusCode.OK, AuthMessageResponse("Logged out successfully"))
            }

            get("/me") {
                val token = call.extractAuthToken()
                if (token == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthErrorResponse("Authentication token required in Authorization header or X-Auth-Token")
                    )
                    return@get
                }

                val profile = userStore.getUserByToken(token)
                if (profile != null) {
                    call.respond(HttpStatusCode.OK, profile)
                } else {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthErrorResponse("Invalid or expired session token")
                    )
                }
            }
        }

        route("/users") {
            get {
                call.respond(userStore.getAllProfiles())
            }

            get("/profile") {
                val token = call.extractAuthToken()
                if (token == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthErrorResponse("Authentication token required")
                    )
                    return@get
                }

                val profile = userStore.getUserByToken(token)
                if (profile != null) {
                    call.respond(HttpStatusCode.OK, profile)
                } else {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthErrorResponse("Invalid or expired session token")
                    )
                }
            }

            put("/profile") {
                val token = call.extractAuthToken()
                if (token == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthErrorResponse("Authentication token required")
                    )
                    return@put
                }

                val userId = userStore.getUserIdByToken(token)
                if (userId == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthErrorResponse("Invalid or expired session token")
                    )
                    return@put
                }

                val request = try {
                    call.receive<UpdateProfileRequest>()
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("Invalid update profile payload", e.message)
                    )
                    return@put
                }

                userStore.updateProfile(userId, request).fold(onSuccess = { updatedProfile ->
                    call.respond(HttpStatusCode.OK, updatedProfile)
                }, onFailure = { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse(error.message ?: "Failed to update profile")
                    )
                })
            }

            post("/profile/change-password") {
                val token = call.extractAuthToken()
                if (token == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthErrorResponse("Authentication token required")
                    )
                    return@post
                }

                val userId = userStore.getUserIdByToken(token)
                if (userId == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthErrorResponse("Invalid or expired session token")
                    )
                    return@post
                }

                val request = try {
                    call.receive<ChangePasswordRequest>()
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse("Invalid password payload", e.message)
                    )
                    return@post
                }

                userStore.changePassword(userId, request).fold(onSuccess = {
                    call.respond(
                        HttpStatusCode.OK, AuthMessageResponse("Password updated successfully")
                    )
                }, onFailure = { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthErrorResponse(error.message ?: "Failed to change password")
                    )
                })
            }

            get("/{id}") {
                val id = call.parameters["id"].orEmpty()
                val profile = userStore.getUserById(id)
                if (profile != null) {
                    call.respond(HttpStatusCode.OK, profile)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound, AuthErrorResponse("User not found")
                    )
                }
            }
        }

        get("/profile") {
            val token = call.extractAuthToken()
            if (token == null) {
                call.respond(
                    HttpStatusCode.Unauthorized, AuthErrorResponse("Authentication token required")
                )
                return@get
            }

            val profile = userStore.getUserByToken(token)
            if (profile != null) {
                call.respond(HttpStatusCode.OK, profile)
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    AuthErrorResponse("Invalid or expired session token")
                )
            }
        }

        put("/profile") {
            val token = call.extractAuthToken()
            if (token == null) {
                call.respond(
                    HttpStatusCode.Unauthorized, AuthErrorResponse("Authentication token required")
                )
                return@put
            }

            val userId = userStore.getUserIdByToken(token)
            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    AuthErrorResponse("Invalid or expired session token")
                )
                return@put
            }

            val request = try {
                call.receive<UpdateProfileRequest>()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    AuthErrorResponse("Invalid update profile payload", e.message)
                )
                return@put
            }

            userStore.updateProfile(userId, request).fold(onSuccess = { updatedProfile ->
                call.respond(HttpStatusCode.OK, updatedProfile)
            }, onFailure = { error ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    AuthErrorResponse(error.message ?: "Failed to update profile")
                )
            })
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
                    p.title.contains(query, ignoreCase = true) || p.category.contains(
                        query, ignoreCase = true
                    ) || p.brand?.contains(
                        query, ignoreCase = true
                    ) == true || p.description.contains(query, ignoreCase = true)
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
                    products = filtered, total = filtered.size, skip = 0, limit = filtered.size
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
