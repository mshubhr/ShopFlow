package com.shopflow.server

import com.shopflow.server.data.UserStore
import com.shopflow.server.models.AddressDto
import com.shopflow.server.models.ChangePasswordRequest
import com.shopflow.server.models.LoginRequest
import com.shopflow.server.models.RegisterRequest
import com.shopflow.server.models.UpdateProfileRequest
import com.shopflow.server.security.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserStoreTest {

    private lateinit var userStore: UserStore

    @Before
    fun setUp() {
        userStore = UserStore()
    }

    @Test
    fun initialSeed_containsDefaultUsers() {
        assertEquals(2, userStore.totalUsers())

        val demoProfile = userStore.getUserById("user_1")
        assertNotNull(demoProfile)
        assertEquals("demo_user", demoProfile?.username)
        assertEquals("demo@shopflow.com", demoProfile?.email)
        assertEquals("Alex Taylor", demoProfile?.fullName)
        assertNotNull(demoProfile?.address)
        assertEquals("Springfield", demoProfile?.address?.city)

        val janeProfile = userStore.getUserById("user_2")
        assertNotNull(janeProfile)
        assertEquals("jane_doe", janeProfile?.username)
    }

    @Test
    fun login_successWithUsername() {
        val result = userStore.login(
            LoginRequest(usernameOrEmail = "demo_user", password = "Password123!")
        )
        assertTrue(result.isSuccess)
        val auth = result.getOrNull()
        assertNotNull(auth)
        assertTrue(auth!!.token.isNotBlank())
        assertEquals("demo_user", auth.user.username)
        assertEquals("Alex Taylor", auth.user.fullName)

        val profile = userStore.getUserByToken(auth.token)
        assertNotNull(profile)
        assertEquals("user_1", profile?.id)
    }

    @Test
    fun login_successWithEmail() {
        val result = userStore.login(
            LoginRequest(usernameOrEmail = "demo@shopflow.com", password = "Password123!")
        )
        assertTrue(result.isSuccess)
        assertEquals("demo_user", result.getOrNull()?.user?.username)
    }

    @Test
    fun login_caseInsensitiveIdentifier() {
        val result = userStore.login(
            LoginRequest(usernameOrEmail = "DEMO_USER", password = "Password123!")
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun login_failureWrongPassword() {
        val result = userStore.login(
            LoginRequest(usernameOrEmail = "demo_user", password = "WrongPassword")
        )
        assertTrue(result.isFailure)
        assertEquals("Invalid username/email or password", result.exceptionOrNull()?.message)
    }

    @Test
    fun login_failureUserNotFound() {
        val result = userStore.login(
            LoginRequest(usernameOrEmail = "non_existent", password = "Password123!")
        )
        assertTrue(result.isFailure)
        assertEquals("Invalid username/email or password", result.exceptionOrNull()?.message)
    }

    @Test
    fun register_success() {
        val result = userStore.register(
            RegisterRequest(
                username = "new_user",
                email = "new@shopflow.com",
                password = "SecretPassword123",
                fullName = "New User"
            )
        )
        assertTrue(result.isSuccess)
        val auth = result.getOrNull()
        assertNotNull(auth)
        assertEquals("new_user", auth!!.user.username)
        assertEquals("new@shopflow.com", auth.user.email)
        assertEquals("New User", auth.user.fullName)
        assertEquals(3, userStore.totalUsers())

        val loginResult = userStore.login(
            LoginRequest(usernameOrEmail = "new_user", password = "SecretPassword123")
        )
        assertTrue(loginResult.isSuccess)
    }

    @Test
    fun register_duplicateUsernameFails() {
        val result = userStore.register(
            RegisterRequest(
                username = "demo_user", email = "another@shopflow.com", password = "Password123!"
            )
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun register_duplicateEmailFails() {
        val result = userStore.register(
            RegisterRequest(
                username = "unique_name", email = "demo@shopflow.com", password = "Password123!"
            )
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun register_validationFailures() {
        val r1 = userStore.register(
            RegisterRequest(username = "ab", email = "test@shopflow.com", password = "Password123!")
        )
        assertTrue(r1.isFailure)

        val r2 = userStore.register(
            RegisterRequest(
                username = "valid_user", email = "not-an-email", password = "Password123!"
            )
        )
        assertTrue(r2.isFailure)

        val r3 = userStore.register(
            RegisterRequest(username = "valid_user", email = "valid@shopflow.com", password = "123")
        )
        assertTrue(r3.isFailure)
    }

    @Test
    fun logout_invalidatesToken() {
        val auth = userStore.login(
            LoginRequest(usernameOrEmail = "demo_user", password = "Password123!")
        ).getOrThrow()

        assertNotNull(userStore.getUserByToken(auth.token))

        val loggedOut = userStore.logout(auth.token)
        assertTrue(loggedOut)
        assertNull(userStore.getUserByToken(auth.token))

        assertFalse(userStore.logout(auth.token))
    }

    @Test
    fun updateProfile_updatesFieldsCorrectly() {
        val updateReq = UpdateProfileRequest(
            fullName = "Alexander Taylor",
            bio = "Updated bio for Alex",
            phone = "+1 (555) 999-0000",
            address = AddressDto(
                street = "123 Main St",
                city = "Portland",
                state = "OR",
                postalCode = "97201",
                country = "USA"
            )
        )

        val result = userStore.updateProfile("user_1", updateReq)
        assertTrue(result.isSuccess)

        val updated = result.getOrNull()
        assertNotNull(updated)
        assertEquals("Alexander Taylor", updated?.fullName)
        assertEquals("Updated bio for Alex", updated?.bio)
        assertEquals("+1 (555) 999-0000", updated?.phone)
        assertEquals("Portland", updated?.address?.city)
    }

    @Test
    fun changePassword_succeedsAndVerifiesNewPassword() {
        val changeReq = ChangePasswordRequest(
            currentPassword = "Password123!", newPassword = "NewSecurePassword456"
        )

        val result = userStore.changePassword("user_1", changeReq)
        assertTrue(result.isSuccess)

        val oldLogin = userStore.login(
            LoginRequest(usernameOrEmail = "demo_user", password = "Password123!")
        )
        assertTrue(oldLogin.isFailure)

        val newLogin = userStore.login(
            LoginRequest(usernameOrEmail = "demo_user", password = "NewSecurePassword456")
        )
        assertTrue(newLogin.isSuccess)
    }

    @Test
    fun changePassword_failsWithIncorrectCurrentPassword() {
        val changeReq = ChangePasswordRequest(
            currentPassword = "WrongOldPassword", newPassword = "NewSecurePassword456"
        )
        val result = userStore.changePassword("user_1", changeReq)
        assertTrue(result.isFailure)
    }

    @Test
    fun securityUtils_verifyPasswordAndToken() {
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword("MyTestPassword", salt)

        assertTrue(SecurityUtils.verifyPassword("MyTestPassword", salt, hash))
        assertFalse(SecurityUtils.verifyPassword("WrongTestPassword", salt, hash))

        val token1 = SecurityUtils.generateToken()
        val token2 = SecurityUtils.generateToken()
        assertTrue(token1.isNotBlank())
        assertTrue(token2.isNotBlank())
        assertTrue(token1 != token2)

        assertTrue(SecurityUtils.isValidEmail("user@example.com"))
        assertFalse(SecurityUtils.isValidEmail("invalid-email"))

        assertTrue(SecurityUtils.isValidUsername("valid_user123"))
        assertFalse(SecurityUtils.isValidUsername("a"))
        assertFalse(SecurityUtils.isValidUsername("user with spaces"))
    }
}
