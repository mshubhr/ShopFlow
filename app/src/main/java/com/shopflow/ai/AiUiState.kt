package com.shopflow.ai

import com.shopflow.ai.model.AiMessage
import com.shopflow.ai.model.AiSender
import com.shopflow.ai.model.ProductInsight

data class AiUiState(
    val messages: List<AiMessage> = listOf(defaultWelcomeMessage),
    val isGenerating: Boolean = false,
    val quickSuggestions: List<String> = defaultSuggestions,
    val productInsights: Map<Int, ProductInsight> = emptyMap()
) {
    companion object {
        val defaultSuggestions = listOf(
            "✨ Top rated items",
            "💰 Deals under $50",
            "🛒 Review my cart",
            "🎧 Best tech deals",
            "🎁 Gift recommendations"
        )

        val defaultWelcomeMessage = AiMessage(
            id = "welcome-message",
            sender = AiSender.ASSISTANT,
            text = "👋 Hello! I'm ShopFlow AI, your smart shopping companion.\n\n" + "I can help you find products, discover deals under your budget, analyze your cart, or answer questions about any item in our store. What are you looking for today?",
            suggestedFollowUps = defaultSuggestions
        )
    }
}
