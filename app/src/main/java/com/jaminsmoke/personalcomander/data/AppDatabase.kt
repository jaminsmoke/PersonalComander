package com.jaminsmoke.personalcomander.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Mesa::class, Producto::class, Pedido::class, LineaPedido::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mesaDao(): MesaDao
    abstract fun productoDao(): ProductoDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun lineaPedidoDao(): LineaPedidoDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mesas ADD COLUMN posX REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE mesas ADD COLUMN posY REAL NOT NULL DEFAULT 0")
            }
        }
    }
}
