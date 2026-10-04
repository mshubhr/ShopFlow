package com.shopflow.viewmodel

import app.cash.turbine.test
import com.shopflow.data.Product
import com.shopflow.data.fakes.FakeCartDao
import com.shopflow.data.fakes.FakeDummyJsonApi
import com.shopflow.data.fakes.FakeProductDao
import com.shopflow.data.remote.ProductDto
import com.shopflow.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeApi: FakeDummyJsonApi
    private lateinit var fakeProductDao: FakeProductDao
    private lateinit var fakeCartDao: FakeCartDao
    private lateinit var repository: ProductRepository
    private lateinit var viewModel: ShopViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeDummyJsonApi()
        fakeProductDao = FakeProductDao()
        fakeCartDao = FakeCartDao()
        repository = ProductRepository(fakeApi, fakeProductDao, fakeCartDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun catalogUiState_filtersProductsByQuery() = runTest {
        val phoneDto = ProductDto(
            id = 1,
            title = "Smart Phone",
            description = "",
            price = 500.0,
            rating = 4.5,
            category = "electronics",
            brand = "Apple",
            stock = 5,
            thumbnail = ""
        )
        val shirtDto = ProductDto(
            id = 2,
            title = "Cotton Shirt",
            description = "",
            price = 20.0,
            rating = 4.0,
            category = "apparel",
            brand = "Nike",
            stock = 10,
            thumbnail = ""
        )
        fakeApi.productsToReturn = listOf(phoneDto, shirtDto)

        viewModel = ShopViewModel(repository, searchDebounceMillis = 0L)

        viewModel.catalogUiState.test {
            val stateWithProducts = expectMostRecentItem()
            assertEquals(2, stateWithProducts.products.size)

            viewModel.setQuery("Phone")
            testScheduler.advanceUntilIdle()

            val filteredState = expectMostRecentItem()
            assertEquals(1, filteredState.products.size)
            assertEquals("Smart Phone", filteredState.products[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun remoteSearch_findsUnpaginatedProductsAndCachesInRoom() = runTest {
        val page1Product = ProductDto(
            id = 1,
            title = "First Page Item",
            description = "desc",
            price = 10.0,
            rating = 4.0,
            category = "cat",
            brand = "brand",
            stock = 5,
            thumbnail = ""
        )
        val unpaginatedProduct = ProductDto(
            id = 99,
            title = "Special Remote Phone",
            description = "desc",
            price = 999.0,
            rating = 4.9,
            category = "smartphones",
            brand = "BrandZ",
            stock = 12,
            thumbnail = ""
        )
        fakeApi.productsToReturn = listOf(page1Product, unpaginatedProduct)

        viewModel = ShopViewModel(repository, searchDebounceMillis = 0L)

        viewModel.catalogUiState.test {
            expectMostRecentItem()

            // Product 99 has not been downloaded in the paginated catalog
            viewModel.setQuery("Remote Phone")
            testScheduler.advanceUntilIdle()

            val searchState = expectMostRecentItem()
            assertEquals(1, searchState.products.size)
            assertEquals("Special Remote Phone", searchState.products[0].title)

            // Verify it was saved to Room cache
            val cachedInRoom = fakeProductDao.findById(99)
            org.junit.Assert.assertNotNull(cachedInRoom)
            assertEquals("Special Remote Phone", cachedInRoom?.title)

            // Clear search restores catalog
            viewModel.setQuery("")
            testScheduler.advanceUntilIdle()

            val restoredCatalogState = expectMostRecentItem()
            assertEquals("", restoredCatalogState.query)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addToCart_updatesCartUiState() = runTest {
        val product = Product(
            id = 1,
            title = "Laptop",
            description = "",
            price = 1000.0,
            rating = 4.8,
            category = "tech",
            brand = "Dell",
            stock = 5,
            thumbnail = ""
        )

        viewModel = ShopViewModel(repository)

        viewModel.cartUiState.test {
            expectMostRecentItem()

            viewModel.addToCart(product)

            val cartState = expectMostRecentItem()
            assertEquals(1, cartState.itemCount)
            assertEquals(1000.0, cartState.total, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
