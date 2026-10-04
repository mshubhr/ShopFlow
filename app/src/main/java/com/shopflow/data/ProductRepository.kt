package com.shopflow.data

import com.shopflow.data.local.CartDao
import com.shopflow.data.local.CartItemEntity
import com.shopflow.data.local.ProductDao
import com.shopflow.data.local.ProductEntity
import com.shopflow.data.remote.DummyJsonApi
import com.shopflow.data.remote.ProductDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val api: DummyJsonApi, private val productDao: ProductDao, private val cartDao: CartDao
) {
    val products: Flow<List<Product>> = productDao.observeAll().map { entities ->
        entities.map(ProductEntity::toProduct)
    }

    val cartItems: Flow<List<CartItem>> = cartDao.observeAll().map { entities ->
        entities.map(CartItemEntity::toCartItem)
    }

    suspend fun refreshProducts() {
        productDao.replaceAll(api.getProducts(limit = 0).products.map(ProductDto::toEntity))
    }

    suspend fun addToCart(product: Product) {
        val existing = cartDao.findById(product.id)
        cartDao.upsert(
            CartItemEntity(
                productId = product.id,
                title = product.title,
                price = product.price,
                thumbnail = product.thumbnail,
                quantity = (existing?.quantity ?: 0) + 1
            )
        )
    }

    suspend fun changeQuantity(productId: Int, delta: Int) {
        val existing = cartDao.findById(productId) ?: return
        val newQuantity = existing.quantity + delta
        if (newQuantity <= 0) cartDao.delete(productId) else cartDao.upsert(existing.copy(quantity = newQuantity))
    }

    suspend fun removeFromCart(productId: Int) = cartDao.delete(productId)
}

private fun ProductDto.toEntity() = ProductEntity(
    id = id,
    title = title,
    description = description,
    price = price,
    rating = rating,
    category = category,
    brand = brand,
    stock = stock,
    thumbnail = thumbnail
)

private fun ProductEntity.toProduct() = Product(
    id = id,
    title = title,
    description = description,
    price = price,
    rating = rating,
    category = category,
    brand = brand,
    stock = stock,
    thumbnail = thumbnail
)

private fun CartItemEntity.toCartItem() = CartItem(
    productId = productId, title = title, price = price, thumbnail = thumbnail, quantity = quantity
)