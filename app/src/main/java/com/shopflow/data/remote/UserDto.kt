package com.shopflow.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class AddressDto(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = ""
)

@Serializable
data class UserProfileDto(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val phone: String? = null,
    val address: AddressDto? = null,
    val memberSince: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String? = null
)

@Serializable
data class LoginRequest(
    val usernameOrEmail: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserProfileDto,
    val expiresInSeconds: Long = 2592000L
)

@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val bio: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val address: AddressDto? = null
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class AuthMessageResponse(
    val message: String
)

@Serializable
data class AuthErrorResponse(
    val error: String,
    val details: String? = null
)

@Serializable
data class ServerCartItemDto(
    val productId: Int,
    val title: String,
    val price: Double,
    val thumbnail: String,
    val quantity: Int
)

@Serializable
data class ServerCartResponse(
    val userId: String,
    val items: List<ServerCartItemDto> = emptyList(),
    val totalItems: Int = 0,
    val totalPrice: Double = 0.0
)

@Serializable
data class AddToCartRequest(
    val productId: Int,
    val quantity: Int = 1
)

@Serializable
data class UpdateCartItemRequest(
    val quantity: Int
)
