package com.shopflow.viewmodel

import com.shopflow.data.CartItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartUiStateTest {

    @Test
    fun itemCountAndTotal_calculatedCorrectly() {
        val items = listOf(
            CartItem(productId = 1, title = "Item 1", price = 10.0, thumbnail = "", quantity = 2),
            CartItem(productId = 2, title = "Item 2", price = 25.0, thumbnail = "", quantity = 1)
        )
        val state = CartUiState(items = items)

        assertEquals(3, state.itemCount)
        assertEquals(45.0, state.total, 0.001)
    }

    @Test
    fun quantityFor_returnsCorrectQuantity() {
        val items = listOf(
            CartItem(productId = 10, title = "Item A", price = 5.0, thumbnail = "", quantity = 3)
        )
        val state = CartUiState(items = items)

        assertEquals(3, state.quantityFor(10))
        assertEquals(0, state.quantityFor(99))
    }

    @Test
    fun canIncrease_respectsStockLimits() {
        val items = listOf(
            CartItem(productId = 1, title = "Phone", price = 500.0, thumbnail = "", quantity = 2)
        )
        val stockMap = mapOf(1 to 3)
        val state = CartUiState(items = items, stockByProductId = stockMap)

        assertTrue(state.canIncrease(1))
    }

    @Test
    fun canIncrease_returnsFalseWhenQuantityReachesStock() {
        val items = listOf(
            CartItem(productId = 1, title = "Phone", price = 500.0, thumbnail = "", quantity = 3)
        )
        val stockMap = mapOf(1 to 3)
        val state = CartUiState(items = items, stockByProductId = stockMap)

        assertFalse(state.canIncrease(1))
    }
}
