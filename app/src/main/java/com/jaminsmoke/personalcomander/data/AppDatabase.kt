package com.jaminsmoke.personalcomander.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Sala::class, Mesa::class, Producto::class, Pedido::class, LineaPedido::class, Reserva::class],
    version = 14,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun salaDao(): SalaDao
    abstract fun mesaDao(): MesaDao
    abstract fun productoDao(): ProductoDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun lineaPedidoDao(): LineaPedidoDao
    abstract fun reservaDao(): ReservaDao

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

        /**
         * v8→v9: añade `indiceZona` a mesas (ID secuencial dentro de cada zona: B1, T2…).
         * Se rellena con el orden actual: 1,2,3… por zona según id. Idempotente.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mesas ADD COLUMN indiceZona INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """UPDATE mesas SET indiceZona = (
                        SELECT COUNT(*) FROM mesas m2
                        WHERE m2.zona = mesas.zona AND m2.id <= mesas.id
                    )""".trimIndent()
                )
            }
        }

        /**
         * v9→v10: hold de sala — `bloqueada`, `reservaActivaId` en mesas + tabla `reservas`.
         * Sin FK en reservaActivaId (SQLite ALTER no añade FK; integridad vía transacciones).
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mesas ADD COLUMN bloqueada INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE mesas ADD COLUMN reservaActivaId INTEGER DEFAULT NULL")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `reservas` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mesaId` INTEGER NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `paraEpoch` INTEGER,
                        `creadaEn` INTEGER NOT NULL,
                        `canceladaEn` INTEGER,
                        `convertidaEn` INTEGER,
                        FOREIGN KEY(`mesaId`) REFERENCES `mesas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reservas_mesaId` ON `reservas` (`mesaId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mesas_reservaActivaId` ON `mesas` (`reservaActivaId`)")
            }
        }

        /**
         * v10→v11: tabla `salas` (mapa del establecimiento) y `mesas.salaId` en lugar de `zona`.
         * Las zonas distintas se convierten en filas; el prefijo B/T/I sigue saliendo del nombre.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `salas` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `orden` INTEGER NOT NULL
                    )""".trimIndent()
                )
                val nombres = linkedSetOf<String>()
                db.query("SELECT zona, MIN(id) FROM mesas GROUP BY zona ORDER BY MIN(id)").use { c ->
                    while (c.moveToNext()) {
                        val raw = c.getString(0) ?: ""
                        nombres.add(raw.trim().ifBlank { "General" })
                    }
                }
                nombres.forEachIndexed { i, nombre ->
                    db.execSQL(
                        "INSERT INTO `salas` (`nombre`, `orden`) VALUES (?, ?)",
                        arrayOf<Any>(nombre, i),
                    )
                }
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `mesas_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `numero` INTEGER NOT NULL,
                        `alias` TEXT,
                        `forma` TEXT NOT NULL,
                        `salaId` INTEGER NOT NULL,
                        `capacidad` INTEGER NOT NULL,
                        `estado` TEXT NOT NULL,
                        `comandaActivaId` INTEGER,
                        `posX` REAL NOT NULL,
                        `posY` REAL NOT NULL,
                        `girada` INTEGER NOT NULL,
                        `indiceZona` INTEGER NOT NULL,
                        `bloqueada` INTEGER NOT NULL,
                        `reservaActivaId` INTEGER,
                        FOREIGN KEY(`comandaActivaId`) REFERENCES `pedidos`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL ,
                        FOREIGN KEY(`salaId`) REFERENCES `salas`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""".trimIndent()
                )
                db.execSQL(
                    """INSERT INTO `mesas_new` (
                        `id`, `numero`, `alias`, `forma`, `salaId`, `capacidad`, `estado`,
                        `comandaActivaId`, `posX`, `posY`, `girada`, `indiceZona`, `bloqueada`, `reservaActivaId`
                    ) SELECT m.`id`, m.`numero`, m.`alias`, m.`forma`, s.`id`, m.`capacidad`, m.`estado`,
                        m.`comandaActivaId`, m.`posX`, m.`posY`, m.`girada`, m.`indiceZona`, m.`bloqueada`, m.`reservaActivaId`
                    FROM `mesas` m
                    INNER JOIN `salas` s ON s.`nombre` = CASE WHEN TRIM(m.`zona`) = '' THEN 'General' ELSE TRIM(m.`zona`) END
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `mesas`")
                db.execSQL("ALTER TABLE `mesas_new` RENAME TO `mesas`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mesas_comandaActivaId` ON `mesas` (`comandaActivaId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mesas_reservaActivaId` ON `mesas` (`reservaActivaId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mesas_salaId` ON `mesas` (`salaId`)")
            }
        }

        /**
         * v11→v12: `ticketId` en líneas para cruzar SSE `ticket.preparado` / `ticket.recogido`
         * con la comanda local (delta de ronda).
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `lineas_pedido` ADD COLUMN `ticketId` TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_lineas_pedido_ticketId` ON `lineas_pedido` (`ticketId`)"
                )
            }
        }

        /**
         * v12→v13: `productos.codigoBar` (id de red del catálogo de Bar) para espejar
         * `GET /v1/carta` sin cambiar la PK Long.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `productos` ADD COLUMN `codigoBar` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_productos_codigoBar` ON `productos` (`codigoBar`)"
                )
            }
        }

        /**
         * v13→v14: `salas.codigoBar` y `mesas.codigoBar` (ids de red de Bar) para
         * espejar el layout de `GET /v1/estado` sin cambiar las PK Long.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `salas` ADD COLUMN `codigoBar` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_salas_codigoBar` ON `salas` (`codigoBar`)"
                )
                db.execSQL("ALTER TABLE `mesas` ADD COLUMN `codigoBar` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_mesas_codigoBar` ON `mesas` (`codigoBar`)"
                )
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
