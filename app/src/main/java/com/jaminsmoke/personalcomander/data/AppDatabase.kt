package com.jaminsmoke.personalcomander.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Mesa::class, Producto::class, Pedido::class, LineaPedido::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mesaDao(): MesaDao
    abstract fun productoDao(): ProductoDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun lineaPedidoDao(): LineaPedidoDao
}
