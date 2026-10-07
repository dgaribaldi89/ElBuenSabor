package com.example.elbuensabor.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "carrito")
data class CartEntity(
    @PrimaryKey(autoGenerate = true)
    val carritoId: Int = 0,
    val usuarioId: String,
    // Firestore usa IDs de texto. Guardarlo como String evita colisiones por hashCode().
    val productoId: String,
    val nombreProducto: String,
    val precio: Double,
    val cantidad: Int,
    val subtotal: Double
)
