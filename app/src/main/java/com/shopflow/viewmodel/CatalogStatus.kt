package com.shopflow.viewmodel

import com.shopflow.data.remote.DataState

data class CatalogStatus(
    val dataState: DataState<Unit>,
    val isLoadingMore: Boolean,
    val canLoadMore: Boolean,
    val errorMessage: String?
)