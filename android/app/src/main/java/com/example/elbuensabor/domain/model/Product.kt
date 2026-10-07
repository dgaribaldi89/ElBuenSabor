package com.example.elbuensabor.domain.model

data class Product(
    val productoId: Int,
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val categoria: String,
    val imagen: String
)