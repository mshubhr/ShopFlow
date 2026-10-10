package com.shopflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shopflow.data.Product
import com.shopflow.data.remote.DataState
import com.shopflow.data.repository.ProductRepository
import kotlinx.coroutines.FlowPreview
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
import kotlin.time.Duration.Companion.milliseconds

class ShopViewModel(
    private val repository: ProductRepository, private val searchDebounceMillis: Long = 300L
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val catalogDataState = MutableStateFlow<DataState<Unit>>(DataState.Loading)
    private val isLoadingMore = MutableStateFlow(false)
    private val canLoadMore = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val searchDataState = MutableStateFlow<DataState<List<Product>>?>(null)

    private val catalogStatus = combine(
        catalogDataState, isLoadingMore, canLoadMore, errorMessage
    ) { catState, loadingMore, hasMore, errMessage ->
        CatalogStatus(catState, loadingMore, hasMore, errMessage)
    }

    private val searchStatus = combine(
        searchDataState, query
    ) { sState, searchQuery ->
        SearchStatus(sState, searchQuery)
    }

    val catalogUiState: StateFlow<CatalogUiState> = combine(
        repository.products, query, catalogStatus, searchStatus
    ) { products, searchQuery, catStatus, sStatus ->
        val isQueryBlank = searchQuery.trim().isBlank()
        if (isQueryBlank) {
            val uiDataState = when (val catState = catStatus.dataState) {
                is DataState.Loading -> if (products.isEmpty()) DataState.Loading else DataState.Success(
                    products
                )

                is DataState.Error -> if (products.isEmpty()) DataState.Error(catState.message) else DataState.Success(
                    products
                )

                is DataState.Success -> DataState.Success(products)
            }
            CatalogUiState(
                dataState = uiDataState,
                query = searchQuery,
                isLoadingMore = catStatus.isLoadingMore,
                canLoadMore = catStatus.canLoadMore,
                errorMessage = if (products.isNotEmpty()) (catStatus.errorMessage
                    ?: (catStatus.dataState as? DataState.Error)?.message) else null
            )
        } else {
            val uiDataState = sStatus.dataState ?: DataState.Loading
            CatalogUiState(
                dataState = uiDataState,
                query = searchQuery,
                isLoadingMore = false,
                canLoadMore = false,
                errorMessage = (uiDataState as? DataState.Error)?.message
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    val cartUiState: StateFlow<CartUiState> =
        combine(repository.cartItems, repository.products) { items, products ->
            CartUiState(items, products.associateBy(Product::id, Product::stock))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    init {
        refreshProducts()
        observeSearch()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            query.debounce(searchDebounceMillis.milliseconds).distinctUntilChanged()
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
            searchDataState.value = null
        }
    }

    private suspend fun performSearch(trimmedQuery: String) {
        searchDataState.value = DataState.Loading
        runCatching { repository.searchProducts(trimmedQuery) }.onSuccess { results ->
            searchDataState.value = DataState.Success(results)
        }.onFailure { error ->
            searchDataState.value = DataState.Error(errorMessageFor(error))
        }
    }

    fun refreshProducts() {
        val trimmed = query.value.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch { performSearch(trimmed) }
            return
        }
        viewModelScope.launch {
            catalogDataState.value = DataState.Loading
            errorMessage.value = null
            runCatching { repository.refreshProducts() }.onSuccess {
                canLoadMore.value = it.hasMore
                catalogDataState.value = DataState.Success(Unit)
            }.onFailure { error ->
                val msg = errorMessageFor(error)
                errorMessage.value = msg
                catalogDataState.value = DataState.Error(msg)
            }
        }
    }

    fun loadNextPage() {
        if (isLoadingMore.value || !canLoadMore.value || query.value.trim().isNotBlank()) return
        viewModelScope.launch {
            isLoadingMore.value = true
            runCatching { repository.loadNextPage() }.onSuccess { canLoadMore.value = it.hasMore }
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
    val dataState: DataState<Unit>,
    val isLoadingMore: Boolean,
    val canLoadMore: Boolean,
    val errorMessage: String?
)

private data class SearchStatus(
    val dataState: DataState<List<Product>>?, val query: String
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
