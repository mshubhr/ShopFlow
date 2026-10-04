package com.shopflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shopflow.data.Product
import com.shopflow.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class ShopViewModel(private val repository: ProductRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)
    private val isLoadingMore = MutableStateFlow(false)
    private val canLoadMore = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val filteredProducts = combine(repository.products, query) { products, searchQuery ->
        val normalized = searchQuery.trim()
        if (normalized.isBlank()) products else products.filter { product ->
            product.title.contains(normalized, ignoreCase = true) || product.category.contains(
                normalized, ignoreCase = true
            ) || product.brand.orEmpty().contains(normalized, ignoreCase = true)
        }
    }

    private val catalogStatus = combine(
        isLoading, isLoadingMore, canLoadMore, errorMessage
    ) { loading, loadingMore, hasMore, error ->
        CatalogStatus(loading, loadingMore, hasMore, error)
    }

    val catalogUiState: StateFlow<CatalogUiState> = combine(
        filteredProducts, query, catalogStatus
    ) { products, searchQuery, status ->
        CatalogUiState(
            products = products,
            query = searchQuery,
            isLoading = status.isLoading,
            isLoadingMore = status.isLoadingMore,
            canLoadMore = status.canLoadMore,
            errorMessage = status.errorMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    val cartUiState: StateFlow<CartUiState> = combine(repository.cartItems, repository.products) { items, products ->
        CartUiState(items, products.associateBy(Product::id, Product::stock))
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    init {
        refreshProducts()
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun refreshProducts() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            runCatching { repository.refreshProducts() }
                .onSuccess { canLoadMore.value = it.hasMore }
                .onFailure { errorMessage.value = errorMessageFor(it) }
            isLoading.value = false
        }
    }

    fun loadNextPage() {
        if (isLoadingMore.value || !canLoadMore.value) return
        viewModelScope.launch {
            isLoadingMore.value = true
            runCatching { repository.loadNextPage() }
                .onSuccess { canLoadMore.value = it.hasMore }
                .onFailure { errorMessage.value = errorMessageFor(it) }
            isLoadingMore.value = false
        }
    }

    fun addToCart(product: Product) {
        viewModelScope.launch { repository.addToCart(product) }
    }

    fun changeQuantity(productId: Int, delta: Int) {
        viewModelScope.launch { repository.changeQuantity(productId, delta) }
    }

    fun removeFromCart(productId: Int) {
        viewModelScope.launch { repository.removeFromCart(productId) }
    }

    private fun errorMessageFor(error: Throwable): String = when (error) {
        is IOException -> "We could not reach the catalog. Your saved cart is still ready to use."
        else -> "The catalog could not be updated. Please try again."
    }
}

private data class CatalogStatus(
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val canLoadMore: Boolean,
    val errorMessage: String?
)

class ShopViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            return modelClass.cast(ShopViewModel(repository))
                ?: throw IllegalArgumentException("Unable to cast ViewModel to ${modelClass.name}")
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
