package com.jaminsmoke.personalcomander.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Mesa::class, Producto::class, Pedido::class, LineaPedido::class],
    version = 8,
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
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mesas ADD COLUMN girada INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_mesas_comandaActivaId ON mesas(comandaActivaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_pedidos_mesaId ON pedidos(mesaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_pedido_pedidoId ON lineas_pedido(pedidoId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_pedido_productoId ON lineas_pedido(productoId)")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_pedidos_creadoEn ON pedidos(creadoEn)")
            }
        }
    }
}
