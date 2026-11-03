package com.shopflow.ai

import com.shopflow.ai.model.AiMessage
import com.shopflow.ai.model.AiSender
import com.shopflow.ai.model.ProductInsight
import com.shopflow.data.CartItem
import com.shopflow.data.Product
import java.util.Locale

class AiShoppingAssistant {

    fun generateResponse(
        query: String, catalog: List<Product>, cart: List<CartItem>
    ): AiMessage {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return AiMessage(
                sender = AiSender.ASSISTANT,
                text = "Please ask me a question or tell me what product you are looking for!",
                suggestedFollowUps = listOf(
                    "✨ Top rated items", "💰 Deals under $50", "🛒 Review my cart"
                )
            )
        }

        val lowerQuery = trimmed.lowercase(Locale.ROOT)

        if (isGreeting(lowerQuery)) {
            val totalInCatalog = catalog.size
            return AiMessage(
                sender = AiSender.ASSISTANT,
                text = "Hello! 👋 I'm your ShopFlow AI Assistant.\n\nI can help you explore our $totalInCatalog items in the catalog. You can ask me to:\n• Find items within a budget (e.g. \"products under $50\")\n• Recommend top-rated products in any category\n• Analyze your cart and suggest complementary items\n• Compare products and provide buying advice\n\nWhat are you shopping for today?",
                suggestedFollowUps = listOf(
                    "✨ What's top rated?",
                    "💰 Budget picks under $30",
                    "🛒 Check my cart",
                    "📱 Tech & Electronics"
                )
            )
        }

        if (isCartQuery(lowerQuery)) {
            return handleCartQuery(cart, catalog)
        }

        val budget = extractBudget(lowerQuery)
        val wantsTopRated = isTopRatedQuery(lowerQuery)

        val directProductMatch = catalog.firstOrNull { product ->
            lowerQuery.contains(product.title.lowercase(Locale.ROOT))
        }

        if (directProductMatch != null && (lowerQuery.contains("tell me about") || lowerQuery.contains(
                "details"
            ) || lowerQuery.contains("is") || lowerQuery.contains("how good"))
        ) {
            return handleProductInquiry(directProductMatch)
        }

        val matchedProducts = filterAndRankProducts(
            query = lowerQuery, catalog = catalog, budget = budget, topRatedOnly = wantsTopRated
        )

        if (matchedProducts.isNotEmpty()) {
            val count = matchedProducts.size
            val headerText = buildString {
                if (budget != null) {
                    append(
                        "I found $count great option${if (count > 1) "s" else ""} under $${
                            String.format(
                                Locale.US, "%.0f", budget
                            )
                        }"
                    )
                } else if (wantsTopRated) {
                    append("Here are the top-rated recommendations matching your request")
                } else {
                    append("Here are $count recommended product${if (count > 1) "s" else ""} for you")
                }
                append(":")
            }

            val advice = generateShoppingAdvice(matchedProducts)
            val fullText = "$headerText\n\n$advice"

            val followUps = mutableListOf<String>()
            val first = matchedProducts.firstOrNull()
            if (first != null) {
                followUps.add("Tell me more about ${first.title.take(24)}")
            }
            if (budget == null) {
                followUps.add("Filter under $50")
            }
            followUps.add("🛒 Check my cart")

            return AiMessage(
                sender = AiSender.ASSISTANT,
                text = fullText,
                suggestedProducts = matchedProducts.take(6),
                suggestedFollowUps = followUps
            )
        }

        val fallbackProducts = catalog.sortedByDescending { it.rating }.take(4)
        return AiMessage(
            sender = AiSender.ASSISTANT,
            text = "I couldn't find exact matches for \"$trimmed\" in our current inventory. However, here are some of our most popular and highest-rated items that you might love:",
            suggestedProducts = fallbackProducts,
            suggestedFollowUps = listOf(
                "✨ View all electronics",
                "💄 Beauty & Skincare",
                "💰 Deals under $25",
                "🛒 Review my cart"
            )
        )
    }

    fun generateProductInsight(product: Product): ProductInsight {
        val isHighRating = product.rating >= 4.5
        val isGoodDeal = product.price <= 50.0 || (isHighRating && product.price <= 100.0)

        val headline = when {
            isHighRating && isGoodDeal -> "Exceptional value • Highly rated by shoppers"
            isHighRating -> "Top-tier quality • Premium rating of ${product.rating}★"
            isGoodDeal -> "Budget-friendly pick • Great everyday value"
            else -> "Popular choice in ${product.category.replace('-', ' ')}"
        }

        val highlights = mutableListOf<String>()
        highlights.add("Customer satisfaction: ${product.rating} / 5.0 stars with verified ratings")
        highlights.add(
            "Price point: $${
                String.format(
                    Locale.US, "%.2f", product.price
                )
            } (Eligible for Free Standard Delivery)"
        )

        if (product.stock <= 5) {
            highlights.add("Urgency: Low inventory! Only ${product.stock} units remaining in stock")
        } else {
            highlights.add("Availability: In stock (${product.stock} units available, ready to dispatch)")
        }

        if (!product.brand.isNullOrBlank()) {
            highlights.add("Brand: Official ${product.brand} product with authenticity guarantee")
        }

        val bestFor = when (product.category.lowercase(Locale.ROOT)) {
            "beauty", "skin-care", "fragrances" -> "Daily self-care, gifting, and quality-conscious personal routines."
            "laptops", "smartphones", "tablets", "mobile-accessories" -> "Students, professionals, and tech lovers needing dependable performance."
            "groceries", "kitchen-accessories" -> "Home chefs, families, and keeping everyday essentials stocked."
            "furniture", "home-decoration" -> "Home styling, upgrading living spaces, and contemporary aesthetics."
            "mens-watches", "women-watches", "sunglasses", "women-jewellery" -> "Fashion-forward accessorizing, gifting, and everyday elegance."
            else -> "Shoppers looking for verified quality and fast delivery."
        }

        val verdict = when {
            isHighRating && product.stock <= 5 -> "High demand item with top marks. Order promptly before stock runs out."
            isHighRating -> "Highly recommended. Proven track record among our shoppers with top ratings."
            isGoodDeal -> "Solid buy if you're looking for great value for your money."
            else -> "A balanced pick for everyday use backed by our return protection."
        }

        return ProductInsight(
            productId = product.id,
            headline = headline,
            highlights = highlights,
            bestFor = bestFor,
            verdict = verdict,
            isGoodDeal = isGoodDeal
        )
    }

    private fun isGreeting(query: String): Boolean {
        val words = query.split(Regex("[\\s,!.?]+"))
        return words.any {
            it in setOf(
                "hi", "hello", "hey", "hola", "greetings", "help", "start"
            )
        } && !query.contains("product") && !query.contains("cart") && !query.contains("under")
    }

    private fun isCartQuery(query: String): Boolean {
        return query.contains("cart") || query.contains("basket") || query.contains("my items") || query.contains(
            "what did i add"
        ) || query.contains("checkout") || query.contains("shipping")
    }

    private fun isTopRatedQuery(query: String): Boolean {
        return query.contains("best") || query.contains("top") || query.contains("highest") || query.contains(
            "popular"
        ) || query.contains("trending") || query.contains("favorite")
    }

    private fun extractBudget(query: String): Double? {
        val patterns = listOf(
            Regex("(?:under|below|less than|cheaper than|max|budget of|within)\\s+\\$?(\\d+(?:\\.\\d+)?)"),
            Regex("\\$?(\\d+(?:\\.\\d+)?)\\s*(?:dollars?|bucks?)\\s*(?:budget|or less)"),
            Regex("cheap|budget picks|affordable")
        )

        for (pattern in patterns) {
            val match = pattern.find(query)
            if (match != null) {
                return if (match.groupValues.size > 1 && match.groupValues[1].isNotEmpty()) {
                    match.groupValues[1].toDoubleOrNull()
                } else {
                    50.0
                }
            }
        }
        return null
    }

    private fun handleCartQuery(cart: List<CartItem>, catalog: List<Product>): AiMessage {
        if (cart.isEmpty()) {
            val starterRecommendations = catalog.sortedByDescending { it.rating }.take(4)
            return AiMessage(
                sender = AiSender.ASSISTANT,
                text = "Your shopping cart is currently empty! 🛒\n\n" + "Here are some top-rated suggestions you can add right now:",
                suggestedProducts = starterRecommendations,
                suggestedFollowUps = listOf(
                    "✨ Trending products", "💰 Deals under $25", "📱 Tech gadgets"
                )
            )
        }

        val totalItems = cart.sumOf { it.quantity }
        val totalPrice = cart.sumOf { it.price * it.quantity }
        val cartCategories =
            catalog.filter { p -> cart.any { it.productId == p.id } }.map { it.category }.toSet()

        val complementary = catalog.filter { p -> cart.none { it.productId == p.id } }
            .sortedWith(compareByDescending<Product> { p -> if (p.category in cartCategories) 1 else 0 }.thenByDescending { it.rating })
            .take(4)

        val summaryText = buildString {
            append(
                "You have $totalItems item${if (totalItems > 1) "s" else ""} in your cart totaling $${
                    String.format(
                        Locale.US, "%.2f", totalPrice
                    )
                }.\n\n"
            )
            append("🎉 Good news: Your order qualifies for **Free Express Shipping**!\n\n")
            if (complementary.isNotEmpty()) {
                append("Based on the items you've chosen, here are some complementary picks you might want to add:")
            }
        }

        return AiMessage(
            sender = AiSender.ASSISTANT,
            text = summaryText,
            suggestedProducts = complementary,
            suggestedFollowUps = listOf(
                "💰 Find items under $30", "⭐ Top rated picks", "Clear cart"
            )
        )
    }

    private fun handleProductInquiry(product: Product): AiMessage {
        val insight = generateProductInsight(product)
        val text = buildString {
            append("### **${product.title}**\n")
            append(
                "**Price**: $${
                    String.format(
                        Locale.US, "%.2f", product.price
                    )
                } | **Rating**: ${product.rating}★\n"
            )
            append(
                "**Category**: ${
                product.category.replace('-', ' ').replaceFirstChar { it.uppercase() }
            }\n\n")
            append("${product.description}\n\n")
            append("**AI Insights:**\n")
            insight.highlights.forEach { append("• $it\n") }
            append("\n**Verdict**: ${insight.verdict}")
        }

        return AiMessage(
            sender = AiSender.ASSISTANT,
            text = text,
            suggestedProducts = listOf(product),
            suggestedFollowUps = listOf(
                "Add to cart", "Show similar items in ${product.category}", "🛒 Check my cart"
            )
        )
    }

    private fun filterAndRankProducts(
        query: String, catalog: List<Product>, budget: Double?, topRatedOnly: Boolean
    ): List<Product> {
        val queryKeywords =
            query.replace(Regex("\\$?\\d+(?:\\.\\d+)?"), " ").split(Regex("[\\s,!.?]+"))
                .filter { it.length > 2 && it !in STOP_WORDS }

        var candidates = catalog

        if (budget != null) {
            candidates = candidates.filter { it.price <= budget }
        }

        if (topRatedOnly) {
            candidates = candidates.filter { it.rating >= 4.0 }
        }

        if (queryKeywords.isEmpty()) {
            return candidates.sortedByDescending { it.rating }.take(8)
        }

        val scored = candidates.mapNotNull { product ->
            val titleLower = product.title.lowercase(Locale.ROOT)
            val descLower = product.description.lowercase(Locale.ROOT)
            val catLower = product.category.lowercase(Locale.ROOT)
            val brandLower = product.brand?.lowercase(Locale.ROOT) ?: ""

            var textScore = 0
            for (keyword in queryKeywords) {
                if (titleLower.contains(keyword)) textScore += 5
                if (catLower.contains(keyword)) textScore += 4
                if (brandLower.contains(keyword)) textScore += 3
                if (descLower.contains(keyword)) textScore += 1
            }

            if (textScore > 0) {
                val totalScore = textScore + (if (product.rating >= 4.5) 2 else 0)
                product to totalScore
            } else {
                null
            }
        }

        return scored.sortedWith(compareByDescending<Pair<Product, Int>> { it.second }.thenByDescending { it.first.rating })
            .map { it.first }.take(8)
    }

    private fun generateShoppingAdvice(products: List<Product>): String {
        val topProduct = products.first()
        return buildString {
            append("Our top recommendation is **${topProduct.title}** ")
            append(
                "for $${
                    String.format(
                        Locale.US, "%.2f", topProduct.price
                    )
                } (rated ${topProduct.rating}★). "
            )
            if (topProduct.stock <= 5) {
                append("Notice that only ${topProduct.stock} are left in stock! ")
            }
            if (products.size > 1) {
                val second = products[1]
                append(
                    "Another strong choice is **${second.title}** at $${
                        String.format(
                            Locale.US, "%.2f", second.price
                        )
                    }."
                )
            }
        }
    }

    companion object {
        private val STOP_WORDS = setOf(
            "the",
            "and",
            "for",
            "with",
            "show",
            "give",
            "find",
            "some",
            "what",
            "which",
            "are",
            "under",
            "below",
            "than",
            "more",
            "about",
            "product",
            "products",
            "item",
            "items",
            "please",
            "tell",
            "need",
            "want",
            "like",
            "recommend",
            "looking"
        )
    }
}
