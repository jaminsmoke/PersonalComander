package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityJsonTest {

    @Test
    fun parseError_string() {
        val msg = IdentityJson.parseErrorDetail("""{"detail":"Email o contraseña incorrectos"}""")
        assertEquals("Email o contraseña incorrectos", msg)
    }

    @Test
    fun parseError_lista() {
        val msg = IdentityJson.parseErrorDetail("""{"detail":["El campo 'email' es obligatorio"]}""")
        assertTrue(msg.contains("email"))
    }

    @Test
    fun parseLogin() {
        val body = """
            {"token":"abc","camarero":{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com","telefono":null},"qr":"phid1:u1:c1:sig"}
        """.trimIndent()
        val sesion = IdentityJson.parseLogin(body)
        assertEquals("abc", sesion.token)
        assertEquals("u1", sesion.perfil.id)
        assertEquals("Ana", sesion.perfil.nombre)
        assertEquals("phid1:u1:c1:sig", sesion.qr)
        assertEquals(4, sesion.qr.split(":").size)
    }

    @Test
    fun parseRegistro() {
        val (id, qr) = IdentityJson.parseRegistro("""{"id":"u1","qr":"phid1:u1:c1:sig"}""")
        assertEquals("u1", id)
        assertTrue(qr.startsWith("phid1:"))
    }
}

class BarLanClienteTest {

    @Test
    fun parseHealth_bar() {
        val h = BarLanCliente.parseHealth("""{"ok":true,"role":"bar","sala":"vacia","version":"0.1"}""")
        assertTrue(BarLanCliente.esBar(h))
        assertEquals("bar", h!!.role)
    }

    @Test
    fun parseHealth_no_bar() {
        val h = BarLanCliente.parseHealth("""{"ok":true,"role":"identity"}""")
        assertFalse(BarLanCliente.esBar(h))
    }

    @Test
    fun parseHealth_invalido() {
        assertEquals(null, BarLanCliente.parseHealth("no-json"))
    }
}
