package com.shopflow.data.repository

sealed interface CartMutationResult {
    data object Updated : CartMutationResult
    data class StockLimitReached(val stock: Int) : CartMutationResult
    data object ProductUnavailable : CartMutationResult
}