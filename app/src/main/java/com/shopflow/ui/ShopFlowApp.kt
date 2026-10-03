package com.shopflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import coil3.compose.AsyncImage
import com.shopflow.data.CartItem
import com.shopflow.data.Product
import com.shopflow.ui.theme.ShopFlowTheme
import com.shopflow.viewmodel.CartUiState
import com.shopflow.viewmodel.CatalogUiState
import com.shopflow.viewmodel.ShopViewModel
import java.text.NumberFormat
import java.util.Locale

private sealed interface ShopRoute : NavKey
private data object CatalogRoute : ShopRoute
private data object CartRoute : ShopRoute
private data class ProductRoute(val productId: Int) : ShopRoute

@Composable
fun ShopFlowApp(viewModel: ShopViewModel) {
    val catalogUiState by viewModel.catalogUiState.collectAsStateWithLifecycle()
    val cartUiState by viewModel.cartUiState.collectAsStateWithLifecycle()
    ShopFlowApp(
        catalogUiState = catalogUiState,
        cartUiState = cartUiState,
        onQueryChanged = viewModel::setQuery,
        onRefreshProducts = viewModel::refreshProducts,
        getProduct = { id -> catalogUiState.products.find { it.id == id } },
        onAddToCart = viewModel::addToCart,
        onChangeQuantity = viewModel::changeQuantity,
        onRemoveFromCart = viewModel::removeFromCart
    )
}

@Composable
fun ShopFlowApp(
    catalogUiState: CatalogUiState,
    cartUiState: CartUiState,
    onQueryChanged: (String) -> Unit = {},
    onRefreshProducts: () -> Unit = {},
    getProduct: (Int) -> Product? = { id -> catalogUiState.products.find { it.id == id } },
    onAddToCart: (Product) -> Unit = {},
    onChangeQuantity: (productId: Int, delta: Int) -> Unit = { _, _ -> },
    onRemoveFromCart: (productId: Int) -> Unit = {}
) {
    val backStack = remember { mutableStateListOf<ShopRoute>(CatalogRoute) }
    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        entryProvider = entryProvider {
            entry<CatalogRoute> {
                CatalogScreen(
                    state = catalogUiState,
                    cart = cartUiState,
                    onQueryChanged = onQueryChanged,
                    onRefreshProducts = onRefreshProducts,
                    openCart = { backStack.add(CartRoute) },
                    openProduct = { backStack.add(ProductRoute(it)) }
                )
            }
            entry<ProductRoute> { route ->
                ProductDetailScreen(
                    product = getProduct(route.productId),
                    cart = cartUiState,
                    onAddToCart = onAddToCart,
                    navigateBack = { backStack.removeLastOrNull() },
                    openCart = { backStack.add(CartRoute) }
                )
            }
            entry<CartRoute> {
                CartScreen(
                    state = cartUiState,
                    onChangeQuantity = onChangeQuantity,
                    onRemoveFromCart = onRemoveFromCart,
                    navigateBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogScreen(
    state: CatalogUiState,
    cart: CartUiState,
    onQueryChanged: (String) -> Unit,
    onRefreshProducts: () -> Unit,
    openCart: () -> Unit,
    openProduct: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ShopFlow", fontWeight = FontWeight.Bold) },
                actions = { CartButton(cart.itemCount, openCart) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            SearchField(state.query, onQueryChanged)
            Spacer(Modifier.height(12.dp))
            CatalogContent(state, onRefreshProducts, openProduct)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        label = { Text("Search products") }
    )
}

@Composable
private fun CatalogContent(
    state: CatalogUiState,
    retry: () -> Unit,
    openProduct: (Int) -> Unit
) {
    when {
        state.isLoading && state.products.isEmpty() -> LoadingState()
        state.products.isEmpty() && state.errorMessage != null -> ErrorState(state.errorMessage, retry)
        state.products.isEmpty() -> EmptyState(
            title = if (state.query.isBlank()) "No products available" else "No matching products",
            action = if (state.query.isBlank()) retry else null
        )
        else -> {
            Column(Modifier.fillMaxSize()) {
                state.errorMessage?.let { message -> OfflineNotice(message, retry) }
                Text(
                    text = if (state.query.isBlank()) "Catalog" else "Search results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.products, key = Product::id) { product ->
                        ProductCard(product, onClick = { openProduct(product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        AsyncImage(
            model = product.thumbnail,
            contentDescription = product.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.padding(12.dp)) {
            Text(
                text = product.title,
                modifier = Modifier.height(48.dp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(price(product.price), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Rating(product.rating)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailScreen(
    product: Product?,
    cart: CartUiState,
    onAddToCart: (Product) -> Unit,
    navigateBack: () -> Unit,
    openCart: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Product details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { CartButton(cart.itemCount, openCart) }
            )
        }
    ) { padding ->
        product?.let { item ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AsyncImage(
                        model = item.thumbnail,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Fit
                    )
                }
                item {
                    Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(price(item.price), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Rating(item.rating)
                    }
                }
                item { Text(item.description, style = MaterialTheme.typography.bodyLarge) }
                item { DetailFacts(item) }
                item {
                    Button(
                        onClick = { onAddToCart(item) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add to cart")
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("This product is not in the saved catalog.")
        }
    }
}

@Composable
private fun DetailFacts(product: Product) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FactRow("Category", product.category.replace('-', ' ').replaceFirstChar { it.uppercase() })
        FactRow("Brand", product.brand ?: "Not listed")
        FactRow("Stock", "${product.stock} available")
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.widthIn(max = 220.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartScreen(
    state: CartUiState,
    onChangeQuantity: (productId: Int, delta: Int) -> Unit,
    onRemoveFromCart: (productId: Int) -> Unit,
    navigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Your cart", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { if (state.items.isNotEmpty()) CartTotal(state) }
    ) { padding ->
        if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("Your cart is empty")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.items, key = CartItem::productId) { item ->
                    CartItemCard(
                        item = item,
                        increase = { onChangeQuantity(item.productId, 1) },
                        decrease = { onChangeQuantity(item.productId, -1) },
                        remove = { onRemoveFromCart(item.productId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(item: CartItem, increase: () -> Unit, decrease: () -> Unit, remove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = item.title,
                modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(price(item.price), color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = decrease) { Icon(Icons.Default.Remove, contentDescription = "Decrease quantity") }
                    Text(item.quantity.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                    IconButton(onClick = increase) { Icon(Icons.Default.Add, contentDescription = "Increase quantity") }
                }
            }
            IconButton(onClick = remove) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove ${item.title}")
            }
        }
    }
}

@Composable
private fun CartTotal(state: CartUiState) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${state.itemCount} ${if (state.itemCount == 1) "item" else "items"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(price(state.total), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CartButton(itemCount: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (itemCount > 0) Badge { Text(itemCount.coerceAtMost(99).toString()) } }) {
            Icon(Icons.Default.ShoppingBag, contentDescription = "Open cart")
        }
    }
}

@Composable
private fun Rating(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(3.dp))
        Text(String.format(Locale.US, "%.1f", rating), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading products...") }
}

@Composable
private fun ErrorState(message: String, retry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = retry) { Text("Retry") }
        }
    }
}

@Composable
private fun EmptyState(title: String, action: (() -> Unit)? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        action?.let {
            Spacer(Modifier.height(16.dp))
            Button(onClick = it) { Text("Retry") }
        }
    }
}

@Composable
private fun OfflineNotice(message: String, retry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Text(
                "Retry",
                modifier = Modifier.clickable(onClick = retry).padding(start = 12.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun price(amount: Double): String = NumberFormat.getCurrencyInstance(Locale.US).format(amount)

private val sampleProducts = listOf(
    Product(
        id = 1,
        title = "Wireless Headphones",
        description = "High-quality wireless headphones with active noise cancellation.",
        price = 99.99,
        rating = 4.5,
        category = "electronics",
        brand = "AudioTech",
        stock = 15,
        thumbnail = "https://cdn.dummyjson.com/products/images/beauty/Essence%20Mascara%20Lash%20Princess/1.png"
    ),
    Product(
        id = 2,
        title = "Smart Watch",
        description = "Feature-rich smartwatch with health and fitness tracking.",
        price = 149.99,
        rating = 4.2,
        category = "electronics",
        brand = "TechTime",
        stock = 8,
        thumbnail = "https://cdn.dummyjson.com/products/images/beauty/Essence%20Mascara%20Lash%20Princess/1.png"
    )
)

private val sampleCartItems = listOf(
    CartItem(
        productId = 1,
        title = "Wireless Headphones",
        price = 99.99,
        thumbnail = "https://cdn.dummyjson.com/products/images/beauty/Essence%20Mascara%20Lash%20Princess/1.png",
        quantity = 1
    )
)

@Preview(showBackground = true)
@Composable
private fun ShopFlowAppPreview() {
    ShopFlowTheme {
        ShopFlowApp(
            catalogUiState = CatalogUiState(
                products = sampleProducts,
                isLoading = false
            ),
            cartUiState = CartUiState(
                items = sampleCartItems
            )
        )
    }
}
