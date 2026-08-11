package com.jaminsmoke.personalcomander.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.DriverManager

/**
 * Pruebas del motor de importación TPV. Usa sqlite-jdbc (testImplementation) para
 * leer bases SQLite en la JVM, replicando la forma en que la app las lee en Android.
 */
class TpvTest {

    /** Implementación JVM de [FilasProvider] sobre sqlite-jdbc. */
    private class TestFilasProvider(private val archivo: File) : FilasProvider {

        override fun tablas(): List<String> =
            DriverManager.getConnection("jdbc:sqlite:${archivo.path}").use { con ->
                con.createStatement().use { st ->
                    st.executeQuery(
                        "SELECT name FROM sqlite_master WHERE type IN ('table','view') AND name NOT LIKE 'sqlite_%'"
                    ).use { rs ->
                        buildList { while (rs.next()) add(rs.getString(1)) }
                    }
                }
            }

        override fun filasDe(tabla: String, filtro: String?): List<Map<String, Any?>> {
            val sql = buildString {
                append("SELECT * FROM \"$tabla\"")
                if (!filtro.isNullOrBlank()) append(" WHERE $filtro")
            }
            return DriverManager.getConnection("jdbc:sqlite:${archivo.path}").use { con ->
                con.createStatement().use { st ->
                    st.executeQuery(sql).use { rs ->
                        val columnas = (1..rs.metaData.columnCount).map { rs.metaData.getColumnName(it) }
                        buildList {
                            while (rs.next()) {
                                val fila = LinkedHashMap<String, Any?>()
                                for (col in columnas) fila[col] = rs.getObject(col)
                                add(fila)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun archivoAgoraTemporal(): File {
        val archivo = File.createTempFile("agora", ".db")
        archivo.deleteOnExit()
        DriverManager.getConnection("jdbc:sqlite:${archivo.path}").use { con ->
            con.createStatement().use { st ->
                st.executeUpdate(
                    "CREATE TABLE PRODUCTOS (CODPRO INTEGER, DESCRIPCION TEXT, PRECIO REAL, FAMILIA TEXT, ACTIVO INTEGER)"
                )
                st.executeUpdate("CREATE TABLE FAMILIAS (CODFAM INTEGER, DESCRIPCION TEXT)")
                st.executeUpdate("CREATE TABLE USUARIOS (CODUSU INTEGER, NOMBRE TEXT, CLAVE TEXT)")
                st.executeUpdate("INSERT INTO PRODUCTOS VALUES (1, 'Café solo', 1.20, 'CAFES', 1)")
                st.executeUpdate("INSERT INTO PRODUCTOS VALUES (2, 'Tarta de queso', 3.50, 'POSTRES', 1)")
                st.executeUpdate("INSERT INTO PRODUCTOS VALUES (3, 'Plato retirado', 2.00, 'OTROS', 0)")
                st.executeUpdate("INSERT INTO PRODUCTOS VALUES (4, 'Precio nulo', NULL, 'OTROS', 1)")
            }
        }
        return archivo
    }

    /** Genera el fixture de ejemplo en devartifacts/ para inspección manual. */
    @Test
    fun generaFixtureEnDevartifacts() {
        val origen = archivoAgoraTemporal()
        val destino = File(dirDevartifacts(), "agora_sample.db")
        origen.copyTo(destino, overwrite = true)
        assertTrue(destino.exists())
        assertTrue(destino.length() > 0)
    }

    @Test
    fun mapeaProductosAgoraRespetandoElFiltro() {
        val provider = TestFilasProvider(archivoAgoraTemporal())
        val mapeo = TpvPrograma.AGORA.mapeo ?: error("Ágora debe tener mapeo")

        val tablas = provider.tablas()
        assertTrue("PRODUCTOS" in tablas)
        assertTrue("USUARIOS" in tablas)

        val filas = provider.filasDe(mapeo.tabla, mapeo.filtro)
        assertEquals(3, filas.size) // el 'Plato retirado' (ACTIVO=0) queda fuera

        val productos = filas.mapNotNull { mapFilaProducto(it, mapeo) }
        assertEquals(3, productos.size)

        val cafe = productos.first { it.nombre == "Café solo" }
        assertEquals("CAFES", cafe.categoria)
        assertEquals(1.2, cafe.precio, 0.0001)
        assertTrue(cafe.disponible)

        // El precio NULL también es un producto válido (precio 0.0)
        val nulo = productos.first { it.nombre == "Precio nulo" }
        assertEquals(0.0, nulo.precio, 0.0001)
    }

    @Test
    fun mapFilaProductoDevuelveNullSinColumnaDeNombre() {
        val mapeo = TpvPrograma.AGORA.mapeo ?: error("Ágora debe tener mapeo")
        assertNull(mapFilaProducto(mapOf("PRECIO" to 1.0), mapeo))
        assertNull(mapFilaProducto(mapOf("DESCRIPCION" to "   "), mapeo))
    }

    @Test
    fun fusionarProductosEmparejaPorNombreNormalizado() {
        val existentes = listOf(
            Producto(id = 1, nombre = "Cafe Solo", categoria = "BEBIDAS", precio = 1.0, disponible = true)
        )
        val importados = listOf(
            Producto(nombre = "Café solo", categoria = "CAFES", precio = 1.2, disponible = true),
            Producto(nombre = "Tarta de queso", categoria = "POSTRES", precio = 3.5, disponible = true),
            Producto(nombre = "", categoria = "BASURA", precio = 9.9, disponible = true)
        )

        val fusion = fusionarProductos(existentes, importados)

        assertEquals(1, fusion.actualizados)
        assertEquals(1, fusion.insertados)
        assertEquals(1, fusion.ignorados)

        val actualizado = fusion.actualizar.first()
        assertEquals(1L, actualizado.id) // conserva el id existente
        assertEquals(1.2, actualizado.precio, 0.0001)
        assertEquals("CAFES", actualizado.categoria)
        assertEquals("Café solo", actualizado.nombre)

        val nuevo = fusion.insertar.first()
        assertEquals(0L, nuevo.id)
        assertEquals("Tarta de queso", nuevo.nombre)
    }

    @Test
    fun backupJsonRedondea() {
        val originales = listOf(
            Producto(nombre = "Café solo", categoria = "CAFES", precio = 1.2, disponible = true),
            Producto(nombre = "Tarta de queso", categoria = "POSTRES", precio = 3.5, disponible = false)
        )
        val json = BackupJson.serializar(originales)
        val recuperados = BackupJson.deserializar(json)
        assertEquals(originales, recuperados)
    }

    @Test
    fun backupJsonRechazaEntradaInvalida() {
        assertNull(BackupJson.deserializar("esto no es json"))
        assertNull(BackupJson.deserializar(""))
        assertEquals(emptyList<Producto>(), BackupJson.deserializar("{}"))
    }

    private fun dirDevartifacts(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".")
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) {
                return File(dir, "devartifacts").apply { mkdirs() }
            }
            dir = dir.parentFile
        }
        return File("devartifacts").apply { mkdirs() }
    }
}
