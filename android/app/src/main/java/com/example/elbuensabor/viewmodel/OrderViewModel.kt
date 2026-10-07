package com.example.elbuensabor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elbuensabor.data.local.entities.CartEntity
import com.example.elbuensabor.data.remote.dto.CartItemDto
import com.example.elbuensabor.data.remote.dto.OrderDto
import com.example.elbuensabor.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderViewModel(
    private val repository: OrderRepository
) : ViewModel() {

    private val _orderState = MutableStateFlow<Result<Unit>?>(null)
    val orderState: StateFlow<Result<Unit>?> = _orderState

    private val _orders = MutableStateFlow<List<OrderDto>>(emptyList())
    val orders: StateFlow<List<OrderDto>> = _orders

    private val _allOrders = MutableStateFlow<List<OrderDto>>(emptyList())
    val allOrders: StateFlow<List<OrderDto>> = _allOrders

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private fun orderTime(order: OrderDto): Long {
        if (order.fechaMillis > 0L) return order.fechaMillis

        // Compatibilidad con pedidos creados antes de agregar fechaMillis.
        return try {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .parse(order.fecha)
                ?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun loadOrders(usuarioId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val remoteOrders = repository.getOrdersRemote(usuarioId)
            _orders.value = remoteOrders.sortedByDescending(::orderTime)
            _isLoading.value = false
        }
    }

    fun loadAllOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            val remoteOrders = repository.getAllOrdersRemote()
            _allOrders.value = remoteOrders.sortedByDescending(::orderTime)
            _isLoading.value = false
        }
    }

    fun updateOrderStatus(pedidoId: String, nuevoEstado: String) {
        viewModelScope.launch {
            repository.updateOrderStatusRemote(pedidoId, nuevoEstado)
            loadAllOrders()
        }
    }

    fun placeOrder(
        usuarioId: String,
        total: Double,
        lat: Double,
        lon: Double,
        address: String,
        cartItems: List<CartEntity>
    ) {
        if (usuarioId.isBlank() || address.isBlank() || cartItems.isEmpty() || total <= 0.0) {
            _orderState.value = Result.failure(
                IllegalArgumentException("Faltan datos para registrar el pedido.")
            )
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val now = System.currentTimeMillis()
            val orderDto = OrderDto(
                usuarioId = usuarioId,
                total = total,
                direccionEntrega = address.trim(),
                latitud = lat,
                longitud = lon,
                fecha = SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
                ).format(Date(now)),
                fechaMillis = now,
                items = cartItems.map {
                    CartItemDto(
                        nombre = it.nombreProducto,
                        precio = it.precio,
                        cantidad = it.cantidad
                    )
                }
            )

            _orderState.value = repository.createOrderRemote(orderDto)
            _isLoading.value = false
        }
    }

    fun resetOrderState() {
        _orderState.value = null
    }
}
