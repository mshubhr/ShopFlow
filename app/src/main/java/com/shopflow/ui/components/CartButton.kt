package com.shopflow.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun CartButton(itemCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = if (itemCount > 0) "Shopping cart, $itemCount items" else "Shopping cart, empty"
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description }
    ) {
        BadgedBox(badge = {
            if (itemCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ) {
                    Text(itemCount.coerceAtMost(99).toString())
                }
            }
        }) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null)
        }
    }
}