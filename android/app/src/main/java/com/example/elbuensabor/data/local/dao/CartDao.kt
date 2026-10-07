package com.example.elbuensabor.data.local.dao

import androidx.room.*
import com.example.elbuensabor.data.local.entities.CartEntity

@Dao
interface CartDao {

    @Query("SELECT * FROM carrito WHERE usuarioId = :usuarioId")
    suspend fun getCartItems(usuarioId: String): List<CartEntity>

    @Insert
    suspend fun addToCart(item: CartEntity)

    @Update
    suspend fun updateCartItem(item: CartEntity)

    @Delete
    suspend fun removeFromCart(item: CartEntity)

    @Query("DELETE FROM carrito WHERE usuarioId = :usuarioId")
    suspend fun clearCart(usuarioId: String)
}
