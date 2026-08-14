package com.jaminsmoke.personalcomander.data.sesion

import com.jaminsmoke.personalcomander.data.Producto
import org.junit.Assert.assertEquals
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
            ProductoLan(id = "cana", nombre = "Caña", categoria = "Bebida", precio = 3.0),
        )
        val plan = CartaSync.plan(locales, remotos)
        assertTrue(plan.insertar.isEmpty())
        assertEquals(1, plan.actualizar.size)
        assertEquals(9L, plan.actualizar[0].id)
        assertEquals("Caña", plan.actualizar[0].nombre)
        assertEquals("cana", plan.actualizar[0].codigoBar)
        assertEquals("Seed local", locales[1].nombre)
    }
}
