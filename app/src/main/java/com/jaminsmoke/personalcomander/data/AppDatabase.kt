package com.jaminsmoke.personalcomander.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_comander.db"
                )
                    .build()
                    .also { db ->
                        INSTANCE = db
                        seedIfEmpty(db)
                    }
            }
        }

        private fun seedIfEmpty(db: AppDatabase) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                if (db.mesaDao().count() == 0) {
                    db.mesaDao().insertAll(Seed.mesas())
                    db.productoDao().insertAll(Seed.productos())
                }
            }
        }
    }
}
