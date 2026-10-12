package com.shopflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shopflow.data.repository.ProductRepository

class ShopViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            return modelClass.cast(ShopViewModel(repository))
                ?: throw IllegalArgumentException("Unable to cast ViewModel to ${modelClass.name}")
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}