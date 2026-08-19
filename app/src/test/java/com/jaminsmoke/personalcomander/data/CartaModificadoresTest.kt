package com.jaminsmoke.personalcomander.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartaModificadoresTest {

    private val punto = GrupoModificador(id = 1, nombre = "Punto", multiple = false, obligatorio = true)
    private val extras = GrupoModificador(id = 2, nombre = "Extras", multiple = true, obligatorio = false)
    private val alPunto = OpcionModificador(id = 11, grupoId = 1, nombre = "Al punto", deltaPrecio = 0.0)
    private val muyHecho = OpcionModificador(id = 12, grupoId = 1, nombre = "Muy hecho")
    private val bacon = OpcionModificador(id = 21, grupoId = 2, nombre = "Bacon", deltaPrecio = 1.5)

    @Test
    fun canonicalJson_ordena_y_es_estable() {
        val a = ModificadorElegido(2, "Extras", 21, "Bacon", 1.5)
        val b = ModificadorElegido(1, "Punto", 11, "Al punto", 0.0)
        assertEquals(
            CartaModificadores.canonicalJson(listOf(a, b)),
            CartaModificadores.canonicalJson(listOf(b, a)),
        )
        assertEquals("[]", CartaModificadores.canonicalJson(emptyList()))
    }

    @Test
    fun parseJson_redondo() {
        val orig = listOf(ModificadorElegido(1, "Punto", 11, "Al punto", 0.0))
        val parsed = CartaModificadores.parseJson(CartaModificadores.canonicalJson(orig))
        assertEquals(orig, parsed)
        assertTrue(CartaModificadores.parseJson(null).isEmpty())
        assertTrue(CartaModificadores.parseJson("no-json").isEmpty())
    }

    @Test
    fun textoLinea_junta_opciones_y_nota() {
        val mods = listOf(
            ModificadorElegido(1, "Punto", 11, "Al punto", 0.0),
            ModificadorElegido(2, "Extras", 21, "Bacon", 1.5),
        )
        assertEquals("Al punto · Bacon · sin cebolla", CartaModificadores.textoLinea(mods, "sin cebolla"))
        assertEquals("", CartaModificadores.textoLinea(emptyList(), "  "))
    }

    @Test
    fun precioUnitario_suma_deltas() {
        val mods = listOf(
            ModificadorElegido(1, "Punto", 11, "Al punto", 0.0),
            ModificadorElegido(2, "Extras", 21, "Bacon", 1.5),
        )
        assertEquals(10.0, CartaModificadores.precioUnitario(8.5, mods), 0.0)
    }

    @Test
    fun faltanObligatorios_si_no_hay_eleccion() {
        val grupos = listOf(
            GrupoConOpciones(punto, listOf(alPunto, muyHecho)),
            GrupoConOpciones(extras, listOf(bacon)),
        )
        assertTrue(CartaModificadores.faltanObligatorios(grupos, emptyList()))
        assertFalse(
            CartaModificadores.faltanObligatorios(
                grupos,
                listOf(ModificadorElegido(1, "Punto", 11, "Al punto")),
            ),
        )
    }

    @Test
    fun agruparPorSubfamilia_respeta_consecutivos() {
        val prods = listOf(
            Producto(id = 1, nombre = "Agua", categoria = "Bebidas", precio = 1.0),
            Producto(id = 2, nombre = "Coca-Cola", categoria = "Bebidas", precio = 2.0, subfamilia = "Coca-Cola"),
            Producto(id = 3, nombre = "Coca-Cola Zero", categoria = "Bebidas", precio = 2.0, subfamilia = "Coca-Cola"),
            Producto(id = 4, nombre = "Café", categoria = "Bebidas", precio = 1.5),
        )
        val grupos = CartaModificadores.agruparPorSubfamilia(prods)
        assertEquals(3, grupos.size)
        assertEquals(null, grupos[0].first)
        assertEquals(listOf("Agua"), grupos[0].second.map { it.nombre })
        assertEquals("Coca-Cola", grupos[1].first)
        assertEquals(2, grupos[1].second.size)
        assertEquals(null, grupos[2].first)
        assertEquals(listOf("Café"), grupos[2].second.map { it.nombre })
    }
}
