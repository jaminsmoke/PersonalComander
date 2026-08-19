package com.jaminsmoke.personalcomander.data.sesion

import com.jaminsmoke.personalcomander.data.ServidorDescubierto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanRadarTest {

    @Test
    fun aspecto_prioridad_error_luego_jornada() {
        assertEquals(LanLocalAspecto.ROJO, aspectoLan(errorConexion = true, admitido = true, jornada = true))
        assertEquals(LanLocalAspecto.VERDE, aspectoLan(errorConexion = false, admitido = true, jornada = true))
        assertEquals(LanLocalAspecto.AMARILLO, aspectoLan(errorConexion = false, admitido = true, jornada = false))
        assertEquals(LanLocalAspecto.APAGADO, aspectoLan(errorConexion = false, admitido = false, jornada = false))
    }

    @Test
    fun nombre_visible_nunca_es_host() {
        assertEquals("Casa Pepe", nombreLanVisible("  Casa Pepe "))
        assertEquals("", nombreLanVisible(null))
        assertEquals("", nombreLanVisible("  "))
        assertFalse(nombreLanVisible("Casa Pepe").contains("10.0.2.2"))
    }

    @Test
    fun extra_sin_bar_no_se_pinta() {
        assertEquals(null, aspectoSondeo(enScan = false, errorConexion = true, admitido = false, jornada = false))
        assertEquals(LanLocalAspecto.ROJO, aspectoSondeo(enScan = true, errorConexion = true, admitido = false, jornada = false))
        assertEquals(LanLocalAspecto.AMARILLO, aspectoSondeo(enScan = false, errorConexion = false, admitido = true, jornada = false))
    }

    @Test
    fun candidatos_anaden_emulador_sin_duplicar() {
        val scan = listOf(ServidorDescubierto("192.168.1.20", 8787))
        val todos = candidatosLan(scan)
        assertEquals(2, todos.size)
        assertTrue(todos.any { it.ip == EMULADOR_BAR_HOST && it.puerto == 8787 })
        val yaEsta = candidatosLan(listOf(ServidorDescubierto(EMULADOR_BAR_HOST, 8787)))
        assertEquals(1, yaEsta.size)
    }
}
