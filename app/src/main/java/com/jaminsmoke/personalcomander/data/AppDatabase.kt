package com.jaminsmoke.personalcomander.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Mesa::class, Producto::class, Pedido::class, LineaPedido::class],
    version = 8,
    exportSchema = true
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
        /**
         * v6→v7: añade las foreign keys e índices que las entidades declaran desde v7.
         *
         * SQLite no permite añadir FKs con ALTER TABLE, así que las tablas pedidos y
         * lineas_pedido se recrean conservando todos los datos. La operación es
         * idempotente: si la tabla ya tiene el FK (BD fresh v7+ o ya saneada) no toca nada.
         *
         * IMPORTANTE: los índices se crean con el prefijo `index_` que Room espera.
         * (La versión anterior usaba `idx_` y rompía la validación de la migración.)
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.asegurarEsquemaConFKsEIndices()
            }
        }

        /**
         * v7→v8: índice en pedidos.creadoEn (optimiza el total del día).
         * Además vuelve a aplicar el saneamiento de FKs/índices de forma idempotente,
         * para reparar BDs que ya estaban en v7 creadas con la migración antigua (idx_*).
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.asegurarEsquemaConFKsEIndices()
            }
        }

        private fun SupportSQLiteDatabase.asegurarEsquemaConFKsEIndices() {
            recrearConFK(
                tabla = "pedidos",
                createSql = "CREATE TABLE IF NOT EXISTS `pedidos_migracion` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mesaId` INTEGER NOT NULL, `estado` TEXT NOT NULL, `creadoEn` INTEGER NOT NULL, `cerradoEn` INTEGER, FOREIGN KEY(`mesaId`) REFERENCES `mesas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                columnas = "`id`, `mesaId`, `estado`, `creadoEn`, `cerradoEn`"
            )
            recrearConFK(
                tabla = "lineas_pedido",
                createSql = "CREATE TABLE IF NOT EXISTS `lineas_pedido_migracion` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pedidoId` INTEGER NOT NULL, `productoId` INTEGER NOT NULL, `nombreProducto` TEXT NOT NULL, `precioUnitario` REAL NOT NULL, `cantidad` INTEGER NOT NULL, `creadoEn` INTEGER NOT NULL, `estado` TEXT NOT NULL, FOREIGN KEY(`pedidoId`) REFERENCES `pedidos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`productoId`) REFERENCES `productos`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
                columnas = "`id`, `pedidoId`, `productoId`, `nombreProducto`, `precioUnitario`, `cantidad`, `creadoEn`, `estado`"
            )
            // Eliminar los índices con el prefijo incorrecto (idx_*) que dejaba la
            // migración antigua: Room valida la igualdad exacta de índices y un índice
            // huérfano en mesas rompería la validación.
            execSQL("DROP INDEX IF EXISTS `idx_mesas_comandaActivaId`")
            execSQL("DROP INDEX IF EXISTS `idx_pedidos_mesaId`")
            execSQL("DROP INDEX IF EXISTS `idx_lineas_pedido_pedidoId`")
            execSQL("DROP INDEX IF EXISTS `idx_lineas_pedido_productoId`")
            // Índices con el nombre exacto que Room espera (ver app/schemas/.../8.json)
            execSQL("CREATE INDEX IF NOT EXISTS `index_mesas_comandaActivaId` ON `mesas` (`comandaActivaId`)")
            execSQL("CREATE INDEX IF NOT EXISTS `index_pedidos_mesaId` ON `pedidos` (`mesaId`)")
            execSQL("CREATE INDEX IF NOT EXISTS `index_pedidos_creadoEn` ON `pedidos` (`creadoEn`)")
            execSQL("CREATE INDEX IF NOT EXISTS `index_lineas_pedido_pedidoId` ON `lineas_pedido` (`pedidoId`)")
            execSQL("CREATE INDEX IF NOT EXISTS `index_lineas_pedido_productoId` ON `lineas_pedido` (`productoId`)")
        }

        private fun SupportSQLiteDatabase.recrearConFK(tabla: String, createSql: String, columnas: String) {
            // Si la tabla ya tiene al menos un FK (BD fresh v7+ o ya saneada), no tocar nada
            val tieneFK = query("PRAGMA foreign_key_list(`$tabla`)").use { it.count > 0 }
            if (tieneFK) return
            val tmp = "${tabla}_migracion"
            execSQL("DROP TABLE IF EXISTS `$tmp`")
            execSQL(createSql)
            execSQL("INSERT INTO `$tmp` ($columnas) SELECT $columnas FROM `$tabla`")
            execSQL("DROP TABLE `$tabla`")
            execSQL("ALTER TABLE `$tmp` RENAME TO `$tabla`")
        }
    }
}
