package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonParser
import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.Mesa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RondaLanMapperTest {

    private val mesaTerraza = Mesa(
        id = 99,
        numero = 3,
        alias = "Mesa de la esquina",
        salaId = 2,
        indiceZona = 3,
    )

    private val lineas = listOf(
        LineaPedido(
            id = 1, pedidoId = 42, productoId = 12,
            nombreProducto = "Caña", precioUnitario = 2.5, cantidad = 2,
        ),
        LineaPedido(
            id = 2, pedidoId = 42, productoId = 3,
            nombreProducto = "Croquetas", precioUnitario = 6.0, cantidad = 1,
        ),
    )

    @Test
    fun mesaId_es_idZona_no_room_ni_alias() {
        val ronda = RondaLanMapper.desdePedido(
            pedidoId = 42,
            mesa = mesaTerraza,
            nombreSala = "Terraza",
            lineas = lineas,
            camarero = "Lucía García",
            creadoEn = 1_730_000_000_000L,
        )
        assertEquals("T3", ronda.mesaId)
        assertEquals("p42-t1730000000000", ronda.id)
        assertEquals("Lucía García", ronda.camarero)
        assertEquals("12", ronda.lineas[0].productoId)
        assertEquals("Caña", ronda.lineas[0].nombreProducto)
        assertEquals(2, ronda.lineas[0].cantidad)
    }

    @Test
    fun json_usa_nombres_del_contrato_bar() {
        val ronda = RondaLanMapper.desdePedido(
            pedidoId = 7,
            mesa = mesaTerraza,
            nombreSala = "Terraza",
            lineas = lineas,
            camarero = "Ana",
            creadoEn = 100L,
        )
        val o = JsonParser.parseString(RondaLanMapper.toJson(ronda)).asJsonObject
        assertEquals("T3", o.get("mesaId").asString)
        assertFalse(o.get("mesaId").asString == "99")
        assertFalse(o.get("mesaId").asString.contains("esquina"))
        assertEquals("p7-t100", o.get("id").asString)
        assertEquals(1, o.get("numero").asInt)
        assertEquals("Ana", o.get("camarero").asString)
        val primera = o.getAsJsonArray("lineas")[0].asJsonObject
        assertEquals("12", primera.get("productoId").asString)
        assertEquals("Caña", primera.get("nombreProducto").asString)
        assertEquals(2, primera.get("cantidad").asInt)
        assertTrue(primera.has("estado"))
    }

    @Test
    fun sala_interior_usa_prefijo_I() {
        val mesa = Mesa(id = 1, numero = 1, salaId = 1, indiceZona = 4)
        val ronda = RondaLanMapper.desdePedido(
            pedidoId = 1,
            mesa = mesa,
            nombreSala = "Interior",
            lineas = lineas.take(1),
            camarero = null,
            creadoEn = 1L,
        )
        assertEquals("I4", ronda.mesaId)
    }

    @Test
    fun productoId_usa_codigoBar_si_hay_match() {
        val ronda = RondaLanMapper.desdePedido(
            pedidoId = 42,
            mesa = mesaTerraza,
            nombreSala = "Terraza",
            lineas = lineas,
            camarero = "Lucía García",
            creadoEn = 1L,
            codigoBarPorProductoId = mapOf(12L to "cana"),
        )
        assertEquals("cana", ronda.lineas[0].productoId)
        assertEquals("3", ronda.lineas[1].productoId)
    }

    @Test
    fun linea_incluye_nota_y_modificadores() {
        val conMods = listOf(
            LineaPedido(
                id = 1, pedidoId = 42, productoId = 10,
                nombreProducto = "Hamburguesa", precioUnitario = 8.0, cantidad = 1,
                nota = "sin cebolla",
                modificadoresJson = com.jaminsmoke.personalcomander.data.CartaModificadores.canonicalJson(
                    listOf(
                        com.jaminsmoke.personalcomander.data.ModificadorElegido(
                            1, "Punto", 11, "Al punto", 0.0,
                        ),
                    ),
                ),
            ),
        )
        val ronda = RondaLanMapper.desdePedido(
            pedidoId = 42,
            mesa = mesaTerraza,
            nombreSala = "Terraza",
            lineas = conMods,
            camarero = "Ana",
            creadoEn = 1L,
        )
        val linea = ronda.lineas.single()
        assertEquals("sin cebolla", linea.nota)
        assertEquals(1, linea.modificadores.size)
        assertEquals("Punto", linea.modificadores[0].grupo)
        assertEquals("Al punto", linea.modificadores[0].opcion)
        val o = JsonParser.parseString(RondaLanMapper.toJson(ronda)).asJsonObject
        val primera = o.getAsJsonArray("lineas")[0].asJsonObject
        assertEquals("sin cebolla", primera.get("nota").asString)
        assertTrue(primera.has("modificadores"))
    }
}
