package com.example.elbuensabor.data.remote.dto

data class ProductDto(
    val productoId: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precio: Double = 0.0,
    val categoria: String = "",
    // Compatibilidad con productos antiguos que todavía usan una URL.
    val imagenUrl: String = "",
    // Imagen seleccionada desde la galería, optimizada y guardada como Base64 en Firestore.
    val imagenBase64: String = ""
)
