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
 *  v8 -> v9: +indiceZona         (IDs B1/T2 por zona)
 *  v9 -> v10: bloqueada + reservas (hold de sala)
 *  v10 -> v11: tabla salas + mesas.salaId (deja de existir zona)
 *  v11 -> v12: ticketId en lineas_pedido (SSE recoger)
 *  v12 -> v13: productos.codigoBar (espejo GET /v1/carta)
 *  v13 -> v14: salas.codigoBar + mesas.codigoBar (espejo GET /v1/estado)
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

    /** Convierte pedidos/líneas al esquema canónico con FKs + índices (v7+). */
    private fun SQLiteDatabase.promoverConFKs() {
        execSQL("ALTER TABLE `pedidos` RENAME TO `pedidos_old`")
        execSQL("CREATE TABLE IF NOT EXISTS `pedidos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mesaId` INTEGER NOT NULL, `estado` TEXT NOT NULL, `creadoEn` INTEGER NOT NULL, `cerradoEn` INTEGER, FOREIGN KEY(`mesaId`) REFERENCES `mesas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        execSQL("INSERT INTO `pedidos` (`id`, `mesaId`, `estado`, `creadoEn`, `cerradoEn`) SELECT `id`, `mesaId`, `estado`, `creadoEn`, `cerradoEn` FROM `pedidos_old`")
        execSQL("DROP TABLE `pedidos_old`")
        execSQL("CREATE INDEX IF NOT EXISTS `index_pedidos_mesaId` ON `pedidos` (`mesaId`)")
        execSQL("CREATE INDEX IF NOT EXISTS `index_pedidos_creadoEn` ON `pedidos` (`creadoEn`)")
        execSQL("ALTER TABLE `lineas_pedido` RENAME TO `lineas_pedido_old`")
        execSQL("CREATE TABLE IF NOT EXISTS `lineas_pedido` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pedidoId` INTEGER NOT NULL, `productoId` INTEGER NOT NULL, `nombreProducto` TEXT NOT NULL, `precioUnitario` REAL NOT NULL, `cantidad` INTEGER NOT NULL, `creadoEn` INTEGER NOT NULL, `estado` TEXT NOT NULL, FOREIGN KEY(`pedidoId`) REFERENCES `pedidos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`productoId`) REFERENCES `productos`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        execSQL("INSERT INTO `lineas_pedido` (`id`, `pedidoId`, `productoId`, `nombreProducto`, `precioUnitario`, `cantidad`, `creadoEn`, `estado`) SELECT `id`, `pedidoId`, `productoId`, `nombreProducto`, `precioUnitario`, `cantidad`, `creadoEn`, `estado` FROM `lineas_pedido_old`")
        execSQL("DROP TABLE `lineas_pedido_old`")
        execSQL("CREATE INDEX IF NOT EXISTS `index_lineas_pedido_pedidoId` ON `lineas_pedido` (`pedidoId`)")
        execSQL("CREATE INDEX IF NOT EXISTS `index_lineas_pedido_productoId` ON `lineas_pedido` (`productoId`)")
    }

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
        // v9: columna indiceZona presente
        val tieneIndiceZona = db.query("PRAGMA table_info(`mesas`)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) if (cursor.getString(1) == "indiceZona") found = true
            found
        }
        assertTrue("debe existir columna indiceZona (v9)", tieneIndiceZona)
        val tieneBloqueada = db.query("PRAGMA table_info(`mesas`)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) if (cursor.getString(1) == "bloqueada") found = true
            found
        }
        assertTrue("debe existir columna bloqueada (v10)", tieneBloqueada)
        assertEquals(
            "tabla reservas debe existir (v10)",
            0L,
            db.query("SELECT COUNT(*) FROM `reservas`").use { it.moveToFirst(); it.getLong(0) }
        )
        val tieneSalaId = db.query("PRAGMA table_info(`mesas`)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) if (cursor.getString(1) == "salaId") found = true
            found
        }
        assertTrue("debe existir columna salaId (v11)", tieneSalaId)
        val noTieneZona = db.query("PRAGMA table_info(`mesas`)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) if (cursor.getString(1) == "zona") found = true
            !found
        }
        assertTrue("no debe existir columna zona (v11)", noTieneZona)
        assertEquals(
            "tabla salas debe existir (v11)",
            2L,
            db.query("SELECT COUNT(*) FROM `salas`").use { it.moveToFirst(); it.getLong(0) }
        )
        val tieneTicketId = db.query("PRAGMA table_info(`lineas_pedido`)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) if (cursor.getString(1) == "ticketId") found = true
            found
        }
        assertTrue("debe existir columna ticketId (v12)", tieneTicketId)
        val tieneCodigoBar = db.query("PRAGMA table_info(`productos`)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) if (cursor.getString(1) == "codigoBar") found = true
            found
        }
        assertTrue("debe existir columna codigoBar (v13)", tieneCodigoBar)
        val tieneSalaCodigoBar = db.query("PRAGMA table_info(`salas`)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) if (cursor.getString(1) == "codigoBar") found = true
            found
        }
        assertTrue("debe existir columna salas.codigoBar (v14)", tieneSalaCodigoBar)
        val tieneMesaCodigoBar = db.query("PRAGMA table_info(`mesas`)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) if (cursor.getString(1) == "codigoBar") found = true
            found
        }
        assertTrue("debe existir columna mesas.codigoBar (v14)", tieneMesaCodigoBar)
    }

    private val migracionesHastaV12 = arrayOf(
        AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6,
        AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8,
        AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
        AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14,
    )

    @Test
    fun migrarV4aV12_conservaDatos() {
        crearBD("migracion-v4.db", 4)
        abrirConRoom("migracion-v4.db", *migracionesHastaV12).apply {
            verificarDatosYEsquema()
            close()
        }
    }

    @Test
    fun migrarV6aV11_conservaDatos() {
        // Caso real: los usuarios del release 1.2 tenían la BD en v6
        crearBD("migracion-v6.db", 6)
        abrirConRoom(
            "migracion-v6.db",
            AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14
        ).apply {
            verificarDatosYEsquema()
            close()
        }
    }

    @Test
    fun migrarV7RotaA_v11_reparaEsquema() {
        // BD v7 dejada por la migración antigua (índices idx_* y sin FKs en pedidos/líneas)
        crearBD("migracion-v7rota.db", 7, rota = true)
        abrirConRoom(
            "migracion-v7rota.db",
            AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14
        ).apply {
            verificarDatosYEsquema()
            close()
        }
    }

    @Test
    fun migrarV7LimpiaA_v11_conservaDatos() {
        // BD v7 creada directamente por Room (esquema correcto con FKs e índices)
        crearBD("migracion-v7limpia.db", 6)
        SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath("migracion-v7limpia.db"), null).apply {
            promoverConFKs()
            version = 7
            close()
        }
        abrirConRoom(
            "migracion-v7limpia.db",
            AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14
        ).apply {
            verificarDatosYEsquema()
            close()
        }
    }

    @Test
    fun migrarV8aV11_anadeIndiceZonaYSala() {
        // BD canónica v8 (con FKs e índices pero sin indiceZona)
        crearBD("migracion-v8.db", 6)
        SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath("migracion-v8.db"), null).apply {
            promoverConFKs()
            version = 8
            close()
        }
        abrirConRoom(
            "migracion-v8.db",
            AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14
        ).apply {
            verificarDatosYEsquema()
            val db = openHelper.writableDatabase
            // indiceZona relleno por zona según id (ambas mesas son de zonas distintas → índice 1)
            assertEquals("mesa 1 (zona vacía) debe tener indiceZona 1", 1L,
                db.query("SELECT `indiceZona` FROM `mesas` WHERE `id` = 1").use { it.moveToFirst(); it.getLong(0) })
            assertEquals("mesa 2 (zona Bar) debe tener indiceZona 1", 1L,
                db.query("SELECT `indiceZona` FROM `mesas` WHERE `id` = 2").use { it.moveToFirst(); it.getLong(0) })
            close()
        }
    }

    @Test
    fun migrarV9aV11_anadeBloqueadaYSalas() {
        crearBD("migracion-v9.db", 6)
        SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath("migracion-v9.db"), null).apply {
            promoverConFKs()
            execSQL("ALTER TABLE mesas ADD COLUMN indiceZona INTEGER NOT NULL DEFAULT 0")
            execSQL(
                """UPDATE mesas SET indiceZona = (
                    SELECT COUNT(*) FROM mesas m2
                    WHERE m2.zona = mesas.zona AND m2.id <= mesas.id
                )""".trimIndent()
            )
            version = 9
            close()
        }
        abrirConRoom("migracion-v9.db", AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14).apply {
            verificarDatosYEsquema()
            val db = openHelper.writableDatabase
            assertEquals(
                0L,
                db.query("SELECT `bloqueada` FROM `mesas` WHERE `id` = 1").use { it.moveToFirst(); it.getLong(0) }
            )
            close()
        }
    }
}
