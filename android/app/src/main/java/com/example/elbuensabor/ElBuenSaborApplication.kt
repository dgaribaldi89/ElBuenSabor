package com.example.elbuensabor

import android.app.Application
import com.example.elbuensabor.data.local.db.DatabaseInstance
import com.example.elbuensabor.data.repository.AuthRepository
import com.example.elbuensabor.data.repository.CartRepository
import com.example.elbuensabor.data.repository.OrderRepository
import com.example.elbuensabor.data.repository.ProductRepository

class ElBuenSaborApplication : Application() {
    val database by lazy { DatabaseInstance.getDatabase(this) }
    val productRepository by lazy { ProductRepository(database.productDao()) }
    val authRepository by lazy { AuthRepository(applicationContext) }
    val cartRepository by lazy { CartRepository(database.cartDao()) }
    val orderRepository by lazy { OrderRepository(database.orderDao()) }
}
