package com.example.elbuensabor.data.local.db

import androidx.room.*
import androidx.room.RoomDatabase
import com.example.elbuensabor.data.local.dao.CartDao
import com.example.elbuensabor.data.local.dao.OrderDao
import com.example.elbuensabor.data.local.dao.ProductDao
import com.example.elbuensabor.data.local.entities.ProductEntity
import com.example.elbuensabor.data.local.entities.CartEntity
import com.example.elbuensabor.data.local.entities.OrderEntity

@Database(
    entities = [
        ProductEntity::class,
        CartEntity::class,
        OrderEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
}