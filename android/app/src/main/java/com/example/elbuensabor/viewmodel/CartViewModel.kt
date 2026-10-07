package com.example.elbuensabor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elbuensabor.data.local.entities.CartEntity
import com.example.elbuensabor.data.remote.dto.ProductDto
import com.example.elbuensabor.data.repository.CartRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CartViewModel(
    private val repository: CartRepository
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartEntity>>(emptyList())
    val cartItems: StateFlow<List<CartEntity>> = _cartItems

    private var currentUserId: String? = null

    val total: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setUserId(userId: String?) {
        if (currentUserId != userId) {
            currentUserId = userId
            if (userId != null) {
                loadCart()
            } else {
                _cartItems.value = emptyList()
            }
        }
    }

    fun loadCart() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _cartItems.value = repository.getCartItems(userId)
        }
    }

    fun addProductToCart(product: ProductDto) {
        val userId = currentUserId ?: return
        if (product.productoId.isBlank()) return

        viewModelScope.launch {
            val existingItem = _cartItems.value.find {
                it.productoId == product.productoId
            }

            if (existingItem != null) {
                val newQuantity = existingItem.cantidad + 1
                repository.updateCartItem(
                    existingItem.copy(
                        cantidad = newQuantity,
                        subtotal = newQuantity * existingItem.precio
                    )
                )
            } else {
                repository.addToCart(
                    CartEntity(
                        usuarioId = userId,
                        productoId = product.productoId,
                        nombreProducto = product.nombre,
                        precio = product.precio,
                        cantidad = 1,
                        subtotal = product.precio
                    )
                )
            }
            loadCart()
        }
    }

    fun removeItem(item: CartEntity) {
        viewModelScope.launch {
            repository.removeFromCart(item)
            loadCart()
        }
    }

    fun clearCart() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            repository.clearCart(userId)
            loadCart()
        }
    }
}
