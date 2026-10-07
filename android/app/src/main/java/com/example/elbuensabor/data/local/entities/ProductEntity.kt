package com.example.elbuensabor.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val productoId: Int = 0,

    val nombre: String,

    val descripcion: String,

    val precio: Double,

    val categoria: String,

    val imagenUrl: String
)