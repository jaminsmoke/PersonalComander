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
            {"token":"abc","camarero":{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com","telefono":null,"foto_url":"/v1/camareros/me/foto"},"qr":"phid1:u1:c1:sig"}
        """.trimIndent()
        val sesion = IdentityJson.parseLogin(body)
        assertEquals("abc", sesion.token)
        assertEquals("u1", sesion.perfil.id)
        assertEquals("Ana", sesion.perfil.nombre)
        assertEquals("phid1:u1:c1:sig", sesion.qr)
        assertEquals(4, sesion.qr.split(":").size)
        assertEquals("/v1/camareros/me/foto", sesion.perfil.fotoUrl)
        assertEquals(null, sesion.perfil.nick)
    }

    @Test
    fun parseLogin_con_nick() {
        val body = """
            {"token":"abc","camarero":{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com","telefono":null,"foto_url":null,"nick":"Anita"},"qr":"phid1:u1:c1:sig"}
        """.trimIndent()
        val sesion = IdentityJson.parseLogin(body)
        assertEquals("Anita", sesion.perfil.nick)
        assertEquals("Anita", sesion.perfil.mote)
    }

    @Test
    fun parsePerfil_sin_foto() {
        val perfil = IdentityJson.parsePerfil(
            com.google.gson.JsonParser.parseString(
                """{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com","telefono":null,"foto_url":null}""",
            ),
        )
        assertEquals(null, perfil.fotoUrl)
        assertEquals(null, perfil.telefono)
        assertEquals(null, perfil.nick)
    }

    @Test
    fun parseError_incluye_code() {
        val err = IdentityJson.parseError(
            """{"detail":"Clave revocada. Renueva la clave","code":"identity.credential_revoked"}""",
        )
        assertEquals("Clave revocada. Renueva la clave", err.detail)
        assertEquals(IdentityJson.CODE_CREDENTIAL_REVOKED, err.code)
    }

    @Test
    fun parseError_password_incorrecta() {
        val err = IdentityJson.parseError(
            """{"detail":"Contraseña incorrecta","code":"identity.password_incorrecta"}""",
        )
        assertEquals(IdentityJson.CODE_PASSWORD_INCORRECTA, err.code)
    }

    @Test
    fun parseFotoUrl_null() {
        assertEquals(null, IdentityJson.parseFotoUrl("""{"foto_url":null}"""))
    }

    @Test
    fun parseFotoUrl_path() {
        assertEquals("/v1/camareros/me/foto", IdentityJson.parseFotoUrl("""{"foto_url":"/v1/camareros/me/foto"}"""))
    }

    @Test
    fun parseRegistro() {
        val (id, qr) = IdentityJson.parseRegistro("""{"id":"u1","qr":"phid1:u1:c1:sig"}""")
        assertEquals("u1", id)
        assertTrue(qr.startsWith("phid1:"))
    }

    @Test
    fun parseEstablecimientos_lista() {
        val body = """
            [{"id":"e1","nombre":"Casa Pepe","cuenta_negocio_id":"n1","rol":"staff"}]
        """.trimIndent()
        val lista = IdentityJson.parseEstablecimientos(body)!!
        assertEquals(1, lista.size)
        assertEquals("e1", lista[0].id)
        assertEquals("Casa Pepe", lista[0].nombre)
        assertEquals("n1", lista[0].cuentaNegocioId)
        assertEquals("staff", lista[0].rol)
    }

    @Test
    fun parseEstablecimientos_vacia() {
        val lista = IdentityJson.parseEstablecimientos("[]")!!
        assertTrue(lista.isEmpty())
    }

    @Test
    fun parseEstablecimientos_item_sin_campo_se_omite() {
        val body = """[{"id":"e1","nombre":"Casa Pepe"}]"""
        val lista = IdentityJson.parseEstablecimientos(body)!!
        assertTrue(lista.isEmpty())
    }

    @Test
    fun parseEstablecimientos_no_array_es_null() {
        assertEquals(null, IdentityJson.parseEstablecimientos("""{"id":"e1"}"""))
        assertEquals(null, IdentityJson.parseEstablecimientos("no-json"))
    }

    @Test
    fun contrastarHealth_sin_datos() {
        assertEquals(ContrasteMembresia.SinDatos, IdentityJson.contrastarHealth("Casa Pepe", emptyList()))
        assertEquals(
            ContrasteMembresia.SinDatos,
            IdentityJson.contrastarHealth(
                null,
                listOf(MembresiaEstablecimiento("e1", "Casa Pepe", "n1", "staff")),
            ),
        )
    }

    @Test
    fun contrastarHealth_coincide_ignora_mayusculas() {
        val lista = listOf(MembresiaEstablecimiento("e1", "Casa Pepe", "n1", "staff"))
        assertEquals(ContrasteMembresia.Coincide, IdentityJson.contrastarHealth("casa pepe", lista))
    }

    @Test
    fun contrastarHealth_no_coincide() {
        val lista = listOf(MembresiaEstablecimiento("e1", "Casa Pepe", "n1", "staff"))
        assertEquals(ContrasteMembresia.NoCoincide, IdentityJson.contrastarHealth("Otro Local", lista))
    }
}

class BarLanClienteTest {

    @Test
    fun parseHealth_bar() {
        val h = BarLanCliente.parseHealth("""{"ok":true,"role":"bar","sala":"vacia","version":"0.1"}""")
        assertTrue(BarLanCliente.esBar(h))
        assertEquals("bar", h!!.role)
        assertEquals("vacia", h.establecimiento)
    }

    @Test
    fun parseHealth_establecimiento_gana_a_sala() {
        val h = BarLanCliente.parseHealth(
            """{"ok":true,"role":"bar","establecimiento":"Casa Pepe","sala":"vacia","version":"0.2"}"""
        )
        assertEquals("Casa Pepe", h!!.establecimiento)
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
