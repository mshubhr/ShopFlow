package com.shopflow.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.shopflow.data.CartItem
import com.shopflow.data.Product
import com.shopflow.ui.screens.CartScreen
import com.shopflow.ui.screens.CatalogScreen
import com.shopflow.ui.screens.ProductDetailScreen
import com.shopflow.ui.theme.ShopFlowTheme
import com.shopflow.viewmodel.CartUiState
import com.shopflow.viewmodel.CatalogUiState
import com.shopflow.viewmodel.ShopViewModel

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
        onLoadNextPage = viewModel::loadNextPage,
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
    onLoadNextPage: () -> Unit = {},
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
                    onLoadNextPage = onLoadNextPage,
                    openCart = { backStack.add(CartRoute) },
                    openProduct = { backStack.add(ProductRoute(it)) })
            }
            entry<ProductRoute> { route ->
                ProductDetailScreen(
                    product = getProduct(route.productId),
                    cart = cartUiState,
                    onAddToCart = onAddToCart,
                    navigateBack = { backStack.removeLastOrNull() },
                    openCart = { backStack.add(CartRoute) })
            }
            entry<CartRoute> {
                CartScreen(
                    state = cartUiState,
                    onChangeQuantity = onChangeQuantity,
                    onRemoveFromCart = onRemoveFromCart,
                    navigateBack = { backStack.removeLastOrNull() })
            }
        })
}

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
    ), Product(
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
                products = sampleProducts, isLoading = false
            ), cartUiState = CartUiState(
                items = sampleCartItems
            )
        )
    }
}
