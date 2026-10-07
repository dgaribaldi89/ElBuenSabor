package com.example.elbuensabor.data.local.entities

import androidx.room.PrimaryKey
import androidx.room.Entity

@Entity(tableName = "pedidos")
data class OrderEntity(

    @PrimaryKey(autoGenerate = true)
    val pedidoId: Int = 0,

    val usuarioId: String,

    val total: Double,

    val direccionEntrega: String,

    val latitud: Double,

    val longitud: Double,

    val fecha: String,

    val estado: String
)