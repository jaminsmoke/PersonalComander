package com.jaminsmoke.personalcomander.data.sesion

import com.jaminsmoke.personalcomander.data.CartaModificadores
import com.jaminsmoke.personalcomander.data.LineaEstado
import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.ModificadorElegido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecogerLogicaTest {

    private fun lp(
        id: Long,
        productoId: Long,
        nombre: String,
        estado: LineaEstado = LineaEstado.PENDIENTE,
        ticketId: String? = null,
    ) = LineaPedido(
        id = id,
        pedidoId = 42,
        productoId = productoId,
        nombreProducto = nombre,
        precioUnitario = 1.0,
        cantidad = 1,
        estado = estado,
        ticketId = ticketId,
    )

    @Test
    fun lineasAEnviar_solo_pendientes() {
        val lineas = listOf(
            lp(1, 12, "Caña", LineaEstado.PENDIENTE),
            lp(2, 3, "Croquetas", LineaEstado.ENVIADA, "t-cocina"),
            lp(3, 4, "Tarta", LineaEstado.LISTA, "t-postre"),
        )
        val delta = RecogerLogica.lineasAEnviar(lineas)
        assertEquals(1, delta.size)
        assertEquals(12L, delta[0].productoId)
    }

    @Test
    fun lineaPendienteDelProducto_no_pisa_enviada() {
        val lineas = listOf(
            lp(1, 12, "Caña", LineaEstado.ENVIADA, "t1"),
            lp(2, 12, "Caña", LineaEstado.PENDIENTE),
        )
        val p = RecogerLogica.lineaPendienteDelProducto(lineas, 12)
        assertEquals(2L, p!!.id)
        assertNull(RecogerLogica.lineaPendienteDelProducto(lineas, 99))
    }

    @Test
    fun lineaPendienteCompatible_distingue_modificadores() {
        val alPunto = LineaPedido(
            id = 1, pedidoId = 42, productoId = 10,
            nombreProducto = "Hamburguesa", precioUnitario = 8.0, cantidad = 1,
            modificadoresJson = CartaModificadores.canonicalJson(
                listOf(ModificadorElegido(1, "Punto", 11, "Al punto")),
            ),
        )
        val muyHecho = alPunto.copy(
            id = 2,
            modificadoresJson = CartaModificadores.canonicalJson(
                listOf(ModificadorElegido(1, "Punto", 12, "Muy hecho")),
            ),
        )
        val hit = RecogerLogica.lineaPendienteCompatible(
            listOf(alPunto, muyHecho),
            10,
            nota = null,
            modificadoresJson = muyHecho.modificadoresJson,
        )
        assertEquals(2L, hit!!.id)
        assertNull(
            RecogerLogica.lineaPendienteCompatible(
                listOf(alPunto), 10, nota = "sin cebolla", modificadoresJson = alPunto.modificadoresJson,
            ),
        )
    }

    @Test
    fun asignarTickets_cruza_por_producto() {
        val enviadas = listOf(lp(1, 12, "Caña"), lp(2, 3, "Croquetas"))
        val tickets = listOf(
            TicketLan(
                id = "p42-t1-barra",
                rondaId = "p42-t1",
                destino = "BARRA",
                lineas = listOf(LineaTicketLan("12", "Caña", 1)),
            ),
            TicketLan(
                id = "p42-t1-cocina",
                rondaId = "p42-t1",
                destino = "COCINA",
                lineas = listOf(LineaTicketLan("3", "Croquetas", 1)),
            ),
        )
        val out = RecogerLogica.asignarTickets(enviadas, tickets)
        assertEquals(LineaEstado.ENVIADA, out[0].estado)
        assertEquals("p42-t1-barra", out[0].ticketId)
        assertEquals("p42-t1-cocina", out[1].ticketId)
    }

    @Test
    fun asignarTickets_sin_body_marca_enviada_sin_id() {
        val out = RecogerLogica.asignarTickets(listOf(lp(1, 12, "Caña")), emptyList())
        assertEquals(LineaEstado.ENVIADA, out[0].estado)
        assertNull(out[0].ticketId)
    }

    @Test
    fun asignarTickets_cruza_por_codigoBar() {
        val enviadas = listOf(lp(1, 12, "Caña"), lp(2, 3, "Croquetas"))
        val tickets = listOf(
            TicketLan(
                id = "p42-t1-barra",
                rondaId = "p42-t1",
                destino = "BARRA",
                lineas = listOf(LineaTicketLan("cana", "Caña", 1)),
            ),
            TicketLan(
                id = "p42-t1-cocina",
                rondaId = "p42-t1",
                destino = "COCINA",
                lineas = listOf(LineaTicketLan("3", "Croquetas", 1)),
            ),
        )
        val out = RecogerLogica.asignarTickets(
            enviadas,
            tickets,
            codigoBarPorProductoId = mapOf(12L to "cana"),
        )
        assertEquals("p42-t1-barra", out[0].ticketId)
        assertEquals("p42-t1-cocina", out[1].ticketId)
    }

    @Test
    fun bloques_tres_secciones() {
        val b = RecogerLogica.bloques(
            listOf(
                lp(1, 1, "A", LineaEstado.PENDIENTE),
                lp(2, 2, "B", LineaEstado.ENVIADA),
                lp(3, 3, "C", LineaEstado.LISTA),
                lp(4, 4, "D", LineaEstado.SERVIDA),
            )
        )
        assertEquals(2, b.estaRonda.size)
        assertEquals(1, b.paraRecoger.size)
        assertEquals(1, b.servido.size)
    }

    @Test
    fun puedeMarcarServida_lista_siempre_enviada_solo_local() {
        val lista = lp(1, 1, "A", LineaEstado.LISTA)
        val enviada = lp(2, 2, "B", LineaEstado.ENVIADA)
        assertTrue(RecogerLogica.puedeMarcarServida(lista, ligadoAlBar = true))
        assertFalse(RecogerLogica.puedeMarcarServida(enviada, ligadoAlBar = true))
        assertTrue(RecogerLogica.puedeMarcarServida(enviada, ligadoAlBar = false))
    }

    @Test
    fun parseSalaEvent_ciego() {
        val ciego = RecogerLogica.parseSalaEvent(
            """{"tipo":"ticket.preparado","ticketId":"p1-barra","preparadoPor":"Ana"}"""
        )!!
        assertEquals(RecogerLogica.TIPO_PREPARADO, ciego.tipo)
        assertEquals("p1-barra", ciego.ticketId)
        assertNull(ciego.mesaId)
        assertEquals(PlantillaAviso.SIN_MESA, RecogerLogica.plantillaAviso(ciego))
    }

    @Test
    fun parseSalaEvent_bar_v1_anida_destino_y_cola_en_ticket() {
        val json = """
            {
              "version": 1,
              "tipo": "ticket.preparado",
              "ticketId": "r-123-barra",
              "preparadoPor": "Ana",
              "mesaId": "T3",
              "camarero": "Lucía",
              "resumen": "2× Caña",
              "ticket": {
                "id": "r-123-barra",
                "rondaId": "r-123",
                "destino": "BARRA",
                "estado": "PREPARADO",
                "preparadoPor": "Ana",
                "numeroCola": 1,
                "lineas": [{"productoId":"cana","nombreProducto":"Caña","cantidad":2}]
              }
            }
        """.trimIndent()
        val e = RecogerLogica.parseSalaEvent(json)!!
        assertEquals("T3", e.mesaId)
        assertEquals("BARRA", e.destino)
        assertEquals(1, e.numeroCola)
        assertEquals("r-123", e.rondaId)
        assertEquals(PlantillaAviso.COMPLETO, RecogerLogica.plantillaAviso(e))
    }

    @Test
    fun parseSalaEvent_planos_siguen_valiendo() {
        val rico = RecogerLogica.parseSalaEvent(
            """{"tipo":"ticket.preparado","ticketId":"p1-barra","rondaId":"p1","mesaId":"T3","destino":"BARRA","numeroCola":1}""",
        )!!
        assertEquals(PlantillaAviso.COMPLETO, RecogerLogica.plantillaAviso(rico))
    }

    @Test
    fun plantillaAviso_solo_mesa_si_falta_cola() {
        val e = RecogerLogica.parseSalaEvent(
            """{"tipo":"ticket.preparado","ticketId":"p1-barra","mesaId":"T3"}""",
        )!!
        assertEquals(PlantillaAviso.SOLO_MESA, RecogerLogica.plantillaAviso(e))
    }

    @Test
    fun parseSalaEvent_usa_event_sse_si_json_sin_tipo() {
        val e = RecogerLogica.parseSalaEvent(
            """{"ticketId":"x-cocina"}""",
            eventType = "ticket.recogido",
        )!!
        assertEquals(RecogerLogica.TIPO_RECOGIDO, e.tipo)
    }

    @Test
    fun parseTickets_y_estado() {
        val tickets = RecogerLogica.parseTickets(
            """[{"id":"p1-barra","rondaId":"p1","destino":"BARRA","lineas":[{"productoId":"12","nombreProducto":"Caña","cantidad":2}]}]"""
        )
        assertEquals(1, tickets.size)
        assertEquals("12", tickets[0].lineas[0].productoId)

        val estado = RecogerLogica.parseEstado(
            """{"bebida":[{"id":"a","estado":"PREPARADO"}],"comida":[],"servidos":[{"id":"b"}]}"""
        )!!
        assertEquals(1, RecogerLogica.ticketsDeColas(estado).size)
        assertEquals("PREPARADO", estado.bebida[0].estado)
        assertEquals("b", estado.servidos[0].id)
    }

    @Test
    fun alimentarSse_cierra_frame() {
        val data = StringBuilder()
        var tipo: String? = null
        var evento: SalaEventLan? = null
        listOf(
            "event: ticket.preparado",
            """data: {"ticketId":"t1","tipo":"ticket.preparado","mesaId":"T3"}""",
            "",
        ).forEach { line ->
            val (nt, ev) = RecogerLogica.alimentarSse(tipo, data, line)
            tipo = nt
            if (ev != null) evento = ev
        }
        val cerrado = evento!!
        assertEquals("T3", cerrado.mesaId)
        assertEquals("t1", cerrado.ticketId)
    }

    @Test
    fun parseSalaEvent_sesion_cortada_sin_ticket() {
        val e = RecogerLogica.parseSalaEvent(
            """{"tipo":"sesion.cortada"}""",
        )!!
        assertEquals(RecogerLogica.TIPO_SESION_CORTADA, e.tipo)
    }

    @Test
    fun parseSalaEvent_sesion_cortada_por_event_sse() {
        val e = RecogerLogica.parseSalaEvent("{}", eventType = RecogerLogica.TIPO_SESION_CORTADA)!!
        assertEquals(RecogerLogica.TIPO_SESION_CORTADA, e.tipo)
    }

    @Test
    fun parseSalaEvent_sin_ticket_sigue_nulo_si_no_es_corte() {
        assertEquals(null, RecogerLogica.parseSalaEvent("""{"tipo":"ticket.preparado"}"""))
    }

    @Test
    fun destinoClave() {
        assertEquals("bebida", RecogerLogica.destinoClave("BARRA"))
        assertEquals("comida", RecogerLogica.destinoClave("cocina"))
        assertNull(RecogerLogica.destinoClave(null))
    }
}
