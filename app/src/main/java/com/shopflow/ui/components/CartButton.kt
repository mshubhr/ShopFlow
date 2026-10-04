package com.shopflow.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun CartButton(itemCount: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = {
            if (itemCount > 0) Badge {
                Text(
                    itemCount.coerceAtMost(99).toString()
                )
            }
        }) {
            Icon(Icons.Default.ShoppingBag, contentDescription = "Open cart")
        }
    }
}