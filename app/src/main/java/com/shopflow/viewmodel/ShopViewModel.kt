package com.shopflow.viewmodel

import androidx.lifecycle.ViewModel
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

data class FilterParams(
    val query: String,
    val category: String,
    val sortOption: SortOption
)

class ShopViewModel(
    private val repository: ProductRepository, private val searchDebounceMillis: Long = 300L
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("All")
    private val sortOption = MutableStateFlow(SortOption.DEFAULT)

    private val catalogDataState = MutableStateFlow<DataState<Unit>>(DataState.Loading)
    private val isLoadingMore = MutableStateFlow(false)
    private val canLoadMore = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val searchDataState = MutableStateFlow<DataState<List<Product>>?>(null)

    private val filterParams = combine(query, selectedCategory, sortOption) { q, cat, sort ->
        FilterParams(q, cat, sort)
    }

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
        repository.products, filterParams, catalogStatus, searchStatus
    ) { products, filters, catStatus, sStatus ->
        val isQueryBlank = filters.query.trim().isBlank()
        val availableCategories = listOf("All") + products.map(Product::category).distinct().sortedBy(String::lowercase)

        if (isQueryBlank) {
            val filteredAndSorted = filterAndSort(products, filters.category, filters.sortOption)
            val uiDataState = when (val catState = catStatus.dataState) {
                is DataState.Loading -> if (products.isEmpty()) DataState.Loading else DataState.Success(
                    filteredAndSorted
                )

                is DataState.Error -> if (products.isEmpty()) DataState.Error(catState.message) else DataState.Success(
                    filteredAndSorted
                )

                is DataState.Success -> DataState.Success(filteredAndSorted)
            }
            CatalogUiState(
                dataState = uiDataState,
                query = filters.query,
                selectedCategory = filters.category,
                availableCategories = availableCategories,
                sortOption = filters.sortOption,
                isLoadingMore = catStatus.isLoadingMore,
                canLoadMore = catStatus.canLoadMore,
                errorMessage = if (products.isNotEmpty()) (catStatus.errorMessage
                    ?: (catStatus.dataState as? DataState.Error)?.message) else null
            )
        } else {
            val baseState = sStatus.dataState ?: DataState.Loading
            val uiDataState = when (baseState) {
                is DataState.Success -> DataState.Success(
                    filterAndSort(baseState.data, filters.category, filters.sortOption)
                )
                is DataState.Error -> baseState
                is DataState.Loading -> DataState.Loading
            }
            CatalogUiState(
                dataState = uiDataState,
                query = filters.query,
                selectedCategory = filters.category,
                availableCategories = availableCategories,
                sortOption = filters.sortOption,
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

    fun setCategory(category: String) {
        selectedCategory.value = category
    }

    fun setSortOption(option: SortOption) {
        sortOption.value = option
    }

    fun clearFilters() {
        query.value = ""
        searchDataState.value = null
        selectedCategory.value = "All"
        sortOption.value = SortOption.DEFAULT
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

    private fun filterAndSort(
        products: List<Product>,
        category: String,
        sort: SortOption
    ): List<Product> {
        val categoryFiltered = if (category.equals("All", ignoreCase = true)) {
            products
        } else {
            products.filter { it.category.equals(category, ignoreCase = true) }
        }
        return when (sort) {
            SortOption.DEFAULT -> categoryFiltered
            SortOption.PRICE_LOW_HIGH -> categoryFiltered.sortedBy(Product::price)
            SortOption.PRICE_HIGH_LOW -> categoryFiltered.sortedByDescending(Product::price)
            SortOption.RATING_HIGH_LOW -> categoryFiltered.sortedByDescending(Product::rating)
            SortOption.NAME_A_Z -> categoryFiltered.sortedBy { it.title.lowercase() }
        }
    }

    private fun errorMessageFor(error: Throwable): String = when (error) {
        is IOException -> "We could not reach the catalog. Your saved cart is still ready to use."
        else -> "The catalog could not be updated. Please try again."
    }
}
