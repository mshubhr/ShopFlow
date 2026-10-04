package com.shopflow.data.fakes

import com.shopflow.data.local.CartDao
import com.shopflow.data.local.CartItemEntity
import com.shopflow.data.local.ProductDao
import com.shopflow.data.local.ProductEntity
import com.shopflow.data.remote.DummyJsonApi
import com.shopflow.data.remote.ProductDto
import com.shopflow.data.remote.ProductResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeProductDao : ProductDao {
    private val productsState = MutableStateFlow<Map<Int, ProductEntity>>(emptyMap())

    override fun observeAll(): Flow<List<ProductEntity>> =
        productsState.map { it.values.sortedBy { p -> p.title.lowercase() } }

    override fun observeById(productId: Int): Flow<ProductEntity?> =
        productsState.map { it[productId] }

    override suspend fun findById(productId: Int): ProductEntity? =
        productsState.value[productId]

    override suspend fun insertAll(products: List<ProductEntity>) {
        productsState.value += products.associateBy { it.id }
    }

    override suspend fun clear() {
        productsState.value = emptyMap()
    }
}

class FakeCartDao : CartDao {
    private val cartItemsState = MutableStateFlow<Map<Int, CartItemEntity>>(emptyMap())

    override fun observeAll(): Flow<List<CartItemEntity>> =
        cartItemsState.map { it.values.sortedBy { c -> c.title.lowercase() } }

    override suspend fun findById(productId: Int): CartItemEntity? =
        cartItemsState.value[productId]

    override suspend fun upsert(item: CartItemEntity) {
        cartItemsState.value += (item.productId to item)
    }

    override suspend fun delete(productId: Int) {
        cartItemsState.value -= productId
    }
}

class FakeDummyJsonApi(
    var productsToReturn: List<ProductDto> = emptyList()
) : DummyJsonApi {
    override suspend fun getProducts(limit: Int, skip: Int): ProductResponse {
        val page = productsToReturn.drop(skip).take(limit)
        return ProductResponse(
            products = page,
            total = productsToReturn.size
        )
    }
}
