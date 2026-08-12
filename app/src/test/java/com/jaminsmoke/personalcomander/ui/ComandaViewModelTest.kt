package com.jaminsmoke.personalcomander.ui

import com.jaminsmoke.personalcomander.data.LineaPedido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComandaViewModelTest {

    private fun lp(nombre: String, cantidad: Int, id: Long = 1, prodId: Long = 1) =
        LineaPedido(
            id = id, pedidoId = 1, productoId = prodId,
            nombreProducto = nombre, precioUnitario = 1.0, cantidad = cantidad
        )

    // ── resolverQuitar ──

    @Test
    fun resolver_quitar_reduce_cantidad() {
        val lineas = listOf(lp("Café con leche", 5))
        val quitadas = listOf(LineaQuitar("Café con leche", 2))

        val cambios = resolverQuitar(quitadas, lineas)

        assertEquals(1, cambios.size)
        assertEquals(3, cambios[0].second) // 5 - 2 = 3
    }

    @Test
    fun resolver_quitar_elimina_si_cantidad_igual() {
        val lineas = listOf(lp("Café con leche", 2))
        val quitadas = listOf(LineaQuitar("Café con leche", 2))

        val cambios = resolverQuitar(quitadas, lineas)

        assertEquals(1, cambios.size)
        assertEquals(null, cambios[0].second)
    }

    @Test
    fun resolver_quitar_elimina_si_pide_mas_de_lo_que_hay() {
        val lineas = listOf(lp("Café con leche", 3))
        val quitadas = listOf(LineaQuitar("Café con leche", 10))

        val cambios = resolverQuitar(quitadas, lineas)

        assertEquals(1, cambios.size)
        assertEquals(null, cambios[0].second)
    }

    @Test
    fun resolver_quitar_varias_lineas_distintas() {
        val lineas = listOf(
            lp("Café con leche", 3, id = 1, prodId = 1),
            lp("Agua", 5, id = 2, prodId = 4),
            lp("Tarta de queso", 1, id = 3, prodId = 2)
        )
        val quitadas = listOf(
            LineaQuitar("Café con leche", 1),
            LineaQuitar("Agua", 3)
        )

        val cambios = resolverQuitar(quitadas, lineas)

        assertEquals(2, cambios.size)
        val cafe = cambios.first { it.first.nombreProducto == "Café con leche" }
        assertEquals(2, cafe.second) // 3 - 1
        val agua = cambios.first { it.first.nombreProducto == "Agua" }
        assertEquals(2, agua.second) // 5 - 3
    }

    @Test
    fun resolver_quitar_producto_no_encontrado_ignora() {
        val lineas = listOf(lp("Agua", 3))
        val quitadas = listOf(LineaQuitar("Pizza", 1))

        val cambios = resolverQuitar(quitadas, lineas)

        assertTrue(cambios.isEmpty())
    }

    @Test
    fun resolver_quitar_vacio_no_hace_nada() {
        val lineas = listOf(lp("Agua", 3))
        val cambios = resolverQuitar(emptyList(), lineas)
        assertTrue(cambios.isEmpty())
    }

    @Test
    fun resolver_quitar_normaliza_nombres() {
        val lineas = listOf(lp("Café con leche", 3))
        val quitadas = listOf(LineaQuitar("cafe con leche", 1))

        val cambios = resolverQuitar(quitadas, lineas)

        assertEquals(1, cambios.size)
        assertEquals(2, cambios[0].second)
    }

    @Test
    fun resolver_quitar_misma_linea_repetida_en_quitadas() {
        val lineas = listOf(lp("Agua", 10))
        val quitadas = listOf(LineaQuitar("Agua", 3), LineaQuitar("Agua", 4))

        val cambios = resolverQuitar(quitadas, lineas)

        assertEquals(2, cambios.size)
        assertEquals(7, cambios[0].second) // 10 - 3 = 7
        assertEquals(3, cambios[1].second) // 7 - 4 = 3
    }
}
