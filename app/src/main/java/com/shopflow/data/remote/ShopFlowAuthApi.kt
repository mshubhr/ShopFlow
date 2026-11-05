package com.shopflow.data.remote

interface ShopFlowAuthApi {
    suspend fun login(request: LoginRequest): AuthResponse
    suspend fun register(request: RegisterRequest): AuthResponse
    suspend fun logout(token: String): AuthMessageResponse
    suspend fun getProfile(token: String): UserProfileDto
    suspend fun updateProfile(token: String, request: UpdateProfileRequest): UserProfileDto
    suspend fun changePassword(token: String, request: ChangePasswordRequest): AuthMessageResponse
}
