package com.shopflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shopflow.data.Product
import com.shopflow.data.repository.ProductRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class ShopViewModel(
    private val repository: ProductRepository,
    private val searchDebounceMillis: Long = 300L
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)
    private val isLoadingMore = MutableStateFlow(false)
    private val canLoadMore = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val isSearching = MutableStateFlow(false)
    private val searchResults = MutableStateFlow<List<Product>>(emptyList())
    private val searchErrorMessage = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null

    private val catalogStatus = combine(
        isLoading, isLoadingMore, canLoadMore, errorMessage
    ) { loading, loadingMore, hasMore, error ->
        CatalogStatus(loading, loadingMore, hasMore, error)
    }

    private val searchStatus = combine(
        isSearching, searchResults, searchErrorMessage
    ) { searching, results, error ->
        SearchStatus(searching, results, error)
    }

    val catalogUiState: StateFlow<CatalogUiState> = combine(
        repository.products, query, catalogStatus, searchStatus
    ) { products, searchQuery, catStatus, sStatus ->
        val isQueryBlank = searchQuery.trim().isBlank()
        CatalogUiState(
            products = if (isQueryBlank) products else sStatus.results,
            query = searchQuery,
            isLoading = if (isQueryBlank) catStatus.isLoading else sStatus.isSearching,
            isLoadingMore = if (isQueryBlank) catStatus.isLoadingMore else false,
            canLoadMore = if (isQueryBlank) catStatus.canLoadMore else false,
            errorMessage = if (isQueryBlank) catStatus.errorMessage else sStatus.errorMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    val cartUiState: StateFlow<CartUiState> = combine(repository.cartItems, repository.products) { items, products ->
        CartUiState(items, products.associateBy(Product::id, Product::stock))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    init {
        refreshProducts()
        observeSearch()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            query
                .debounce(searchDebounceMillis)
                .distinctUntilChanged()
                .collectLatest { searchQuery ->
                    val trimmed = searchQuery.trim()
                    if (trimmed.isNotBlank()) {
                        performSearch(trimmed)
                    }
                }
        }
    }

    fun setQuery(value: String) {
        query.value = value
        if (value.trim().isBlank()) {
            searchJob?.cancel()
            searchResults.value = emptyList()
            isSearching.value = false
            searchErrorMessage.value = null
        } else {
            isSearching.value = true
        }
    }

    private fun performSearch(trimmedQuery: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            isSearching.value = true
            searchErrorMessage.value = null
            runCatching { repository.searchProducts(trimmedQuery) }
                .onSuccess { results ->
                    searchResults.value = results
                    isSearching.value = false
                }
                .onFailure { error ->
                    isSearching.value = false
                    searchErrorMessage.value = errorMessageFor(error)
                }
        }
    }

    fun refreshProducts() {
        if (query.value.trim().isNotBlank()) {
            performSearch(query.value.trim())
            return
        }
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
        if (isLoadingMore.value || !canLoadMore.value || query.value.trim().isNotBlank()) return
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

private data class SearchStatus(
    val isSearching: Boolean,
    val results: List<Product>,
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
