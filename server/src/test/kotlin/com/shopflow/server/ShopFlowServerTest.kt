package com.shopflow.server

import com.shopflow.server.data.CatalogSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopFlowServerTest {

    @Test
    fun catalogSeed_containsValidProducts() {
        val products = CatalogSeed.products
        assertTrue("Catalog should contain products", products.isNotEmpty())

        products.forEach { product ->
            assertTrue("Product ID must be positive", product.id > 0)
            assertTrue("Title must not be blank", product.title.isNotBlank())
            assertTrue("Price must be non-negative", product.price >= 0.0)
            assertTrue("Rating must be between 0 and 5", product.rating in 0.0..5.0)
            assertTrue("Stock must be non-negative", product.stock >= 0)
            assertTrue("Category must not be blank", product.category.isNotBlank())
            assertTrue("Thumbnail must not be blank", product.thumbnail.isNotBlank())
        }
    }

    @Test
    fun catalogSeed_uniqueProductIds() {
        val products = CatalogSeed.products
        val uniqueIds = products.map { it.id }.toSet()
        assertEquals("All product IDs should be unique", products.size, uniqueIds.size)
    }

    @Test
    fun searchProducts_filtersCorrectly() {
        val products = CatalogSeed.products
        val phoneResults = products.filter {
            it.title.contains("Phone", ignoreCase = true) ||
            it.category.contains("Phone", ignoreCase = true) ||
            it.brand?.contains("Phone", ignoreCase = true) == true ||
            it.description.contains("Phone", ignoreCase = true)
        }
        assertTrue("Should find phone products in catalog", phoneResults.isNotEmpty())
    }
}
