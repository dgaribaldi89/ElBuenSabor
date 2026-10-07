package com.example.elbuensabor.data.remote.dto

data class OrderDto(
    val pedidoId: String = "",
    val usuarioId: String = "",
    val total: Double = 0.0,
    val direccionEntrega: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val fecha: String = "",
    // Se guarda además como milisegundos para ordenar correctamente los pedidos.
    // Se conserva "fecha" como texto para mantener compatibilidad con pedidos antiguos.
    val fechaMillis: Long = 0L,
    val estado: String = "PENDIENTE",
    val items: List<CartItemDto> = emptyList()
)

data class CartItemDto(
    val nombre: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 0
)
