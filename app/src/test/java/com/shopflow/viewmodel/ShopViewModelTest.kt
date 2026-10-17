package com.shopflow.viewmodel

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.shopflow.data.Product
import com.shopflow.data.fakes.FakeCartDao
import com.shopflow.data.fakes.FakeDummyJsonApi
import com.shopflow.data.fakes.FakeProductDao
import com.shopflow.data.remote.OfflineCatalogLoader
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
        repository = ProductRepository(fakeApi, fakeProductDao, fakeCartDao) {
            OfflineCatalogLoader(ApplicationProvider.getApplicationContext()).load()
        }
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

            viewModel.setQuery("Remote Phone")
            testScheduler.advanceUntilIdle()

            val searchState = expectMostRecentItem()
            assertEquals(1, searchState.products.size)
            assertEquals("Special Remote Phone", searchState.products[0].title)

            val cachedInRoom = fakeProductDao.findById(99)
            org.junit.Assert.assertNotNull(cachedInRoom)
            assertEquals("Special Remote Phone", cachedInRoom?.title)

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

    @Test
    fun categoryFilter_filtersCatalogProducts() = runTest {
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
            expectMostRecentItem()

            viewModel.setCategory("electronics")
            val filteredState = expectMostRecentItem()
            assertEquals(1, filteredState.products.size)
            assertEquals("Smart Phone", filteredState.products[0].title)
            assertEquals("electronics", filteredState.selectedCategory)

            viewModel.setCategory("All")
            val allState = expectMostRecentItem()
            assertEquals(2, allState.products.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sortOption_sortsCatalogProductsCorrectly() = runTest {
        val p1 = ProductDto(
            id = 1,
            title = "B Phone",
            description = "",
            price = 500.0,
            rating = 4.2,
            category = "electronics",
            brand = "Brand",
            stock = 5,
            thumbnail = ""
        )
        val p2 = ProductDto(
            id = 2,
            title = "A Laptop",
            description = "",
            price = 1000.0,
            rating = 4.8,
            category = "electronics",
            brand = "Brand",
            stock = 10,
            thumbnail = ""
        )
        val p3 = ProductDto(
            id = 3,
            title = "C Watch",
            description = "",
            price = 150.0,
            rating = 3.9,
            category = "electronics",
            brand = "Brand",
            stock = 2,
            thumbnail = ""
        )
        fakeApi.productsToReturn = listOf(p1, p2, p3)

        viewModel = ShopViewModel(repository, searchDebounceMillis = 0L)

        viewModel.catalogUiState.test {
            expectMostRecentItem()

            viewModel.setSortOption(SortOption.PRICE_LOW_HIGH)
            val lowToHigh = expectMostRecentItem()
            assertEquals(listOf(150.0, 500.0, 1000.0), lowToHigh.products.map { it.price })

            viewModel.setSortOption(SortOption.PRICE_HIGH_LOW)
            val highToLow = expectMostRecentItem()
            assertEquals(listOf(1000.0, 500.0, 150.0), highToLow.products.map { it.price })

            viewModel.setSortOption(SortOption.RATING_HIGH_LOW)
            val topRated = expectMostRecentItem()
            assertEquals(listOf(4.8, 4.2, 3.9), topRated.products.map { it.rating })

            viewModel.setSortOption(SortOption.NAME_A_Z)
            val nameSort = expectMostRecentItem()
            assertEquals(
                listOf("A Laptop", "B Phone", "C Watch"), nameSort.products.map { it.title })

            cancelAndIgnoreRemainingEvents()
        }
    }
}
