package com.shopflow.viewmodel

import androidx.compose.runtime.Immutable
import com.shopflow.data.Product

@Immutable
data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)