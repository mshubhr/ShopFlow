package com.shopflow.viewmodel

enum class SortOption(val displayName: String) {
    DEFAULT("Featured"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    RATING_HIGH_LOW("Top Rated"),
    NAME_A_Z("Name: A to Z")
}
