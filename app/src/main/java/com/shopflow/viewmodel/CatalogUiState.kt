package com.shopflow.viewmodel

import androidx.compose.runtime.Immutable
import com.shopflow.data.Product

@Immutable
data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val errorMessage: String? = null
)
