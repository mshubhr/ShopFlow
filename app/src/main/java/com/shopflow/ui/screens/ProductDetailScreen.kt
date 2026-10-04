package com.shopflow.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shopflow.data.Product
import com.shopflow.ui.components.CartButton
import com.shopflow.ui.components.Rating
import com.shopflow.ui.components.price
import com.shopflow.viewmodel.CartUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product?,
    cart: CartUiState,
    onAddToCart: (Product) -> Unit,
    navigateBack: () -> Unit,
    openCart: () -> Unit
) {
    var contentVisible by remember(product?.id) { mutableStateOf(false) }
    LaunchedEffect(product?.id) { contentVisible = true }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Product details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { CartButton(cart.itemCount, openCart) })
        }) { padding ->
        product?.let { item ->
            val listState = rememberLazyListState()
            AnimatedVisibility(
                visible = contentVisible,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
                    initialOffsetY = { height -> height / 12 }, animationSpec = tween(320)
                )
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "image", contentType = "header_image") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.shapes.large
                                )
                        ) {
                            AsyncImage(
                                model = item.thumbnail,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.large),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    item(key = "title_price", contentType = "header_info") {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                price(item.price),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(16.dp))
                            Rating(item.rating)
                        }
                    }
                    item(key = "description", contentType = "description") {
                        Text(item.description, style = MaterialTheme.typography.bodyLarge)
                    }
                    item(key = "facts", contentType = "facts") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FactRow(
                                "Category",
                                item.category.replace('-', ' ').replaceFirstChar { it.uppercase() })
                            FactRow("Brand", item.brand ?: "Not listed")
                            FactRow("Stock", "${item.stock} available")
                        }
                    }
                    item(key = "cta", contentType = "cta") {
                        Button(
                            onClick = { onAddToCart(item) },
                            enabled = cart.canIncrease(item.id),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (cart.canIncrease(item.id)) "Add to cart" else "Stock limit reached")
                        }
                    }
                }
            }
        } ?: Box(
            Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center
        ) {
            Text("This product is not in the saved catalog.")
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.widthIn(max = 220.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
