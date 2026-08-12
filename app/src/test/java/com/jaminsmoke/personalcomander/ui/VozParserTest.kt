package com.jaminsmoke.personalcomander.ui

import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.Producto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VozParserTest {

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

    // ── Números expandidos 1-99 ──

    @Test
    fun parsear_numero_diecisiete() {
        val r = parsearComanda("diecisiete aguas", productos)
        assertEquals(listOf(LineaVoz(prod("Agua"), 17)), r.lineas)
    }

    @Test
    fun parsear_numero_veintitres() {
        val r = parsearComanda("veintitres aguas", productos)
        assertEquals(listOf(LineaVoz(prod("Agua"), 23)), r.lineas)
    }

    @Test
    fun parsear_numero_veintinueve() {
        val r = parsearComanda("veintinueve aguas", productos)
        assertEquals(listOf(LineaVoz(prod("Agua"), 29)), r.lineas)
    }

    @Test
    fun parsear_numero_compuesto_treinta_y_cinco() {
        val r = parsearComanda("treinta y cinco aguas", productos)
        assertEquals(listOf(LineaVoz(prod("Agua"), 35)), r.lineas)
    }

    @Test
    fun parsear_numero_compuesto_cuarenta_y_dos() {
        val r = parsearComanda("cuarenta y dos cafes con leche", productos)
        assertEquals(listOf(LineaVoz(prod("Café con leche"), 42)), r.lineas)
    }

    @Test
    fun parsear_numero_compuesto_setenta_y_ocho() {
        val r = parsearComanda("setenta y ocho aguas", productos)
        assertEquals(listOf(LineaVoz(prod("Agua"), 78)), r.lineas)
    }

    @Test
    fun parsear_numero_compuesto_noventa_y_nueve() {
        val r = parsearComanda("noventa y nueve aguas", productos)
        assertEquals(listOf(LineaVoz(prod("Agua"), 99)), r.lineas)
    }

    @Test
    fun parsear_numero_decena_suelta() {
        val r = parsearComanda("cincuenta aguas", productos)
        assertEquals(listOf(LineaVoz(prod("Agua"), 50)), r.lineas)
    }

    @Test
    fun parsear_numero_cien() {
        val r = parsearComanda("cien aguas", productos)
        assertEquals(listOf(LineaVoz(prod("Agua"), 100)), r.lineas)
    }

    @Test
    fun parsear_numero_ciento() {
        val r = parsearComanda("ciento aguas", productos)
        assertEquals(listOf(LineaVoz(prod("Agua"), 100)), r.lineas)
    }

    // ── Palabras de relleno ampliadas ──

    @Test
    fun parsear_relleno_vale_gracias() {
        val r = parsearComanda("vale un cafe con leche gracias", productos)
        assertEquals(listOf(LineaVoz(prod("Café con leche"), 1)), r.lineas)
    }

    @Test
    fun parsear_relleno_dame_anade() {
        val r = parsearComanda("dame tres aguas y anade una tarta de queso", productos)
        assertEquals(
            listOf(LineaVoz(prod("Agua"), 3), LineaVoz(prod("Tarta de queso"), 1)),
            r.lineas
        )
    }

    @Test
    fun parsear_relleno_apunta() {
        val r = parsearComanda("apunta dos cafes con leche porfa", productos)
        assertEquals(listOf(LineaVoz(prod("Café con leche"), 2)), r.lineas)
    }

    @Test
    fun parsear_relleno_con_sin() {
        val r = parsearComanda("un cafe con leche con dos aguas", productos)
        assertEquals(
            listOf(LineaVoz(prod("Café con leche"), 1), LineaVoz(prod("Agua"), 2)),
            r.lineas
        )
    }

    // ── Keyword extraction ──

    @Test
    fun extraer_accion_anade() {
        val a = extraerAccion("añade dos cafés con leche")
        assertTrue(a is AccionVoz.Anadir)
        assertEquals("anade dos cafes con leche", (a as AccionVoz.Anadir).texto)
    }

    @Test
    fun extraer_accion_quita() {
        val a = extraerAccion("quita dos cafés con leche")
        assertTrue(a is AccionVoz.Quitar)
        assertEquals("dos cafes con leche", (a as AccionVoz.Quitar).texto)
    }

    @Test
    fun extraer_accion_borra() {
        val a = extraerAccion("borra una tarta de queso")
        assertTrue(a is AccionVoz.Quitar)
        assertEquals("una tarta de queso", (a as AccionVoz.Quitar).texto)
    }

    @Test
    fun extraer_accion_elimina() {
        val a = extraerAccion("elimina tres aguas")
        assertTrue(a is AccionVoz.Quitar)
        assertEquals("tres aguas", (a as AccionVoz.Quitar).texto)
    }

    @Test
    fun extraer_accion_comanda() {
        val a = extraerAccion("comanda tres aguas")
        assertTrue(a is AccionVoz.Anadir)
        assertEquals("comanda tres aguas", (a as AccionVoz.Anadir).texto)
    }

    @Test
    fun extraer_accion_sin_keyword_es_anadir() {
        val a = extraerAccion("dos cafés con leche")
        assertTrue(a is AccionVoz.Anadir)
        assertEquals("dos cafes con leche", (a as AccionVoz.Anadir).texto)
    }

    @Test
    fun extraer_accion_solo_keyword_quitar() {
        val a = extraerAccion("quita")
        assertTrue(a is AccionVoz.Quitar)
        assertEquals("", (a as AccionVoz.Quitar).texto)
    }

    @Test
    fun extraer_accion_keyword_con_ruido_anadir() {
        val a = extraerAccion("añade dos cafes con leche porfa")
        assertTrue(a is AccionVoz.Anadir)
        val r = parsearComanda((a as AccionVoz.Anadir).texto, productos)
        assertEquals(listOf(LineaVoz(prod("Café con leche"), 2)), r.lineas)
    }

    @Test
    fun extraer_accion_retira() {
        val a = extraerAccion("retira un cafe con leche")
        assertTrue(a is AccionVoz.Quitar)
        assertEquals("un cafe con leche", (a as AccionVoz.Quitar).texto)
    }

    // ── parsearQuitar ──

    @Test
    fun parsear_quitar_exacto() {
        val lineas = listOf(
            LineaPedido(pedidoId = 1, productoId = 1, nombreProducto = "Café con leche", precioUnitario = 1.8, cantidad = 2),
            LineaPedido(pedidoId = 1, productoId = 4, nombreProducto = "Agua", precioUnitario = 1.5, cantidad = 3)
        )
        val r = parsearQuitar("dos cafes con leche", lineas)
        assertEquals(listOf(LineaQuitar("Café con leche", 2)), r.lineas)
        assertTrue(r.noEntendido.isEmpty())
    }

    @Test
    fun parsear_quitar_parcial() {
        val lineas = listOf(
            LineaPedido(pedidoId = 1, productoId = 4, nombreProducto = "Agua", precioUnitario = 1.5, cantidad = 3)
        )
        val r = parsearQuitar("dos cafes", lineas)
        assertTrue(r.lineas.isEmpty())
        assertEquals(listOf("cafes"), r.noEntendido)
    }

    @Test
    fun parsear_quitar_varias() {
        val lineas = listOf(
            LineaPedido(pedidoId = 1, productoId = 1, nombreProducto = "Café con leche", precioUnitario = 1.8, cantidad = 2),
            LineaPedido(pedidoId = 1, productoId = 2, nombreProducto = "Tarta de queso", precioUnitario = 3.5, cantidad = 1)
        )
        val r = parsearQuitar("un cafe con leche y una tarta de queso", lineas)
        assertEquals(
            listOf(LineaQuitar("Café con leche", 1), LineaQuitar("Tarta de queso", 1)),
            r.lineas
        )
    }

    @Test
    fun parsear_quitar_fuzzy_plural() {
        val lineas = listOf(
            LineaPedido(pedidoId = 1, productoId = 1, nombreProducto = "Café con leche", precioUnitario = 1.8, cantidad = 2)
        )
        val r = parsearQuitar("un cafes con leche", lineas)
        assertEquals(listOf(LineaQuitar("Café con leche", 1)), r.lineas)
    }
}
