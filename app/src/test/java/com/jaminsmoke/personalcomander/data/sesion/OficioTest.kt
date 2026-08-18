package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class OficioTest {

    @Test
    fun limites_dia_desde_medianoche_local() {
        val ahora = ZonedDateTime.of(2026, 8, 18, 15, 30, 0, 0, ZoneOffset.ofHours(2))
        val b = OficioVentana.DIA.limites(ahora)
        assertEquals(Instant.parse("2026-08-18T00:00:00+02:00"), b.desde)
        assertEquals(ahora.toInstant(), b.hasta)
    }

    @Test
    fun limites_semana_desde_lunes() {
        val martes = ZonedDateTime.of(2026, 8, 18, 10, 0, 0, 0, ZoneOffset.UTC)
        val b = OficioVentana.SEMANA.limites(martes)
        assertEquals(Instant.parse("2026-08-17T00:00:00Z"), b.desde)
    }

    @Test
    fun limites_mes_desde_dia_1() {
        val ahora = ZonedDateTime.of(2026, 8, 18, 10, 0, 0, 0, ZoneOffset.UTC)
        val b = OficioVentana.MES.limites(ahora)
        assertEquals(Instant.parse("2026-08-01T00:00:00Z"), b.desde)
    }

    @Test
    fun formato_horas_compuesto() {
        assertEquals("2 h 15 min", formatoHorasOficio(2 * 3600 + 15 * 60))
        assertEquals("3 h", formatoHorasOficio(3 * 3600))
        assertEquals("45 min", formatoHorasOficio(45 * 60))
        assertEquals("0 min", formatoHorasOficio(0))
    }

    @Test
    fun horas_por_dia_recorta_intervalo_a_la_ventana() {
        val zona = ZoneOffset.UTC
        val jornada = JornadaOficio(
            id = "j1",
            camareroId = "c1",
            establecimientoId = "e1",
            inicio = Instant.parse("2026-08-17T22:00:00Z"),
            fin = Instant.parse("2026-08-18T02:00:00Z"),
        )
        val puntos = horasPorDia(
            listOf(jornada),
            Instant.parse("2026-08-18T00:00:00Z"),
            Instant.parse("2026-08-18T12:00:00Z"),
            zona,
        )
        assertEquals(1, puntos.size)
        assertEquals(LocalDate.of(2026, 8, 18), puntos.single().fecha)
        assertEquals(2 * 3600, puntos.single().segundos)
    }

    @Test
    fun parse_resumen_mapea_mesas_servidas_a_rondas() {
        val json = """
            {"desde":"2026-08-01T00:00:00Z","hasta":"2026-08-18T12:00:00Z",
             "horas_segundos":3660,"mesas_servidas":4,
             "por_establecimiento":[{"establecimiento_id":"e1","horas_segundos":3660,"mesas_servidas":4}]}
        """.trimIndent()
        val r = IdentityJson.parseResumenOficio(json)!!
        assertEquals(3660, r.horasSegundos)
        assertEquals(4, r.rondasServidas)
        assertEquals("e1", r.porEstablecimiento.single().establecimientoId)
    }

    @Test
    fun parse_jornada_abierta_fin_null() {
        val json = """
            {"id":"j1","camarero_id":"c1","establecimiento_id":"e1",
             "inicio":"2026-08-18T08:00:00Z","fin":null}
        """.trimIndent()
        val j = IdentityJson.parseJornada(json)!!
        assertEquals("j1", j.id)
        assertNull(j.fin)
        assertEquals(Instant.parse("2026-08-18T08:00:00Z"), j.inicio)
    }

    @Test
    fun establecimiento_id_por_health_match_unico() {
        val membresias = listOf(
            MembresiaEstablecimiento("uuid-1", "La Terraza", "n1", "staff"),
            MembresiaEstablecimiento("uuid-2", "El Bar", "n2", "staff"),
        )
        assertEquals("uuid-1", IdentityJson.establecimientoIdPorHealth("la terraza", membresias))
        assertNull(IdentityJson.establecimientoIdPorHealth("otro", membresias))
        assertNull(IdentityJson.establecimientoIdPorHealth(null, membresias))
    }

    @Test
    fun parse_resumen_invalido_no_inventa() {
        assertNull(IdentityJson.parseResumenOficio("{}"))
        assertNull(IdentityJson.parseResumenOficio("no-json"))
    }

    @Test
    fun cuerpo_iniciar_jornada() {
        val o = com.google.gson.JsonParser.parseString(
            IdentityJson.cuerpoIniciarJornada("est-1"),
        ).asJsonObject
        assertEquals("est-1", o.get("establecimiento_id").asString)
        assertTrue(o.entrySet().size == 1)
    }
}
