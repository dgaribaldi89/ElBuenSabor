package com.example.elbuensabor.data.local.dao

import androidx.room.*
import com.example.elbuensabor.data.local.entities.OrderEntity

@Dao
interface OrderDao {

    @Insert
    suspend fun createOrder(order: OrderEntity)

    @Query("SELECT * FROM pedidos WHERE usuarioId = :usuarioId")
    suspend fun getOrdersByUser(usuarioId: String): List<OrderEntity>

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("DELETE FROM pedidos")
    suspend fun deleteAllOrders()
}