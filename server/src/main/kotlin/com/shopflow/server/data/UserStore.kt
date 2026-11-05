package com.shopflow.server.data

import com.shopflow.server.models.AddressDto
import com.shopflow.server.models.AuthResponse
import com.shopflow.server.models.ChangePasswordRequest
import com.shopflow.server.models.LoginRequest
import com.shopflow.server.models.RegisterRequest
import com.shopflow.server.models.UpdateProfileRequest
import com.shopflow.server.models.UserProfileDto
import com.shopflow.server.security.SecurityUtils
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class StoredUser(
    val id: String,
    val username: String,
    val email: String,
    var passwordHash: String,
    var salt: String,
    var fullName: String,
    var avatarUrl: String? = null,
    var bio: String? = null,
    var phone: String? = null,
    var address: AddressDto? = null,
    val memberSince: String
) {
    fun toProfileDto(): UserProfileDto = UserProfileDto(
        id = id,
        username = username,
        email = email,
        fullName = fullName,
        avatarUrl = avatarUrl,
        bio = bio,
        phone = phone,
        address = address,
        memberSince = memberSince
    )
}

class UserStore {
    private val usersById = ConcurrentHashMap<String, StoredUser>()
    private val userIdByUsername = ConcurrentHashMap<String, String>()
    private val userIdByEmail = ConcurrentHashMap<String, String>()
    private val activeTokens = ConcurrentHashMap<String, String>()

    init {
        seedInitialUsers()
    }

    private fun seedInitialUsers() {
        val salt1 = SecurityUtils.generateSalt()
        val user1 = StoredUser(
            id = "user_1",
            username = "demo_user",
            email = "demo@shopflow.com",
            passwordHash = SecurityUtils.hashPassword("Password123!", salt1),
            salt = salt1,
            fullName = "Alex Taylor",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80",
            bio = "Tech enthusiast, audiophile, and avid online shopper.",
            phone = "+1 (555) 234-5678",
            address = AddressDto(
                street = "742 Evergreen Terrace",
                city = "Springfield",
                state = "OR",
                postalCode = "97477",
                country = "USA"
            ),
            memberSince = "2024-01-15"
        )
        saveUser(user1)

        val salt2 = SecurityUtils.generateSalt()
        val user2 = StoredUser(
            id = "user_2",
            username = "jane_doe",
            email = "jane@shopflow.com",
            passwordHash = SecurityUtils.hashPassword("Password123!", salt2),
            salt = salt2,
            fullName = "Jane Doe",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80",
            bio = "Designer, coffee lover, and style explorer.",
            phone = "+1 (555) 876-5432",
            address = AddressDto(
                street = "100 Innovation Way",
                city = "Austin",
                state = "TX",
                postalCode = "78701",
                country = "USA"
            ),
            memberSince = "2024-06-20"
        )
        saveUser(user2)
    }

    @Synchronized
    private fun saveUser(user: StoredUser) {
        usersById[user.id] = user
        userIdByUsername[user.username.lowercase()] = user.id
        userIdByEmail[user.email.lowercase()] = user.id
    }

    fun totalUsers(): Int = usersById.size

    @Synchronized
    fun register(request: RegisterRequest): Result<AuthResponse> {
        val trimmedUsername = request.username.trim()
        val trimmedEmail = request.email.trim()

        if (!SecurityUtils.isValidUsername(trimmedUsername)) {
            return Result.failure(IllegalArgumentException("Username must be 3-30 alphanumeric characters or underscores"))
        }

        if (!SecurityUtils.isValidEmail(trimmedEmail)) {
            return Result.failure(IllegalArgumentException("Invalid email address format"))
        }

        if (request.password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters long"))
        }

        if (userIdByUsername.containsKey(trimmedUsername.lowercase())) {
            return Result.failure(IllegalStateException("Username '$trimmedUsername' is already taken"))
        }

        if (userIdByEmail.containsKey(trimmedEmail.lowercase())) {
            return Result.failure(IllegalStateException("Email '$trimmedEmail' is already registered"))
        }

        val salt = SecurityUtils.generateSalt()
        val passwordHash = SecurityUtils.hashPassword(request.password, salt)
        val id = "user_" + UUID.randomUUID().toString().take(8)
        val today = LocalDate.now().toString()

        val newUser = StoredUser(
            id = id,
            username = trimmedUsername,
            email = trimmedEmail,
            passwordHash = passwordHash,
            salt = salt,
            fullName = request.fullName?.trim()?.ifEmpty { trimmedUsername } ?: trimmedUsername,
            avatarUrl = null,
            bio = null,
            phone = null,
            address = null,
            memberSince = today)
        saveUser(newUser)

        val token = SecurityUtils.generateToken()
        activeTokens[token] = id

        return Result.success(
            AuthResponse(
                token = token, user = newUser.toProfileDto()
            )
        )
    }

    fun login(request: LoginRequest): Result<AuthResponse> {
        val identifier = request.usernameOrEmail.trim().lowercase()
        val userId =
            userIdByUsername[identifier] ?: userIdByEmail[identifier] ?: return Result.failure(
                IllegalArgumentException("Invalid username/email or password")
            )

        val user = usersById[userId]
            ?: return Result.failure(IllegalArgumentException("Invalid username/email or password"))

        val isPasswordCorrect =
            SecurityUtils.verifyPassword(request.password, user.salt, user.passwordHash)
        if (!isPasswordCorrect) {
            return Result.failure(IllegalArgumentException("Invalid username/email or password"))
        }

        val token = SecurityUtils.generateToken()
        activeTokens[token] = user.id

        return Result.success(
            AuthResponse(
                token = token, user = user.toProfileDto()
            )
        )
    }

    fun logout(token: String): Boolean {
        return activeTokens.remove(token) != null
    }

    fun getUserByToken(token: String): UserProfileDto? {
        val userId = activeTokens[token] ?: return null
        return usersById[userId]?.toProfileDto()
    }

    fun getUserIdByToken(token: String): String? {
        return activeTokens[token]
    }

    fun getUserById(id: String): UserProfileDto? {
        return usersById[id]?.toProfileDto()
    }

    fun getAllProfiles(): List<UserProfileDto> {
        return usersById.values.map { it.toProfileDto() }.sortedBy { it.id }
    }

    @Synchronized
    fun updateProfile(userId: String, request: UpdateProfileRequest): Result<UserProfileDto> {
        val user =
            usersById[userId] ?: return Result.failure(NoSuchElementException("User not found"))

        request.fullName?.let { user.fullName = it.trim() }
        request.bio?.let { user.bio = it.trim() }
        request.phone?.let { user.phone = it.trim() }
        request.avatarUrl?.let { user.avatarUrl = it.trim() }
        request.address?.let { user.address = it }

        return Result.success(user.toProfileDto())
    }

    @Synchronized
    fun changePassword(userId: String, request: ChangePasswordRequest): Result<Unit> {
        val user =
            usersById[userId] ?: return Result.failure(NoSuchElementException("User not found"))

        val isCurrentValid =
            SecurityUtils.verifyPassword(request.currentPassword, user.salt, user.passwordHash)
        if (!isCurrentValid) {
            return Result.failure(IllegalArgumentException("Current password is incorrect"))
        }

        if (request.newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters long"))
        }

        val newSalt = SecurityUtils.generateSalt()
        user.salt = newSalt
        user.passwordHash = SecurityUtils.hashPassword(request.newPassword, newSalt)

        return Result.success(Unit)
    }

    @Synchronized
    fun resetToDefaults() {
        usersById.clear()
        userIdByUsername.clear()
        userIdByEmail.clear()
        activeTokens.clear()
        seedInitialUsers()
    }
}
