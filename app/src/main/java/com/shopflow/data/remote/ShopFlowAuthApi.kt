package com.shopflow.data.remote

interface ShopFlowAuthApi {
    suspend fun login(request: LoginRequest): AuthResponse
    suspend fun register(request: RegisterRequest): AuthResponse
    suspend fun logout(token: String): AuthMessageResponse
    suspend fun getProfile(token: String): UserProfileDto
    suspend fun updateProfile(token: String, request: UpdateProfileRequest): UserProfileDto
    suspend fun changePassword(token: String, request: ChangePasswordRequest): AuthMessageResponse

    suspend fun getCart(token: String? = null, userId: String? = null): ServerCartResponse
    suspend fun addToCart(request: AddToCartRequest, token: String? = null, userId: String? = null): ServerCartResponse
    suspend fun updateCartItem(productId: Int, request: UpdateCartItemRequest, token: String? = null, userId: String? = null): ServerCartResponse
    suspend fun removeFromCart(productId: Int, token: String? = null, userId: String? = null): ServerCartResponse
    suspend fun clearCart(token: String? = null, userId: String? = null): ServerCartResponse
}
