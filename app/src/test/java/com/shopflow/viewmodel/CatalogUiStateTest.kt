package com.shopflow.viewmodel

import com.shopflow.data.Product
import com.shopflow.data.remote.DataState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogUiStateTest {

    private val sampleProduct = Product(
        id = 1,
        title = "Test Product",
        description = "Description",
        price = 99.99,
        rating = 4.5,
        category = "test",
        brand = "Brand",
        stock = 10,
        thumbnail = ""
    )

    @Test
    fun defaultState_isDataStateLoading() {
        val state = CatalogUiState()
        assertTrue(state.dataState is DataState.Loading)
        assertTrue(state.isLoading)
        assertTrue(state.products.isEmpty())
        assertNull(state.errorMessage)
    }

    @Test
    fun successState_exposesProductsAndNotLoading() {
        val products = listOf(sampleProduct)
        val state = CatalogUiState(dataState = DataState.Success(products))

        assertTrue(state.dataState is DataState.Success)
        assertFalse(state.isLoading)
        assertEquals(1, state.products.size)
        assertEquals(sampleProduct, state.products[0])
        assertNull(state.errorMessage)
    }

    @Test
    fun errorState_exposesErrorMessage() {
        val state = CatalogUiState(dataState = DataState.Error("Network failure"))

        assertTrue(state.dataState is DataState.Error)
        assertFalse(state.isLoading)
        assertTrue(state.products.isEmpty())
        assertEquals("Network failure", state.errorMessage)
    }

    @Test
    fun dataStateHelpers_workCorrectly() {
        val loading: DataState<String> = DataState.Loading
        val success: DataState<String> = DataState.Success("Hello")
        val error: DataState<String> = DataState.Error("Failed")

        assertTrue(loading.isLoading())
        assertFalse(loading.isError())
        assertNull(loading.dataOrNull())

        assertFalse(success.isLoading())
        assertFalse(success.isError())
        assertEquals("Hello", success.dataOrNull())

        assertFalse(error.isLoading())
        assertTrue(error.isError())
        assertNull(error.dataOrNull())
    }
}
