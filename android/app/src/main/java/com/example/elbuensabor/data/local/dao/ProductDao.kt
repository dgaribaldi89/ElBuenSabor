package com.example.elbuensabor.data.local.dao

import androidx.room.*
import com.example.elbuensabor.data.local.entities.ProductEntity

@Dao
interface ProductDao {

    @Query("SELECT * FROM productos")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM productos WHERE categoria = :categoria")
    suspend fun getByCategory(categoria: String): List<ProductEntity>

    @Insert
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)
}