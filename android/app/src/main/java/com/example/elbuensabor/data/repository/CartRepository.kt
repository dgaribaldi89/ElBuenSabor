package com.example.elbuensabor.data.repository

import com.example.elbuensabor.data.local.dao.CartDao
import com.example.elbuensabor.data.local.entities.CartEntity

class CartRepository(
    private val cartDao: CartDao
) {

    suspend fun getCartItems(usuarioId: String): List<CartEntity> {
        return cartDao.getCartItems(usuarioId)
    }

    suspend fun addToCart(item: CartEntity) {
        cartDao.addToCart(item)
    }

    suspend fun updateCartItem(item: CartEntity) {
        cartDao.updateCartItem(item)
    }

    suspend fun removeFromCart(item: CartEntity) {
        cartDao.removeFromCart(item)
    }

    suspend fun clearCart(usuarioId: String) {
        cartDao.clearCart(usuarioId)
    }
}
