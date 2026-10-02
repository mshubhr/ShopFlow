package com.shopflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shopflow.data.CartItem
import com.shopflow.data.Product
import com.shopflow.data.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class CartUiState(val items: List<CartItem> = emptyList()) {
    val itemCount: Int get() = items.sumOf(CartItem::quantity)
    val total: Double get() = items.sumOf { it.price * it.quantity }
}

class ShopViewModel(private val repository: ProductRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)

    val catalogUiState: StateFlow<CatalogUiState> = combine(
        repository.products,
        query,
        isLoading,
        errorMessage
    ) { products, searchQuery, loading, error ->
        val normalized = searchQuery.trim()
        val filtered = if (normalized.isBlank()) products else products.filter { product ->
            product.title.contains(normalized, ignoreCase = true) ||
                product.category.contains(normalized, ignoreCase = true) ||
                product.brand.orEmpty().contains(normalized, ignoreCase = true)
        }
        CatalogUiState(filtered, searchQuery, loading, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    val cartUiState: StateFlow<CartUiState> = repository.cartItems
        .map(::CartUiState)
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
                .onFailure { errorMessage.value = errorMessageFor(it) }
            isLoading.value = false
        }
    }

    fun product(productId: Int): Flow<Product?> = repository.product(productId)

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

class ShopViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ShopViewModel::class.java))
        return ShopViewModel(repository) as T
    }
}
