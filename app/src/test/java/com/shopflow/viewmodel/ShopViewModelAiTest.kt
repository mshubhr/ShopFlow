package com.shopflow.viewmodel

import com.shopflow.ai.model.AiSender
import com.shopflow.data.fakes.FakeCartDao
import com.shopflow.data.fakes.FakeDummyJsonApi
import com.shopflow.data.fakes.FakeProductDao
import com.shopflow.data.local.ProductEntity
import com.shopflow.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelAiTest {

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
        repository = ProductRepository(fakeApi, fakeProductDao, fakeCartDao) { emptyList() }
        viewModel = ShopViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aiUiState_initiallyContainsWelcomeMessage() = runTest {
        val state = viewModel.aiUiState.value
        assertEquals(1, state.messages.size)
        assertEquals(AiSender.ASSISTANT, state.messages.first().sender)
        assertTrue(state.messages.first().text.contains("ShopFlow AI"))
        assertFalse(state.isGenerating)
    }

    @Test
    fun sendAiMessage_addsUserAndAssistantMessages() = runTest {
        fakeProductDao.insertAll(
            listOf(
                ProductEntity(
                    id = 1,
                    title = "Gaming Laptop",
                    description = "Powerful laptop for gaming.",
                    price = 1200.0,
                    rating = 4.7,
                    category = "laptops",
                    brand = "GamerBrand",
                    stock = 5,
                    thumbnail = ""
                )
            )
        )

        viewModel.sendAiMessage("Show me laptops")

        val state = viewModel.aiUiState.first()
        assertEquals(3, state.messages.size)
        assertEquals(AiSender.USER, state.messages[1].sender)
        assertEquals("Show me laptops", state.messages[1].text)
        assertEquals(AiSender.ASSISTANT, state.messages[2].sender)
        assertFalse(state.isGenerating)
    }

    @Test
    fun clearAiConversation_resetsMessages() = runTest {
        viewModel.sendAiMessage("Hello")
        assertEquals(3, viewModel.aiUiState.value.messages.size)

        viewModel.clearAiConversation()
        assertEquals(1, viewModel.aiUiState.value.messages.size)
        assertEquals(AiSender.ASSISTANT, viewModel.aiUiState.value.messages.first().sender)
    }

    @Test
    fun getOrGenerateProductInsight_generatesAndCachesInsight() = runTest {
        val product = com.shopflow.data.Product(
            id = 10,
            title = "Noise Cancelling Earbuds",
            description = "Crystal clear audio.",
            price = 79.99,
            rating = 4.6,
            category = "mobile-accessories",
            brand = "SoundPro",
            stock = 8,
            thumbnail = ""
        )

        val insight1 = viewModel.getOrGenerateProductInsight(product)
        assertNotNull(insight1)
        assertEquals(10, insight1.productId)

        val cachedInsight = viewModel.aiUiState.value.productInsights[10]
        assertEquals(insight1, cachedInsight)
    }
}
