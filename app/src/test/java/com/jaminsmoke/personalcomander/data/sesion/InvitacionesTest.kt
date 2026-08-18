package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvitacionesTest {

    @Test
    fun parse_lista_mixta_pendiente_y_aceptada() {
        val json = """
            [
              {"id":"i1","establecimiento_id":"e1","establecimiento_nombre":"La Terraza",
               "rol":"staff","estado":"pendiente",
               "expira_en":"2026-08-21T12:00:00Z","creada_en":"2026-08-18T08:00:00Z"},
              {"id":"i2","establecimiento_id":"e2","establecimiento_nombre":"El Bar",
               "rol":"staff","estado":"aceptada",
               "expira_en":"2026-08-20T12:00:00Z","creada_en":"2026-08-17T08:00:00Z"}
            ]
        """.trimIndent()
        val lista = IdentityJson.parseInvitaciones(json)!!
        assertEquals(2, lista.size)
        assertTrue(lista[0].esPendiente)
        assertEquals("La Terraza", lista[0].establecimientoNombre)
        assertFalse(lista[1].esPendiente)
        assertEquals("aceptada", lista[1].estado)
    }

    @Test
    fun parse_cuerpo_no_array_es_nulo() {
        assertNull(IdentityJson.parseInvitaciones("""{"error":"no"}"""))
        assertNull(IdentityJson.parseInvitaciones("no-json"))
    }

    @Test
    fun parse_lista_vacia() {
        assertEquals(emptyList<InvitacionCamarero>(), IdentityJson.parseInvitaciones("[]"))
    }

    @Test
    fun parse_omite_filas_sin_id() {
        val json = """
            [
              {"establecimiento_id":"e1","establecimiento_nombre":"X","rol":"staff",
               "estado":"pendiente","expira_en":"2026-08-21T12:00:00Z","creada_en":"2026-08-18T08:00:00Z"},
              {"id":"i1","establecimiento_id":"e1","establecimiento_nombre":"La Terraza",
               "rol":"staff","estado":"rechazada",
               "expira_en":"2026-08-21T12:00:00Z","creada_en":"2026-08-18T08:00:00Z"}
            ]
        """.trimIndent()
        val lista = IdentityJson.parseInvitaciones(json)!!
        assertEquals(listOf("i1"), lista.map { it.id })
        assertEquals("rechazada", lista.single().estado)
    }
}
