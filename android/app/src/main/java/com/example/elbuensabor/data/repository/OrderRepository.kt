package com.example.elbuensabor.data.repository

import com.example.elbuensabor.data.local.dao.OrderDao
import com.example.elbuensabor.data.local.entities.OrderEntity
import com.example.elbuensabor.data.remote.dto.OrderDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class OrderRepository(
    private val orderDao: OrderDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun createOrderRemote(order: OrderDto): Result<Unit> {
        return try {
            val docRef = firestore.collection("pedidos").document()
            val orderWithId = order.copy(pedidoId = docRef.id)
            docRef.set(orderWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrdersRemote(usuarioId: String): List<OrderDto> {
        return try {
            val snapshot = firestore.collection("pedidos")
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .await()
            snapshot.toObjects(OrderDto::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllOrdersRemote(): List<OrderDto> {
        return try {
            val snapshot = firestore.collection("pedidos")
                .get()
                .await()
            snapshot.toObjects(OrderDto::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateOrderStatusRemote(pedidoId: String, nuevoEstado: String): Result<Unit> {
        return try {
            firestore.collection("pedidos").document(pedidoId)
                .update("estado", nuevoEstado)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrderLocal(order: OrderEntity) {
        orderDao.createOrder(order)
    }

    suspend fun getOrdersLocal(usuarioId: String): List<OrderEntity> {
        return orderDao.getOrdersByUser(usuarioId)
    }
}
