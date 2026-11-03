package com.shopflow.ai

import com.shopflow.ai.model.AiSender
import com.shopflow.data.CartItem
import com.shopflow.data.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiShoppingAssistantTest {

    private lateinit var assistant: AiShoppingAssistant
    private lateinit var sampleCatalog: List<Product>

    @Before
    fun setup() {
        assistant = AiShoppingAssistant()
        sampleCatalog = listOf(
            Product(
                id = 1,
                title = "Wireless Headphones",
                description = "High-quality wireless headphones with active noise cancellation.",
                price = 99.99,
                rating = 4.8,
                category = "electronics",
                brand = "AudioTech",
                stock = 3,
                thumbnail = "https://example.com/headphones.png"
            ), Product(
                id = 2,
                title = "Smart Fitness Watch",
                description = "Fitness tracking smartwatch with heart rate monitor.",
                price = 49.99,
                rating = 4.6,
                category = "electronics",
                brand = "TechTime",
                stock = 15,
                thumbnail = "https://example.com/watch.png"
            ), Product(
                id = 3,
                title = "Lavender Perfume Spray",
                description = "Fragrant organic lavender perfume for all-day freshness.",
                price = 24.50,
                rating = 4.2,
                category = "fragrances",
                brand = "Essence",
                stock = 20,
                thumbnail = "https://example.com/perfume.png"
            ), Product(
                id = 4,
                title = "Luxury Leather Sofa",
                description = "Spacious 3-seater genuine leather sofa.",
                price = 899.00,
                rating = 4.1,
                category = "furniture",
                brand = "HomeComfort",
                stock = 2,
                thumbnail = "https://example.com/sofa.png"
            )
        )
    }

    @Test
    fun generateResponse_blankQuery_returnsPromptMessage() {
        val response = assistant.generateResponse("   ", sampleCatalog, emptyList())
        assertEquals(AiSender.ASSISTANT, response.sender)
        assertTrue(response.text.contains("Please ask me a question"))
    }

    @Test
    fun generateResponse_greeting_returnsHelpfulCapabilities() {
        val response = assistant.generateResponse("Hello there!", sampleCatalog, emptyList())
        assertEquals(AiSender.ASSISTANT, response.sender)
        assertTrue(response.text.contains("ShopFlow AI"))
        assertTrue(response.text.contains("catalog"))
        assertTrue(response.suggestedFollowUps.isNotEmpty())
    }

    @Test
    fun generateResponse_budgetFilter_returnsMatchingAffordableProducts() {
        val response =
            assistant.generateResponse("Find me items under $50", sampleCatalog, emptyList())
        assertEquals(AiSender.ASSISTANT, response.sender)
        assertTrue(response.suggestedProducts.isNotEmpty())
        assertTrue(response.suggestedProducts.all { it.price <= 50.0 })
        assertTrue(response.suggestedProducts.any { it.id == 2 })
        assertTrue(response.suggestedProducts.any { it.id == 3 })
        assertFalse(response.suggestedProducts.any { it.id == 1 || it.id == 4 })
    }

    @Test
    fun generateResponse_topRatedQuery_prioritizesHighestRatings() {
        val response = assistant.generateResponse(
            "What are the best top rated products?", sampleCatalog, emptyList()
        )
        assertTrue(response.suggestedProducts.isNotEmpty())
        assertEquals(1, response.suggestedProducts.first().id)
    }

    @Test
    fun generateResponse_cartQuery_emptyCart_suggestsStarterItems() {
        val response =
            assistant.generateResponse("Review my cart please", sampleCatalog, emptyList())
        assertTrue(response.text.contains("cart is currently empty"))
        assertTrue(response.suggestedProducts.isNotEmpty())
    }

    @Test
    fun generateResponse_cartQuery_withItems_summarizesTotalAndComplementaryItems() {
        val cartItems = listOf(
            CartItem(
                productId = 2,
                title = "Smart Fitness Watch",
                price = 49.99,
                thumbnail = "",
                quantity = 2
            )
        )
        val response = assistant.generateResponse("Check my cart", sampleCatalog, cartItems)
        assertTrue(response.text.contains("2 items in your cart"))
        assertTrue(response.text.contains("Free Express Shipping"))
        assertTrue(response.suggestedProducts.none { it.id == 2 })
    }

    @Test
    fun generateResponse_directProductInquiry_providesDetailedBreakdown() {
        val response = assistant.generateResponse(
            "Tell me about Wireless Headphones", sampleCatalog, emptyList()
        )
        assertTrue(response.text.contains("Wireless Headphones"))
        assertTrue(response.text.contains("AI Insights"))
        assertEquals(1, response.suggestedProducts.size)
        assertEquals(1, response.suggestedProducts.first().id)
    }

    @Test
    fun generateResponse_keywordSearch_returnsCategoryMatches() {
        val response =
            assistant.generateResponse("I want to buy some perfume", sampleCatalog, emptyList())
        assertTrue(response.suggestedProducts.any { it.category == "fragrances" })
    }

    @Test
    fun generateProductInsight_createsComprehensiveHighlights() {
        val product = sampleCatalog[0]
        val insight = assistant.generateProductInsight(product)

        assertEquals(1, insight.productId)
        assertNotNull(insight.headline)
        assertTrue(insight.highlights.any { it.contains("4.8") })
        assertTrue(insight.highlights.any { it.contains("Low inventory") || it.contains("3 units") })
        assertNotNull(insight.bestFor)
        assertNotNull(insight.verdict)
    }
}
