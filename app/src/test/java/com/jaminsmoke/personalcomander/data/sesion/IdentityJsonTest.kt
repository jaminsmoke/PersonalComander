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
    fun parseLogin_con_ficha_url() {
        val body = """
            {"token":"abc","camarero":{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com"},"qr":"phid1:u1:c1:sig","ficha_url":"https://ficha.example/ficha?qr=phid1:u1:c1:sig"}
        """.trimIndent()
        val sesion = IdentityJson.parseLogin(body)
        assertEquals("phid1:u1:c1:sig", sesion.qr)
        assertEquals("https://ficha.example/ficha?qr=phid1:u1:c1:sig", sesion.fichaUrl)
    }

    @Test
    fun parseQr_con_ficha_url() {
        val q = IdentityJson.parseQr(
            """{"qr":"phid1:u1:c1:sig","ficha_url":"https://ficha.example/ficha?qr=phid1:u1:c1:sig"}""",
        )
        assertEquals("phid1:u1:c1:sig", q.qr)
        assertEquals("https://ficha.example/ficha?qr=phid1:u1:c1:sig", q.fichaUrl)
    }

    @Test
    fun parseQr_sin_ficha_url() {
        val q = IdentityJson.parseQr("""{"qr":"phid1:u1:c1:sig"}""")
        assertEquals("phid1:u1:c1:sig", q.qr)
        assertEquals(null, q.fichaUrl)
    }

    @Test
    fun parseRegistro_con_ficha_url() {
        val r = IdentityJson.parseRegistro(
            """{"id":"u1","qr":"phid1:u1:c1:sig","ficha_url":"https://ficha.example/ficha?qr=phid1:u1:c1:sig"}""",
        )
        assertEquals("https://ficha.example/ficha?qr=phid1:u1:c1:sig", r.fichaUrl)
    }

    @Test
    fun parseVisibilidad_defaults_si_faltan_campos() {
        val v = IdentityJson.parseVisibilidad("{}")
        assertEquals(true, v.nombre)
        assertEquals(true, v.apellidos)
        assertEquals(true, v.nick)
        assertEquals(false, v.email)
        assertEquals(false, v.telefono)
        assertEquals(false, v.foto)
    }

    @Test
    fun parseVisibilidad_respeta_opt_in() {
        val v = IdentityJson.parseVisibilidad(
            """{"nombre":true,"apellidos":true,"nick":true,"email":true,"telefono":false,"foto":true}""",
        )
        assertTrue(v.email)
        assertFalse(v.telefono)
        assertTrue(v.foto)
        assertEquals(true, v.valor(CampoVisibilidad.EMAIL))
        assertEquals(false, v.con(CampoVisibilidad.EMAIL, false).email)
    }

    @Test
    fun cuerpoVisibilidad_es_parcial() {
        assertEquals("""{"foto":true}""", IdentityJson.cuerpoVisibilidad(CampoVisibilidad.FOTO, true))
        val completo = IdentityJson.parseVisibilidad(IdentityJson.cuerpoVisibilidadCompleto(VisibilidadCamarero.DEFAULT))
        assertEquals(VisibilidadCamarero.DEFAULT, completo)
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
        assertEquals(null, sesion.fichaUrl)
        assertEquals(4, sesion.qr.split(":").size)
        assertEquals("/v1/camareros/me/foto", sesion.perfil.fotoUrl)
        assertEquals(null, sesion.perfil.nick)
        assertEquals(DataOrigin.Real, sesion.perfil.origen)
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
        assertEquals(DataOrigin.Real, perfil.origen)
    }

    @Test
    fun parsePerfil_data_origin_test() {
        val perfil = IdentityJson.parsePerfil(
            com.google.gson.JsonParser.parseString(
                """{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com","data_origin":"test"}""",
            ),
        )
        assertEquals(DataOrigin.Test, perfil.origen)
        assertEquals(DataOrigin.Test, perfil.dataOrigin)
    }

    @Test
    fun parsePerfil_data_origin_demo() {
        val perfil = IdentityJson.parsePerfil(
            com.google.gson.JsonParser.parseString(
                """{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com","data_origin":"demo"}""",
            ),
        )
        assertEquals(DataOrigin.Demo, perfil.origen)
    }

    @Test
    fun parsePerfil_data_origin_basura_es_real() {
        val perfil = IdentityJson.parsePerfil(
            com.google.gson.JsonParser.parseString(
                """{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com","data_origin":"staging"}""",
            ),
        )
        assertEquals(DataOrigin.Real, perfil.origen)
    }

    @Test
    fun parseLogin_con_data_origin() {
        val body = """
            {"token":"abc","camarero":{"id":"u1","nombre":"Ana","apellidos":"García","email":"ana@example.com","data_origin":"test"},"qr":"phid1:u1:c1:sig"}
        """.trimIndent()
        val sesion = IdentityJson.parseLogin(body)
        assertEquals(DataOrigin.Test, sesion.perfil.origen)
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
        val r = IdentityJson.parseRegistro("""{"id":"u1","qr":"phid1:u1:c1:sig"}""")
        assertEquals("u1", r.id)
        assertTrue(r.qr.startsWith("phid1:"))
        assertEquals(DataOrigin.Real, r.dataOrigin)
    }

    @Test
    fun parseRegistro_con_origen() {
        val r = IdentityJson.parseRegistro("""{"id":"u1","qr":"phid1:u1:c1:sig","data_origin":"demo"}""")
        assertEquals(DataOrigin.Demo, r.dataOrigin)
    }

    @Test
    fun cuerpoRegistro_incluye_data_origin_real() {
        val json = IdentityJson.cuerpoRegistro(
            nombre = "Ana",
            apellidos = "García",
            email = "ana@example.com",
            password = "secret12",
            nick = "Anita",
        )
        val o = com.google.gson.JsonParser.parseString(json).asJsonObject
        assertEquals("real", o.get("data_origin").asString)
        assertEquals("Anita", o.get("nick").asString)
        assertEquals(false, o.has("telefono"))
    }

    @Test
    fun cuerpoRegistro_test_y_telefono() {
        val json = IdentityJson.cuerpoRegistro(
            nombre = "Ana",
            apellidos = "García",
            email = "ana@example.com",
            password = "secret12",
            nick = "Anita",
            telefono = "600111222",
            origin = DataOrigin.Test,
        )
        val o = com.google.gson.JsonParser.parseString(json).asJsonObject
        assertEquals("test", o.get("data_origin").asString)
        assertEquals("600111222", o.get("telefono").asString)
    }

    @Test
    fun gson_perfil_sin_origen_es_real() {
        val perfil = com.google.gson.Gson().fromJson(
            """{"id":"u1","nombre":"Ana","apellidos":"García","email":"a@b.c"}""",
            PerfilCamarero::class.java,
        )
        assertEquals(DataOrigin.Real, perfil.origen)
        assertEquals(null, perfil.dataOrigin)
    }

    @Test
    fun dataOrigin_fromWire() {
        assertEquals(DataOrigin.Real, DataOrigin.fromWire(null))
        assertEquals(DataOrigin.Real, DataOrigin.fromWire(""))
        assertEquals(DataOrigin.Real, DataOrigin.fromWire("real"))
        assertEquals(DataOrigin.Test, DataOrigin.fromWire("TEST"))
        assertEquals(DataOrigin.Demo, DataOrigin.fromWire(" Demo "))
        assertEquals("real", DataOrigin.Real.wire)
        assertEquals("test", DataOrigin.Test.wire)
        assertEquals("demo", DataOrigin.Demo.wire)
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

    @Test
    fun parseSesion_admitido() {
        val s = BarLanCliente.parseSesion(
            """{"admitido":true,"camareroId":"11111111-1111-4111-8111-111111111111","nombre":"luciaTest"}""",
        )
        assertEquals(true, s!!.admitido)
        assertEquals("11111111-1111-4111-8111-111111111111", s.camareroId)
        assertEquals("luciaTest", s.nombre)
    }

    @Test
    fun parseSesion_pendiente() {
        val s = BarLanCliente.parseSesion(
            """{"admitido":false,"camareroId":"11111111-1111-4111-8111-111111111111"}""",
        )
        assertEquals(false, s!!.admitido)
        assertEquals("11111111-1111-4111-8111-111111111111", s.camareroId)
        assertEquals(null, s.nombre)
    }

    @Test
    fun parseSesion_sin_flag_o_basura() {
        assertEquals(null, BarLanCliente.parseSesion("""{"camareroId":"x"}"""))
        assertEquals(null, BarLanCliente.parseSesion("{"))
        assertEquals(null, BarLanCliente.parseSesion("no-json"))
    }

    @Test
    fun interpretarIniciar_404_es_nodo_viejo() {
        val r = BarLanCliente.interpretarIniciar(404, "")
        assertTrue(r.ok)
        assertTrue(r.nodoViejo)
        assertTrue(r.sesionActiva)
    }

    @Test
    fun interpretarIniciar_200_sin_campo_es_activa() {
        val r = BarLanCliente.interpretarIniciar(200, """{"admitido":true}""")
        assertTrue(r.ok)
        assertTrue(r.sesionActiva)
        assertFalse(r.nodoViejo)
    }

    @Test
    fun interpretarIniciar_sesionActiva_false() {
        val r = BarLanCliente.interpretarIniciar(200, """{"sesionActiva":false}""")
        assertTrue(r.ok)
        assertFalse(r.sesionActiva)
    }

    @Test
    fun interpretarIniciar_403_rechaza() {
        val r = BarLanCliente.interpretarIniciar(403, """{"detail":"no"}""")
        assertFalse(r.ok)
        assertFalse(r.sesionActiva)
    }

    @Test
    fun cuerpoQr_incluye_qr() {
        val o = com.google.gson.JsonParser.parseString(BarLanCliente.cuerpoQr("phid1:u1:c1:sig")).asJsonObject
        assertEquals("phid1:u1:c1:sig", o.get("qr").asString)
    }
}
