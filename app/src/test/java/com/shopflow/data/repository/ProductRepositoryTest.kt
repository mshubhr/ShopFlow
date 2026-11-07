package com.shopflow.data.repository

import com.shopflow.data.Product
import com.shopflow.data.fakes.FakeCartDao
import com.shopflow.data.fakes.FakeDummyJsonApi
import com.shopflow.data.fakes.FakeProductDao
import com.shopflow.data.remote.ProductDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductRepositoryTest {

    private lateinit var fakeApi: FakeDummyJsonApi
    private lateinit var fakeProductDao: FakeProductDao
    private lateinit var fakeCartDao: FakeCartDao
    private lateinit var repository: ProductRepository

    @Before
    fun setup() {
        fakeApi = FakeDummyJsonApi()
        fakeProductDao = FakeProductDao()
        fakeCartDao = FakeCartDao()
        repository = ProductRepository(fakeApi, fakeProductDao, fakeCartDao) { emptyList() }
    }

    @Test
    fun refreshProducts_populatesProductDao() = runTest {
        val sampleDto = ProductDto(
            id = 1,
            title = "Test Phone",
            description = "A test phone",
            price = 299.99,
            rating = 4.5,
            category = "electronics",
            brand = "BrandX",
            stock = 10,
            thumbnail = "thumb.png"
        )
        fakeApi.productsToReturn = listOf(sampleDto)

        val pageResult = repository.refreshProducts()

        assertFalse(pageResult.hasMore)
        val products = repository.products.first()
        assertEquals(1, products.size)
        assertEquals("Test Phone", products[0].title)
    }

    @Test
    fun addToCart_respectsStockLimit() = runTest {
        val product = Product(
            id = 1,
            title = "Test Phone",
            description = "A test phone",
            price = 299.99,
            rating = 4.5,
            category = "electronics",
            brand = "BrandX",
            stock = 1,
            thumbnail = "thumb.png"
        )

        val firstAdd = repository.addToCart(product)
        assertTrue(firstAdd is CartMutationResult.Updated)

        val secondAdd = repository.addToCart(product)
        assertTrue(secondAdd is CartMutationResult.StockLimitReached)
        assertEquals(1, (secondAdd as CartMutationResult.StockLimitReached).stock)
    }

    @Test
    fun changeQuantity_removesItemWhenQuantityReachesZero() = runTest {
        val product = Product(
            id = 1,
            title = "Test Phone",
            description = "A test phone",
            price = 299.99,
            rating = 4.5,
            category = "electronics",
            brand = "BrandX",
            stock = 5,
            thumbnail = "thumb.png"
        )

        repository.addToCart(product)
        val itemsAfterAdd = repository.cartItems.first()
        assertEquals(1, itemsAfterAdd.size)

        repository.changeQuantity(productId = 1, delta = -1)
        val itemsAfterDecrement = repository.cartItems.first()
        assertTrue(itemsAfterDecrement.isEmpty())
    }

    @Test
    fun searchProducts_cachesRemoteResultsInRoom() = runTest {
        val remoteProduct = ProductDto(
            id = 55,
            title = "Galaxy Phone",
            description = "High end",
            price = 799.99,
            rating = 4.7,
            category = "smartphones",
            brand = "Samsung",
            stock = 8,
            thumbnail = "samsung.png"
        )
        fakeApi.productsToReturn = listOf(remoteProduct)

        val results = repository.searchProducts("Galaxy")
        assertEquals(1, results.size)
        assertEquals("Galaxy Phone", results[0].title)

        val fromDb = fakeProductDao.findById(55)
        org.junit.Assert.assertNotNull(fromDb)
        assertEquals("Galaxy Phone", fromDb?.title)
    }

    @Test
    fun carts_arePersistedPerUser() = runTest {
        val phone = Product(
            id = 1,
            title = "Phone",
            description = "Desc",
            price = 500.0,
            rating = 4.5,
            category = "electronics",
            brand = "BrandX",
            stock = 10,
            thumbnail = "phone.png"
        )
        val laptop = Product(
            id = 2,
            title = "Laptop",
            description = "Desc",
            price = 1000.0,
            rating = 4.8,
            category = "laptops",
            brand = "BrandY",
            stock = 5,
            thumbnail = "laptop.png"
        )

        repository.setCurrentUser("guest")
        repository.addToCart(phone)

        val guestItems = repository.cartItems.first()
        assertEquals(1, guestItems.size)
        assertEquals(1, guestItems[0].productId)

        repository.setCurrentUser("user_1")
        val user1InitialItems = repository.cartItems.first()
        assertTrue(user1InitialItems.isEmpty())

        repository.addToCart(laptop)
        val user1Items = repository.cartItems.first()
        assertEquals(1, user1Items.size)
        assertEquals(2, user1Items[0].productId)

        repository.setCurrentUser("guest")
        val guestItemsAgain = repository.cartItems.first()
        assertEquals(1, guestItemsAgain.size)
        assertEquals(1, guestItemsAgain[0].productId)
    }

    @Test
    fun transferCart_mergesItemsFromGuestToUser() = runTest {
        val phone = Product(
            id = 1,
            title = "Phone",
            description = "Desc",
            price = 500.0,
            rating = 4.5,
            category = "electronics",
            brand = "BrandX",
            stock = 10,
            thumbnail = "phone.png"
        )
        fakeProductDao.insertAll(
            listOf(
                com.shopflow.data.local.ProductEntity(
                    id = 1,
                    title = "Phone",
                    description = "Desc",
                    price = 500.0,
                    rating = 4.5,
                    category = "electronics",
                    brand = "BrandX",
                    stock = 10,
                    thumbnail = "phone.png"
                )
            )
        )

        repository.addToCart(phone, userId = "guest")
        assertEquals(1, repository.observeCartForUser("guest").first().size)
        assertTrue(repository.observeCartForUser("user_1").first().isEmpty())

        repository.transferCart(fromUserId = "guest", toUserId = "user_1")

        assertTrue(repository.observeCartForUser("guest").first().isEmpty())
        val user1Items = repository.observeCartForUser("user_1").first()
        assertEquals(1, user1Items.size)
        assertEquals(1, user1Items[0].productId)
    }

    @Test
    fun clearCart_onlyClearsSpecifiedUser() = runTest {
        val phone = Product(
            id = 1,
            title = "Phone",
            description = "Desc",
            price = 500.0,
            rating = 4.5,
            category = "electronics",
            brand = "BrandX",
            stock = 10,
            thumbnail = "phone.png"
        )

        repository.addToCart(phone, userId = "user_1")
        repository.addToCart(phone, userId = "user_2")

        repository.clearCart(userId = "user_1")

        assertTrue(repository.observeCartForUser("user_1").first().isEmpty())
        assertEquals(1, repository.observeCartForUser("user_2").first().size)
    }
}
