package com.example.elbuensabor.domain.model

data class Order(
    val pedidoId: Int,
    val total: Double,
    val direccion: String,
    val fecha: String,
    val estado: String
)