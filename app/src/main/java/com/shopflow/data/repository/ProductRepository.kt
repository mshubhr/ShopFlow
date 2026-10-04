package com.shopflow.data.repository

import com.shopflow.data.CartItem
import com.shopflow.data.Product
import com.shopflow.data.local.CartDao
import com.shopflow.data.local.CartItemEntity
import com.shopflow.data.local.ProductDao
import com.shopflow.data.local.ProductEntity
import com.shopflow.data.remote.DummyJsonApi
import com.shopflow.data.remote.ProductDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProductRepository(
    private val api: DummyJsonApi, private val productDao: ProductDao, private val cartDao: CartDao
) {
    private val paginationMutex = Mutex()
    private var nextSkip = 0
    private var totalProducts = Int.MAX_VALUE

    val products: Flow<List<Product>> = productDao.observeAll().map { entities ->
        entities.map(ProductEntity::toProduct)
    }

    val cartItems: Flow<List<CartItem>> = cartDao.observeAll().map { entities ->
        entities.map(CartItemEntity::toCartItem)
    }

    suspend fun refreshProducts(): PageLoadResult = paginationMutex.withLock {
        val response = api.getProducts(limit = PAGE_SIZE, skip = 0)
        productDao.replaceAll(response.products.map(ProductDto::toEntity))
        nextSkip = response.products.size
        totalProducts = response.total
        PageLoadResult(hasMore = nextSkip < totalProducts)
    }

    suspend fun loadNextPage(): PageLoadResult = paginationMutex.withLock {
        if (nextSkip >= totalProducts) return@withLock PageLoadResult(hasMore = false)
        val response = api.getProducts(limit = PAGE_SIZE, skip = nextSkip)
        productDao.insertAll(response.products.map(ProductDto::toEntity))
        nextSkip += response.products.size
        totalProducts = response.total
        PageLoadResult(hasMore = nextSkip < totalProducts)
    }

    suspend fun searchProducts(query: String): List<Product> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()
        return try {
            val response = api.searchProducts(query = trimmed, limit = 0)
            val entities = response.products.map(ProductDto::toEntity)
            productDao.insertAll(entities)
            entities.map(ProductEntity::toProduct)
        } catch (e: Exception) {
            val localResults = productDao.searchLocally(trimmed)
            if (localResults.isNotEmpty()) {
                localResults.map(ProductEntity::toProduct)
            } else {
                throw e
            }
        }
    }

    suspend fun findProductById(productId: Int): Product? =
        productDao.findById(productId)?.toProduct()

    suspend fun addToCart(product: Product): CartMutationResult {
        val existing = cartDao.findById(product.id)
        if ((existing?.quantity ?: 0) >= product.stock) {
            return CartMutationResult.StockLimitReached(product.stock)
        }
        cartDao.upsert(
            CartItemEntity(
                productId = product.id,
                title = product.title,
                price = product.price,
                thumbnail = product.thumbnail,
                quantity = (existing?.quantity ?: 0) + 1
            )
        )
        return CartMutationResult.Updated
    }

    suspend fun changeQuantity(productId: Int, delta: Int): CartMutationResult {
        val existing = cartDao.findById(productId) ?: return CartMutationResult.ProductUnavailable
        val newQuantity = existing.quantity + delta
        if (newQuantity <= 0) {
            cartDao.delete(productId)
            return CartMutationResult.Updated
        }
        val stock = productDao.findById(productId)?.stock
            ?: return CartMutationResult.ProductUnavailable
        if (newQuantity > stock) return CartMutationResult.StockLimitReached(stock)
        cartDao.upsert(existing.copy(quantity = newQuantity))
        return CartMutationResult.Updated
    }

    suspend fun removeFromCart(productId: Int) = cartDao.delete(productId)

    private companion object {
        const val PAGE_SIZE = 24
    }
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
