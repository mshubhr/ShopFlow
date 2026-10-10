package com.shopflow.viewmodel

import androidx.compose.runtime.Immutable
import com.shopflow.data.Product
import com.shopflow.data.remote.DataState

@Immutable
data class CatalogUiState(
    val dataState: DataState<List<Product>> = DataState.Loading,
    val query: String = "",
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val errorMessage: String? = (dataState as? DataState.Error)?.message
) {
    val products: List<Product>
        get() = (dataState as? DataState.Success)?.data ?: emptyList()

    val isLoading: Boolean
        get() = dataState is DataState.Loading

    constructor(
        products: List<Product>,
        query: String = "",
        isLoading: Boolean = false,
        isLoadingMore: Boolean = false,
        canLoadMore: Boolean = false,
        errorMessage: String? = null
    ) : this(
        dataState = when {
            isLoading -> DataState.Loading
            errorMessage != null && products.isEmpty() -> DataState.Error(errorMessage)
            else -> DataState.Success(products)
        },
        query = query,
        isLoadingMore = isLoadingMore,
        canLoadMore = canLoadMore,
        errorMessage = errorMessage
    )
}
