package com.jaminsmoke.personalcomander.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de migración de la BD. Recrean a mano los esquemas que generaba Room en cada
 * versión publicada y abren la BD con el MISMO builder de producción
 * (Room.databaseBuilder + addMigrations). Room aplica las migraciones y valida el
 * esquema final contra las entidades actuales: si la migración deja índices con el
 * nombre incorrecto o faltan FKs, lanza IllegalStateException (el crash que sufrían
 * los usuarios al actualizar).
 *
 * Historico de versiones:
 *  v1 (f9b016d) -> v4: mesas con FK comandaActivaId→pedidos (sin posX/posY)
 *  v4 -> v5: +posX, +posY        (board con posición libre)
 *  v5 -> v6: +girada             (girar mesas rectangulares)
 *  v6 -> v7: FKs + índices       (M1 — ver nota en AppDatabase)
 *  v7 -> v8: +índice creadoEn    (optimización total del día)
 */
@RunWith(AndroidJUnit4::class)
class MigracionesTest {

    private val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val sqlMesas = "CREATE TABLE IF NOT EXISTS `mesas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `numero` INTEGER NOT NULL, `alias` TEXT, `forma` TEXT NOT NULL, `zona` TEXT NOT NULL, `capacidad` INTEGER NOT NULL, `estado` TEXT NOT NULL, `comandaActivaId` INTEGER, FOREIGN KEY(`comandaActivaId`) REFERENCES `pedidos`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
    private val sqlProductos = "CREATE TABLE IF NOT EXISTS `productos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `categoria` TEXT NOT NULL, `precio` REAL NOT NULL, `disponible` INTEGER NOT NULL)"
    private val sqlPedidos = "CREATE TABLE IF NOT EXISTS `pedidos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mesaId` INTEGER NOT NULL, `estado` TEXT NOT NULL, `creadoEn` INTEGER NOT NULL, `cerradoEn` INTEGER)"
    private val sqlLineas = "CREATE TABLE IF NOT EXISTS `lineas_pedido` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pedidoId` INTEGER NOT NULL, `productoId` INTEGER NOT NULL, `nombreProducto` TEXT NOT NULL, `precioUnitario` REAL NOT NULL, `cantidad` INTEGER NOT NULL, `creadoEn` INTEGER NOT NULL, `estado` TEXT NOT NULL)"

    /**
     * Crea una BD con el esquema de la versión indicada (sin índices/FKs extra salvo
     * los que esa versión tenía) e inserta datos representativos.
     */
    private fun crearBD(nombre: String, version: Int, rota: Boolean = false) {
        ctx.deleteDatabase(nombre)
        val db = SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath(nombre), null)
        db.version = version
        db.execSQL(sqlMesas)
        // Índice implícito que Room generaba para el FK de mesas.comandaActivaId
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mesas_comandaActivaId` ON `mesas` (`comandaActivaId`)")
        db.execSQL(sqlProductos)
        db.execSQL(sqlPedidos)
        db.execSQL(sqlLineas)
        if (version >= 5) {
            db.execSQL("ALTER TABLE `mesas` ADD COLUMN `posX` REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `mesas` ADD COLUMN `posY` REAL NOT NULL DEFAULT 0")
        }
        if (version >= 6) {
            db.execSQL("ALTER TABLE `mesas` ADD COLUMN `girada` INTEGER NOT NULL DEFAULT 0")
        }
        if (rota) {
            // v7 "rota": la dejaba la migración antigua — índices con prefijo idx_ y sin FKs
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_mesas_comandaActivaId ON mesas(comandaActivaId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_pedidos_mesaId ON pedidos(mesaId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_pedido_pedidoId ON lineas_pedido(pedidoId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lineas_pedido_productoId ON lineas_pedido(productoId)")
        }
        // Datos (los mismos en todos los tests)
        db.execSQL("INSERT INTO `mesas` (`id`, `numero`, `alias`, `forma`, `zona`, `capacidad`, `estado`, `comandaActivaId`) VALUES (1, 1, NULL, 'CUADRADA', '', 4, 'LIBRE', NULL)")
        db.execSQL("INSERT INTO `mesas` (`id`, `numero`, `alias`, `forma`, `zona`, `capacidad`, `estado`, `comandaActivaId`) VALUES (2, 2, 'Terraza', 'RECTANGULAR', 'Bar', 8, 'OCUPADA', 10)")
        db.execSQL("INSERT INTO `productos` (`id`, `nombre`, `categoria`, `precio`, `disponible`) VALUES (1, 'Cafe con leche', 'Bebidas', 2.5, 1)")
        db.execSQL("INSERT INTO `pedidos` (`id`, `mesaId`, `estado`, `creadoEn`, `cerradoEn`) VALUES (10, 2, 'ABIERTA', 1723000000000, NULL)")
        db.execSQL("INSERT INTO `lineas_pedido` (`id`, `pedidoId`, `productoId`, `nombreProducto`, `precioUnitario`, `cantidad`, `creadoEn`, `estado`) VALUES (100, 10, 1, 'Cafe con leche', 2.5, 2, 1723000000000, 'PENDIENTE')")
        db.close()
    }

    /** Abre la BD con Room (mismo flujo que producción): migra, valida y devuelve la app. */
    private fun abrirConRoom(nombre: String, vararg migraciones: Migration): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, nombre)
            .addMigrations(*migraciones)
            .build()
            .also { it.openHelper.writableDatabase }

    private fun AppDatabase.verificarDatosYEsquema() {
        val db = openHelper.writableDatabase
        // Datos conservados tras la migración
        assertEquals("Debe conservarse el nº de mesas", 2L, db.query("SELECT COUNT(*) FROM `mesas`").use { it.moveToFirst(); it.getLong(0) })
        assertEquals("Debe conservarse el alias de la mesa", "Terraza",
            db.query("SELECT `alias` FROM `mesas` WHERE `id` = 2").use { it.moveToFirst(); it.getString(0) })
        assertEquals("Debe conservarse la comanda activa", 10L,
            db.query("SELECT `comandaActivaId` FROM `mesas` WHERE `id` = 2").use { it.moveToFirst(); it.getLong(0) })
        assertEquals(1L, db.query("SELECT COUNT(*) FROM `productos`").use { it.moveToFirst(); it.getLong(0) })
        assertEquals(1L, db.query("SELECT COUNT(*) FROM `pedidos`").use { it.moveToFirst(); it.getLong(0) })
        assertEquals("Debe conservarse la línea de pedido", 1L, db.query("SELECT COUNT(*) FROM `lineas_pedido`").use { it.moveToFirst(); it.getLong(0) })
        // FKs presentes tras la migración
        assertTrue("pedidos debe tener FK", db.query("PRAGMA foreign_key_list(`pedidos`)").use { it.count > 0 })
        assertTrue("lineas_pedido debe tener 2 FKs", db.query("PRAGMA foreign_key_list(`lineas_pedido`)").use { it.count == 2 })
    }

    @Test
    fun migrarV4aV8_conservaDatos() {
        crearBD("migracion-v4.db", 4)
        abrirConRoom("migracion-v4.db",
            AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8
        ).apply {
            verificarDatosYEsquema()
            close()
        }
    }

    @Test
    fun migrarV6aV8_conservaDatos() {
        // Caso real: los usuarios del release 1.2 tenían la BD en v6
        crearBD("migracion-v6.db", 6)
        abrirConRoom("migracion-v6.db",
            AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8
        ).apply {
            verificarDatosYEsquema()
            close()
        }
    }

    @Test
    fun migrarV7RotaA_v8_reparaEsquema() {
        // BD v7 dejada por la migración antigua (índices idx_* y sin FKs en pedidos/líneas)
        crearBD("migracion-v7rota.db", 7, rota = true)
        abrirConRoom("migracion-v7rota.db", AppDatabase.MIGRATION_7_8).apply {
            verificarDatosYEsquema()
            close()
        }
    }

    @Test
    fun migrarV7LimpiaA_v8_conservaDatos() {
        // BD v7 creada directamente por Room (esquema correcto con FKs e índices)
        crearBD("migracion-v7limpia.db", 6)
        // Reconstruir pedidos y líneas con sus FKs/índices (esquema canónico v7)
        SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath("migracion-v7limpia.db"), null).apply {
            execSQL("ALTER TABLE `pedidos` RENAME TO `pedidos_old`")
            execSQL("CREATE TABLE IF NOT EXISTS `pedidos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mesaId` INTEGER NOT NULL, `estado` TEXT NOT NULL, `creadoEn` INTEGER NOT NULL, `cerradoEn` INTEGER, FOREIGN KEY(`mesaId`) REFERENCES `mesas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            execSQL("INSERT INTO `pedidos` (`id`, `mesaId`, `estado`, `creadoEn`, `cerradoEn`) SELECT `id`, `mesaId`, `estado`, `creadoEn`, `cerradoEn` FROM `pedidos_old`")
            execSQL("DROP TABLE `pedidos_old`")
            execSQL("CREATE INDEX IF NOT EXISTS `index_pedidos_mesaId` ON `pedidos` (`mesaId`)")
            execSQL("ALTER TABLE `lineas_pedido` RENAME TO `lineas_pedido_old`")
            execSQL("CREATE TABLE IF NOT EXISTS `lineas_pedido` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pedidoId` INTEGER NOT NULL, `productoId` INTEGER NOT NULL, `nombreProducto` TEXT NOT NULL, `precioUnitario` REAL NOT NULL, `cantidad` INTEGER NOT NULL, `creadoEn` INTEGER NOT NULL, `estado` TEXT NOT NULL, FOREIGN KEY(`pedidoId`) REFERENCES `pedidos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`productoId`) REFERENCES `productos`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
            execSQL("INSERT INTO `lineas_pedido` (`id`, `pedidoId`, `productoId`, `nombreProducto`, `precioUnitario`, `cantidad`, `creadoEn`, `estado`) SELECT `id`, `pedidoId`, `productoId`, `nombreProducto`, `precioUnitario`, `cantidad`, `creadoEn`, `estado` FROM `lineas_pedido_old`")
            execSQL("DROP TABLE `lineas_pedido_old`")
            execSQL("CREATE INDEX IF NOT EXISTS `index_lineas_pedido_pedidoId` ON `lineas_pedido` (`pedidoId`)")
            execSQL("CREATE INDEX IF NOT EXISTS `index_lineas_pedido_productoId` ON `lineas_pedido` (`productoId`)")
            version = 7
            close()
        }
        abrirConRoom("migracion-v7limpia.db", AppDatabase.MIGRATION_7_8).apply {
            verificarDatosYEsquema()
            close()
        }
    }
}
