package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parseo y política de caducidad del par rotado (refresh Identity). Sin red ni Android. */
class SesionRefreshTest {

    // --- parseLogin con el contrato nuevo (PR #181) ---

    @Test
    fun parseLogin_con_refresh_y_expira() {
        val body = """
            {"token":"jwt","refresh_token":"r-30d","expires_in":43200,"sesion_id":"s-1",
             "camarero":{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com"},
             "qr":"phid1:u1:c1:sig"}
        """.trimIndent()
        val sesion = IdentityJson.parseLogin(body)
        assertEquals("jwt", sesion.token)
        assertEquals("r-30d", sesion.refreshToken)
        assertEquals("s-1", sesion.sesionId)
        assertEquals(43200L, sesion.expiresInSegundos)
    }

    @Test
    fun parseLogin_legacy_sin_refresh() {
        val body = """
            {"token":"abc","camarero":{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com"},
             "qr":"phid1:u1:c1:sig"}
        """.trimIndent()
        val sesion = IdentityJson.parseLogin(body)
        assertNull(sesion.refreshToken)
        assertNull(sesion.sesionId)
        assertNull(sesion.expiresInSegundos)
    }

    @Test
    fun parseLoginOrNull_sin_refresh_sigue_funcionando() {
        val body = """
            {"token":"abc","camarero":{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com"},"qr":"phid1:u1:c1:sig"}
        """.trimIndent()
        val sesion = IdentityJson.parseLoginOrNull(body)
        assertEquals("abc", sesion!!.token)
        assertNull(sesion.refreshToken)
    }

    @Test
    fun parseLoginOrNull_basura_sigue_null() {
        assertNull(IdentityJson.parseLoginOrNull("{}"))
        assertNull(IdentityJson.parseLoginOrNull("no-json"))
    }

    // ----------------------------------------------------- parseRefresh / parseRefreshOrNull ---

    @Test
    fun parseRefreshOrNull_completo() {
        val body = """{"token":"jwt-2","refresh_token":"r-rotado-2","expires_in":43200,"sesion_id":"s-1"}"""
        val r = IdentityJson.parseRefreshOrNull(body)!!
        assertEquals("jwt-2", r.token)
        assertEquals("r-rotado-2", r.refreshToken)
        assertEquals("s-1", r.sesionId)
        assertEquals(43200L, r.expiresInSegundos)
    }

    @Test
    fun parseRefreshOrNull_sin_sesion_id() {
        val body = """{"token":"jwt-2","refresh_token":"r-rotado-2","expires_in":43200}"""
        val r = IdentityJson.parseRefreshOrNull(body)!!
        assertEquals("jwt-2", r.token)
        assertNull(r.sesionId)
        assertEquals(43200L, r.expiresInSegundos)
    }

    @Test
    fun parseRefreshOrNull_sin_expires() {
        val body = """{"token":"jwt-2","refresh_token":"r-rotado-2"}"""
        val r = IdentityJson.parseRefreshOrNull(body)!!
        assertNull(r.expiresInSegundos)
    }

    @Test
    fun parseRefreshOrNull_cuerpo_invalido() {
        assertNull(IdentityJson.parseRefreshOrNull("{}"))
        assertNull(IdentityJson.parseRefreshOrNull("""{"token":"x"}"""))
        assertNull(IdentityJson.parseRefreshOrNull("""{"refresh_token":"x"}"""))
        assertNull(IdentityJson.parseRefreshOrNull("no-json"))
    }

    @Test
    fun parseRefreshOrNull_error_identity() {
        val body = """{"detail":"Refresh inválido","code":"identity.refresh_invalido"}"""
        assertNull(IdentityJson.parseRefreshOrNull(body))
    }

    // ------------------------------------------------------------- expiraEnDe / debeRenovar ---

    @Test
    fun expiraEnDe_segundos_a_epoch() {
        val ahora = 1_700_000_000_000L
        assertEquals(1_700_043_200_000L, IdentityJson.expiraEnDe(43200L, ahora))
    }

    @Test
    fun expiraEnDe_null_o_invalido() {
        assertNull(IdentityJson.expiraEnDe(null, 1_700_000_000_000L))
        assertNull(IdentityJson.expiraEnDe(0L, 1_700_000_000_000L))
        assertNull(IdentityJson.expiraEnDe(-5L, 1_700_000_000_000L))
    }

    @Test
    fun debeRenovar_sin_expira_no_renueva() {
        assertFalse(IdentityJson.debeRenovar(null, 1_700_000_000_000L))
    }

    @Test
    fun debeRenovar_caducado_true() {
        val expira = 1_700_000_000_000L
        assertTrue(IdentityJson.debeRenovar(expira, expira))
        assertTrue(IdentityJson.debeRenovar(expira, expira + 5_000L))
    }

    @Test
    fun debeRenovar_dentro_del_margen_true() {
        val ahora = 1_700_000_000_000L
        // Caduca en 30 s < margen de 60 s → renovar
        assertTrue(IdentityJson.debeRenovar(ahora + 30_000L, ahora))
        // Caduca en exactamente el margen → renovar
        assertTrue(IdentityJson.debeRenovar(ahora + 60_000L, ahora))
    }

    @Test
    fun debeRenovar_lejos_false() {
        val ahora = 1_700_000_000_000L
        assertFalse(IdentityJson.debeRenovar(ahora + 300_000L, ahora))
    }

    @Test
    fun debeRenovar_margen_personalizado() {
        val ahora = 1_700_000_000_000L
        // Con margen de 10 min, 5 min < 10 min → renovar
        assertTrue(IdentityJson.debeRenovar(ahora + 300_000L, ahora, margenMs = 600_000L))
        // Con margen de 1 min, 5 min está lejos → no renovar
        assertFalse(IdentityJson.debeRenovar(ahora + 300_000L, ahora, margenMs = 60_000L))
    }
}