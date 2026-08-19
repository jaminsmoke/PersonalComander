package com.jaminsmoke.personalcomander.data.sesion

import com.jaminsmoke.personalcomander.data.Producto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartaSyncTest {

    @Test
    fun parse_wrapper_productos() {
        val carta = CartaSync.parse(
            """{"productos":[{"id":"cana","nombre":"Caña","categoria":"Bebida","precio":2.5,"disponible":true}]}""",
        )!!
        assertEquals(1, carta.productos.size)
        assertEquals("cana", carta.productos[0].id)
        assertEquals("Caña", carta.productos[0].nombre)
        assertEquals("Bebida", carta.productos[0].categoria)
        assertEquals(2.5, carta.productos[0].precio, 0.0)
        assertTrue(carta.productos[0].disponible)
    }

    @Test
    fun parse_propaga_subfamilia_grupos_y_modificadores() {
        val carta = CartaSync.parse(
            """
            {"productos":[{"id":"burger","nombre":"Burger","categoria":"Burgers","precio":8,
              "subfamilia":"Clásicas","permiteNota":true,"grupos":["punto"]}],
             "gruposModificador":[{"id":"punto","nombre":"Punto","obligatorio":true,
               "opciones":[{"id":"al-punto","nombre":"Al punto","alias":"punto"}]}]}
            """.trimIndent(),
        )!!
        assertEquals("Clásicas", carta.productos[0].subfamilia)
        assertTrue(carta.productos[0].permiteNota)
        assertEquals(listOf("punto"), carta.productos[0].grupos)
        assertEquals("Punto", carta.gruposModificador.single().nombre)
        assertEquals("Al punto", carta.gruposModificador.single().opciones.single().nombre)
    }

    @Test
    fun parse_ignora_sin_id_o_nombre() {
        val carta = CartaSync.parse(
            """{"productos":[{"id":"","nombre":"X"},{"id":"cana","nombre":""},{"id":"cana","nombre":"Caña"}]}""",
        )!!
        assertEquals(listOf("cana"), carta.productos.map { it.id })
    }

    @Test
    fun parse_invalido_devuelve_null() {
        assertNull(CartaSync.parse("{"))
    }

    @Test
    fun plan_inserta_nuevos_y_no_borra_locales() {
        val locales = listOf(
            Producto(id = 1, nombre = "Cerveza caña", categoria = "Bebidas", precio = 2.0),
        )
        val remotos = listOf(
            ProductoLan(id = "cana", nombre = "Caña", categoria = "Bebida", precio = 2.5),
            ProductoLan(id = "croquetas", nombre = "Croquetas", categoria = "Comida", precio = 6.0, disponible = false),
        )
        val plan = CartaSync.plan(locales, remotos)
        assertEquals(2, plan.insertar.size)
        assertTrue(plan.actualizar.isEmpty())
        assertEquals("cana", plan.insertar[0].codigoBar)
        assertEquals("Caña", plan.insertar[0].nombre)
        assertEquals(false, plan.insertar[1].disponible)
    }

    @Test
    fun plan_actualiza_por_codigoBar() {
        val locales = listOf(
            Producto(id = 9, nombre = "Viejo", categoria = "X", precio = 1.0, codigoBar = "cana"),
            Producto(id = 1, nombre = "Seed local", categoria = "Bebidas", precio = 2.0),
        )
        val remotos = listOf(
            ProductoLan(id = "cana", nombre = "Caña", categoria = "Bebida", precio = 3.0, subfamilia = "Cerveza", permiteNota = true),
        )
        val plan = CartaSync.plan(locales, remotos)
        assertTrue(plan.insertar.isEmpty())
        assertEquals(1, plan.actualizar.size)
        assertEquals(9L, plan.actualizar[0].id)
        assertEquals("Caña", plan.actualizar[0].nombre)
        assertEquals("cana", plan.actualizar[0].codigoBar)
        assertEquals("Cerveza", plan.actualizar[0].subfamilia)
        assertTrue(plan.actualizar[0].permiteNota)
        assertEquals("Seed local", locales[1].nombre)
    }

    @Test
    fun parse_propaga_schema() {
        val carta = CartaSync.parse("""{"schema":2,"productos":[]}""")!!
        assertEquals(2, carta.schema)
    }

    @Test
    fun parse_sin_schema_es_cero() {
        val carta = CartaSync.parse("""{"productos":[]}""")!!
        assertEquals(0, carta.schema)
    }

    @Test
    fun debeReconstruir_si_schema_cambia() {
        assertTrue(
            CartaSync.debeReconstruir(
                schemaRemoto = 2,
                schemaGuardado = 0,
                existentes = emptyList(),
                remotos = emptyList(),
            ),
        )
    }

    @Test
    fun debeReconstruir_si_remotos_son_uuid_y_locales_slug() {
        val locales = listOf(
            Producto(id = 9, nombre = "Caña", categoria = "Bebida", precio = 2.0, codigoBar = "cana"),
        )
        val remotos = listOf(
            ProductoLan(id = "550e8400-e29b-41d4-a716-446655440000", nombre = "Caña", categoria = "Bebida", precio = 2.5),
        )
        assertTrue(
            CartaSync.debeReconstruir(
                schemaRemoto = 0,
                schemaGuardado = 0,
                existentes = locales,
                remotos = remotos,
            ),
        )
    }

    @Test
    fun debeReconstruir_falso_si_ya_estan_en_uuid() {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        val locales = listOf(
            Producto(id = 9, nombre = "Caña", categoria = "Bebida", precio = 2.0, codigoBar = uuid),
        )
        val remotos = listOf(
            ProductoLan(id = uuid, nombre = "Caña", categoria = "Bebida", precio = 2.5),
        )
        assertFalse(
            CartaSync.debeReconstruir(
                schemaRemoto = 2,
                schemaGuardado = 2,
                existentes = locales,
                remotos = remotos,
            ),
        )
    }

    @Test
    fun planReconstruccion_reapunta_por_nombre_y_no_toca_locales() {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        val locales = listOf(
            Producto(id = 9, nombre = "Caña", categoria = "Bebida", precio = 2.0, codigoBar = "cana"),
            Producto(id = 1, nombre = "Seed local", categoria = "Bebidas", precio = 1.5),
        )
        val remotos = listOf(
            ProductoLan(id = uuid, nombre = "Caña", categoria = "Bebida", precio = 2.5),
            ProductoLan(id = "7f4c2c2e-0000-4000-8000-000000000000", nombre = "Croquetas", categoria = "Comida", precio = 6.0),
        )
        val plan = CartaSync.planReconstruccion(locales, remotos)
        assertEquals(1, plan.actualizar.size)
        assertEquals(9L, plan.actualizar[0].id)          // conserva el id Long local
        assertEquals(uuid, plan.actualizar[0].codigoBar) // re-apunta al nuevo id
        assertEquals(1, plan.insertar.size)
        assertEquals("Croquetas", plan.insertar[0].nombre)
        assertEquals("Seed local", locales[1].nombre)    // local sin codigoBar intacto
    }
}
