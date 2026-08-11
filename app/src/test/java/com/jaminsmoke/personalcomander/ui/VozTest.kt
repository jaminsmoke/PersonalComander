package com.jaminsmoke.personalcomander.ui

import com.jaminsmoke.personalcomander.data.Producto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VozTest {

    private val productos = listOf(
        Producto(id = 1, nombre = "Café con leche", categoria = "Cafetería", precio = 1.8),
        Producto(id = 2, nombre = "Tarta de queso", categoria = "Postres", precio = 3.5),
        Producto(id = 3, nombre = "Coca-Cola", categoria = "Bebidas", precio = 2.0),
        Producto(id = 4, nombre = "Agua", categoria = "Bebidas", precio = 1.5)
    )

    private fun prod(nombre: String): Producto = productos.first { it.nombre == nombre }

    @Test
    fun normalizar_minusculas_sin_tildes() {
        assertEquals("cafe con leche", normalizar("Café Con Léche"))
        assertEquals("coca cola", normalizar("Coca-Cola"))
    }

    @Test
    fun parsear_cantidad_en_palabras() {
        val r = parsearComanda("dos cafés con leche", productos)
        assertEquals(listOf(LineaVoz(prod("Café con leche"), 2)), r.lineas)
        assertTrue(r.noEntendido.isEmpty())
    }

    @Test
    fun parsear_varias_lineas() {
        val r = parsearComanda("dos cafés con leche y una tarta de queso", productos)
        assertEquals(
            listOf(
                LineaVoz(prod("Café con leche"), 2),
                LineaVoz(prod("Tarta de queso"), 1)
            ),
            r.lineas
        )
        assertTrue(r.noEntendido.isEmpty())
    }

    @Test
    fun parsear_cantidad_en_digitos() {
        val r = parsearComanda("2 cocacola 1 agua", productos)
        assertEquals(
            listOf(
                LineaVoz(prod("Coca-Cola"), 2),
                LineaVoz(prod("Agua"), 1)
            ),
            r.lineas
        )
    }

    @Test
    fun parsear_palabras_de_relleno() {
        val r = parsearComanda("quiero un cafe con leche por favor", productos)
        assertEquals(listOf(LineaVoz(prod("Café con leche"), 1)), r.lineas)
    }

    @Test
    fun parsear_erratas_de_voz_plurales() {
        val r = parsearComanda("2 cocacolas", productos)
        assertEquals(listOf(LineaVoz(prod("Coca-Cola"), 2)), r.lineas)
    }

    @Test
    fun parsear_desconocidos_a_noEntendido() {
        val r = parsearComanda("una pizza", productos)
        assertTrue(r.lineas.isEmpty())
        assertEquals(listOf("pizza"), r.noEntendido)
    }

    @Test
    fun parsear_sin_cantidad_por_defecto_uno() {
        val r = parsearComanda("agua", productos)
        assertEquals(listOf(LineaVoz(prod("Agua"), 1)), r.lineas)
    }

    @Test
    fun parsear_texto_vacio_no_anade_nada() {
        val r = parsearComanda("   ", productos)
        assertTrue(r.lineas.isEmpty())
        assertTrue(r.noEntendido.isEmpty())
    }

    @Test
    fun buscar_ranking_prefijo_exacto() {
        val q = "caf"
        val scored = productos.mapNotNull { p ->
            coincidenciaBusqueda(q, p)?.let { it to p }
        }
        val mejor = scored.minByOrNull { it.first }!!.second
        assertEquals(prod("Café con leche"), mejor)
    }

    @Test
    fun buscar_sin_coincidencia_devuelve_null() {
        assertNull(coincidenciaBusqueda("zzzzzz", productos[0]))
    }

    @Test
    fun buscar_coincidencia_en_categoria() {
        val scored = productos.mapNotNull { p ->
            coincidenciaBusqueda("postre", p)?.let { it to p }
        }
        assertEquals(prod("Tarta de queso"), scored.minByOrNull { it.first }!!.second)
    }

    @Test
    fun mensaje_error_voz_no_vacio() {
        assertTrue(mensajeErrorVoz(null, 7).isNotBlank())
    }
}
