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
        repository = ProductRepository(fakeApi, fakeProductDao, fakeCartDao)
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
}
