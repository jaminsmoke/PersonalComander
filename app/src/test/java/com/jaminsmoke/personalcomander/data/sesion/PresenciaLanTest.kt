package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresenciaLanTest {

    @Test
    fun encode_decode_redondo() {
        val a = PresenciaLan.Anuncio(establecimiento = "Casa Pepe", activo = true)
        val json = PresenciaLan.encode(a)
        assertEquals(
            """{"ph":"phbar1","role":"bar","establecimiento":"Casa Pepe","puerto":8787,"activo":true}""",
            json,
        )
        assertEquals(a, PresenciaLan.decode(json))
    }

    @Test
    fun decode_adios() {
        val json = PresenciaLan.encode(PresenciaLan.Anuncio(establecimiento = "Casa Pepe", activo = false))
        assertEquals(false, PresenciaLan.decode(json)?.activo)
    }

    @Test
    fun decode_rechaza_basura() {
        assertNull(PresenciaLan.decode("{}"))
        assertNull(PresenciaLan.decode("""{"ph":"otro","role":"bar","activo":true}"""))
        assertNull(PresenciaLan.decode("""{"ph":"phbar1","role":"tpv","activo":true}"""))
        assertNull(PresenciaLan.decode("no-json"))
    }
}
