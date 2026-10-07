package com.example.elbuensabor.data.remote.dto

data class UserDto(
    val usuarioId: String = "",
    val nombre: String = "",
    val correo: String = "",
    val rol: String = "CLIENTE"
)