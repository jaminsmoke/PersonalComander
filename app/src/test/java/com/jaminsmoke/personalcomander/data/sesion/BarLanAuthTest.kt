package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class BarLanAuthTest {

    // ── parseToken ──

    @Test
    fun parseToken_extrae_de_json_valido() {
        val token = BarLanCliente.parseToken("""{"token":"abc123","sesionActiva":true}""")
        assertEquals("abc123", token)
    }

    @Test
    fun parseToken_null_sin_campo() {
        val token = BarLanCliente.parseToken("""{"sesionActiva":true}""")
        assertNull(token)
    }

    @Test
    fun parseToken_null_campo_vacio() {
        val token = BarLanCliente.parseToken("""{"token":"","sesionActiva":true}""")
        assertNull(token)
    }

    @Test
    fun parseToken_null_campo_null_json() {
        val token = BarLanCliente.parseToken("""{"token":null,"sesionActiva":true}""")
        assertNull(token)
    }

    @Test
    fun parseToken_null_json_invalido() {
        assertNull(BarLanCliente.parseToken("no-json"))
        assertNull(BarLanCliente.parseToken(""))
        assertNull(BarLanCliente.parseToken("   "))
    }

    @Test
    fun parseToken_trim_espacios() {
        // Los tokens reales no llevan espacios, pero parseToken hace trim por defensa
        val token = BarLanCliente.parseToken("""{"token":"  abc123  ","sesionActiva":true}""")
        assertEquals("abc123", token)
    }

    // ── interpretarIniciar ──

    @Test
    fun interpretarIniciar_200_con_token_activa() {
        val resultado = BarLanCliente.interpretarIniciar(
            codigo = 200,
            cuerpo = """{"sesionActiva":true,"token":"tokABC"}""",
            token = "tokABC",
        )
        assertTrue(resultado.ok)
        assertEquals(200, resultado.codigo)
        assertTrue(resultado.sesionActiva)
        assertEquals("tokABC", resultado.token)
    }

    @Test
    fun interpretarIniciar_200_sin_token_bar_01() {
        val resultado = BarLanCliente.interpretarIniciar(
            codigo = 200,
            cuerpo = """{"sesionActiva":true}""",
        )
        assertTrue(resultado.ok)
        assertTrue(resultado.sesionActiva)
        assertNull(resultado.token)
        assertFalse(resultado.nodoViejo)
    }

    @Test
    fun interpretarIniciar_200_sesion_no_activa() {
        val resultado = BarLanCliente.interpretarIniciar(
            codigo = 200,
            cuerpo = """{"sesionActiva":false}""",
        )
        assertTrue(resultado.ok)
        assertFalse(resultado.sesionActiva)
        assertNull(resultado.token)
    }

    @Test
    fun interpretarIniciar_404_nodo_viejo_sin_jornada() {
        val resultado = BarLanCliente.interpretarIniciar(codigo = 404, cuerpo = "")
        assertTrue(resultado.ok)
        assertEquals(404, resultado.codigo)
        assertTrue(resultado.sesionActiva)
        assertTrue(resultado.nodoViejo)
        assertNull(resultado.token)
    }

    @Test
    fun interpretarIniciar_403_error() {
        val resultado = BarLanCliente.interpretarIniciar(codigo = 403, cuerpo = """{"error":"forbidden"}""")
        assertFalse(resultado.ok)
        assertEquals(403, resultado.codigo)
        assertFalse(resultado.sesionActiva)
        assertNull(resultado.token)
    }

    @Test
    fun interpretarIniciar_500_error() {
        val resultado = BarLanCliente.interpretarIniciar(codigo = 500, cuerpo = "")
        assertFalse(resultado.ok)
        assertEquals(500, resultado.codigo)
        assertFalse(resultado.sesionActiva)
        assertNull(resultado.token)
    }

    @Test
    fun interpretarIniciar_cuerpo_sin_sesion_activa_asume_true() {
        val resultado = BarLanCliente.interpretarIniciar(
            codigo = 200,
            cuerpo = """{}""",
        )
        assertTrue(resultado.ok)
        assertTrue(resultado.sesionActiva) // parseSesionActiva devuelve null → true
    }

    // ── parseSesionActiva ──

    @Test
    fun parseSesionActiva_true() {
        assertEquals(true, BarLanCliente.parseSesionActiva("""{"sesionActiva":true}"""))
    }

    @Test
    fun parseSesionActiva_false() {
        assertEquals(false, BarLanCliente.parseSesionActiva("""{"sesionActiva":false}"""))
    }

    @Test
    fun parseSesionActiva_snake_case() {
        assertEquals(true, BarLanCliente.parseSesionActiva("""{"sesion_activa":true}"""))
    }

    @Test
    fun parseSesionActiva_campo_ausente() {
        assertNull(BarLanCliente.parseSesionActiva("""{}"""))
    }

    @Test
    fun parseSesionActiva_json_invalido() {
        assertNull(BarLanCliente.parseSesionActiva("basura"))
    }

    @Test
    fun parseSesionActiva_no_es_booleano() {
        assertNull(BarLanCliente.parseSesionActiva("""{"sesionActiva":"true"}"""))
    }

    // ── JornadaLanResult ──

    @Test
    fun jornadaLanResult_token_se_preserva() {
        val resultado = BarLanCliente.JornadaLanResult(
            ok = true,
            codigo = 200,
            sesionActiva = true,
            token = "tokXYZ",
        )
        assertEquals("tokXYZ", resultado.token)
    }

    @Test
    fun jornadaLanResult_sin_token_null_por_defecto() {
        val resultado = BarLanCliente.JornadaLanResult(ok = false, codigo = 0)
        assertNull(resultado.token)
    }

    // ── postRonda recibe tokenLan ──

    @Test
    fun postRondaResult_sin_tickets() {
        val resultado = BarLanCliente.PostRondaResult(ok = false, codigo = 0)
        assertFalse(resultado.ok)
        assertTrue(resultado.tickets.isEmpty())
    }

    // ── parseHealth con establecimientoId ──

    @Test
    fun parseHealth_con_establecimiento_id() {
        val health = BarLanCliente.parseHealth(
            """{"ok":true,"role":"bar","establecimiento":"Casa Pepe","establecimiento_id":"aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee","version":"0.2"}"""
        )
        assertNotNull(health)
        assertEquals("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee", health!!.establecimientoId)
    }

    @Test
    fun parseHealth_establecimiento_id_null() {
        val health = BarLanCliente.parseHealth(
            """{"ok":true,"role":"bar","establecimiento":"Casa Pepe","establecimiento_id":null}"""
        )
        assertNotNull(health)
        assertNull(health!!.establecimientoId)
    }

    @Test
    fun parseHealth_establecimiento_id_ausente() {
        val health = BarLanCliente.parseHealth(
            """{"ok":true,"role":"bar","establecimiento":"Casa Pepe"}"""
        )
        assertNotNull(health)
        assertNull(health!!.establecimientoId)
    }
}