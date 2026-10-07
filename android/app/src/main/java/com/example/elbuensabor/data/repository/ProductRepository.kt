package com.example.elbuensabor.data.repository

import com.example.elbuensabor.data.local.dao.ProductDao
import com.example.elbuensabor.data.local.entities.ProductEntity
import com.example.elbuensabor.data.remote.dto.ProductDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProductRepository(
    private val productDao: ProductDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun addProductToFirestore(product: ProductDto): Result<Unit> {
        return try {
            val ref = firestore.collection("productos").document()
            val productWithId = product.copy(productoId = ref.id)
            ref.set(productWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductsFromFirestore(categoria: String): List<ProductDto> {
        return try {
            val snapshot = firestore.collection("productos")
                .whereEqualTo("categoria", categoria)
                .get()
                .await()
            snapshot.toObjects(ProductDto::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllProducts(): List<ProductEntity> {
        return productDao.getAllProducts()
    }

    suspend fun getProductsByCategory(categoria: String): List<ProductEntity> {
        return productDao.getByCategory(categoria)
    }

    suspend fun insertProduct(product: ProductEntity) {
        productDao.insertProduct(product)
    }

    suspend fun updateProductFromFirestore(product: ProductDto): Result<Unit> {
        return try {
            firestore.collection("productos")
                .document(product.productoId)
                .set(product)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProductFromFirestore(productoId: String): Result<Unit> {
        return try {
            firestore.collection("productos")
                .document(productoId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
