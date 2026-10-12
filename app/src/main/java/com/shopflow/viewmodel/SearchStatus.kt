package com.shopflow.viewmodel

import com.shopflow.data.Product
import com.shopflow.data.remote.DataState

data class SearchStatus(
    val dataState: DataState<List<Product>>?, val query: String
)