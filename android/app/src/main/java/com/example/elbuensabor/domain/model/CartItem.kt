package com.example.elbuensabor.domain.model

data class CartItem(
    val productId: Int,
    val nombre: String,
    val precio: Double,
    val cantidad: Int
)