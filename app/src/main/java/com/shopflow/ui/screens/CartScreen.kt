package com.shopflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shopflow.data.CartItem
import com.shopflow.ui.components.EmptyState
import com.shopflow.ui.components.price
import com.shopflow.viewmodel.CartUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    state: CartUiState,
    onChangeQuantity: (productId: Int, delta: Int) -> Unit,
    onRemoveFromCart: (productId: Int) -> Unit,
    navigateBack: () -> Unit
) {
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Your cart", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = navigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            })
    }, bottomBar = { if (state.items.isNotEmpty()) CartTotal(state) }) { padding ->
        if (state.items.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                EmptyState("Your cart is empty")
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = state.items,
                    key = { it.productId },
                    contentType = { "cart_item" }) { item ->
                    CartItemCard(
                        item = item,
                        increase = { onChangeQuantity(item.productId, 1) },
                        canIncrease = state.canIncrease(item.productId),
                        decrease = { onChangeQuantity(item.productId, -1) },
                        remove = { onRemoveFromCart(item.productId) })
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(
    item: CartItem,
    increase: () -> Unit,
    canIncrease: Boolean,
    decrease: () -> Unit,
    remove: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small
                    )
            ) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(price(item.price), color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = decrease) {
                        Icon(
                            Icons.Default.Remove, contentDescription = "Decrease quantity"
                        )
                    }
                    Text(
                        item.quantity.toString(),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                    IconButton(onClick = increase, enabled = canIncrease) {
                        Icon(
                            Icons.Default.Add, contentDescription = "Increase quantity"
                        )
                    }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "${state.itemCount} ${if (state.itemCount == 1) "item" else "items"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                price(state.total),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
