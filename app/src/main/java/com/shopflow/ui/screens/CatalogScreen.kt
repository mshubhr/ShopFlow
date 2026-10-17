package com.shopflow.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shopflow.data.Product
import com.shopflow.data.remote.DataState
import com.shopflow.ui.components.CartButton
import com.shopflow.ui.components.CatalogSkeletonGrid
import com.shopflow.ui.components.EmptyState
import com.shopflow.ui.components.Rating
import com.shopflow.ui.components.price
import com.shopflow.viewmodel.CartUiState
import com.shopflow.viewmodel.CatalogUiState
import com.shopflow.viewmodel.SortOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    state: CatalogUiState,
    cart: CartUiState,
    onQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit = {},
    onSortOptionSelected: (SortOption) -> Unit = {},
    onClearFilters: () -> Unit = {},
    onRefreshProducts: () -> Unit,
    onLoadNextPage: () -> Unit,
    onAddToCart: (Product) -> Unit = {},
    openCart: () -> Unit,
    openProduct: (Int) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showSortSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "ShopFlow",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = { CartButton(cart.itemCount, openCart) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search query")
                            }
                        }
                    },
                    placeholder = { Text("Search products, brands, categories...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            if (state.availableCategories.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.availableCategories) { category ->
                        val isSelected = state.selectedCategory.equals(category, ignoreCase = true)
                        val formattedCategory = if (category.equals("All", ignoreCase = true)) {
                            "All"
                        } else {
                            category.replace('-', ' ').replaceFirstChar { it.uppercase() }
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategorySelected(category) },
                            label = {
                                Text(
                                    formattedCategory,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = MaterialTheme.shapes.medium)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val countText = when {
                    state.products.isEmpty() && !state.isLoading -> "No items"
                    state.products.size == 1 -> "1 item"
                    else -> "${state.products.size} items"
                }
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    onClick = { showSortSheet = true },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(MaterialTheme.shapes.small)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort options",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = state.sortOption.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            CatalogContent(
                state = state,
                cart = cart,
                retry = onRefreshProducts,
                loadNextPage = onLoadNextPage,
                onClearFilters = onClearFilters,
                onAddToCart = onAddToCart,
                openProduct = openProduct
            )
        }

        if (showSortSheet) {
            SortBottomSheet(currentOption = state.sortOption, onSelectOption = {
                onSortOptionSelected(it)
                showSortSheet = false
            }, onDismiss = { showSortSheet = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    currentOption: SortOption, onSelectOption: (SortOption) -> Unit, onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Sort Products",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SortOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectOption(option) }
                        .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (option == currentOption), onClick = { onSelectOption(option) })
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = option.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (option == currentOption) FontWeight.Bold else FontWeight.Normal,
                        color = if (option == currentOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CatalogContent(
    state: CatalogUiState,
    cart: CartUiState,
    retry: () -> Unit,
    loadNextPage: () -> Unit,
    onClearFilters: () -> Unit,
    onAddToCart: (Product) -> Unit,
    openProduct: (Int) -> Unit
) {
    when (val dataState = state.dataState) {
        is DataState.Loading -> {
            if (state.products.isEmpty()) {
                CatalogSkeletonGrid()
            } else {
                CatalogGrid(state, cart, retry, loadNextPage, onAddToCart, openProduct)
            }
        }

        is DataState.Error -> {
            if (state.products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "Unable to load products",
                        message = dataState.message,
                        icon = Icons.Default.WifiOff,
                        actionLabel = "Try Again",
                        action = retry
                    )
                }
            } else {
                CatalogGrid(state, cart, retry, loadNextPage, onAddToCart, openProduct)
            }
        }

        is DataState.Success -> {
            if (state.products.isEmpty()) {
                val hasFilter = state.query.isNotBlank() || !state.selectedCategory.equals(
                    "All", ignoreCase = true
                )
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = if (hasFilter) "No matching products found" else "No products available",
                        message = if (hasFilter) "Try adjusting your search query or category filter." else "Check back later for new arrivals.",
                        icon = Icons.Default.SearchOff,
                        actionLabel = if (hasFilter) "Clear all filters" else "Refresh",
                        action = if (hasFilter) onClearFilters else retry
                    )
                }
            } else {
                CatalogGrid(state, cart, retry, loadNextPage, onAddToCart, openProduct)
            }
        }
    }
}

@Composable
private fun CatalogGrid(
    state: CatalogUiState,
    cart: CartUiState,
    retry: () -> Unit,
    loadNextPage: () -> Unit,
    onAddToCart: (Product) -> Unit,
    openProduct: (Int) -> Unit
) {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 3 }
    }
    val shouldLoadMore by remember {
        derivedStateOf {
            state.query.isBlank() && state.canLoadMore && !state.isLoadingMore && (gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: -1) >= state.products.lastIndex - 5
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) loadNextPage()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minCellWidth =
            if (maxWidth >= 840.dp) 200.dp else if (maxWidth >= 600.dp) 180.dp else 156.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                state.errorMessage?.let { message ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                TextButton(onClick = retry) {
                                    Text(
                                        "Retry",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = minCellWidth),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.products,
                        key = { product -> product.id },
                        contentType = { "product" }) { product ->
                        ProductCard(
                            product = product,
                            canIncrease = cart.canIncrease(product.id),
                            onAddToCart = { onAddToCart(product) },
                            onClick = { openProduct(product.id) })
                    }
                    if (state.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Loading more products...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            gridState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    canIncrease: Boolean,
    onAddToCart: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "${product.title}, ${price(product.price)}, rating ${product.rating} stars"
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = product.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                if (product.stock in 1..5) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Only ${product.stock} left",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                if (!product.brand.isNullOrBlank()) {
                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = product.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Rating(product.rating)

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = price(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FilledIconButton(
                        onClick = onAddToCart,
                        enabled = canIncrease,
                        modifier = Modifier
                            .size(36.dp)
                            .semantics {
                                contentDescription = "Add ${product.title} to cart"
                            },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
