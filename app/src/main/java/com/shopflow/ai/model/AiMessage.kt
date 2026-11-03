package com.shopflow.ai.model

import com.shopflow.data.Product
import java.util.UUID

enum class AiSender {
    USER, ASSISTANT
}

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: AiSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedProducts: List<Product> = emptyList(),
    val suggestedFollowUps: List<String> = emptyList(),
    val isError: Boolean = false
)

data class ProductInsight(
    val productId: Int,
    val headline: String,
    val highlights: List<String>,
    val bestFor: String,
    val verdict: String,
    val isGoodDeal: Boolean
)
