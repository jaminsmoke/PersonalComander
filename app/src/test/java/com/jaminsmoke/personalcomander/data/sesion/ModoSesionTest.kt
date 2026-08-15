package com.jaminsmoke.personalcomander.data.sesion

import com.jaminsmoke.personalcomander.data.sesion.esActivo
import com.jaminsmoke.personalcomander.data.sesion.esStandalone
import com.jaminsmoke.personalcomander.data.sesion.etiquetaLocal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModoSesionTest {

    private val perfil = PerfilCamarero(
        id = "11111111-1111-1111-1111-111111111111",
        nombre = "Ana",
        apellidos = "García",
        email = "ana@example.com",
    )

    @Test
    fun local_cartaEditable() {
        assertTrue(ModoSesion.Local.cartaEditable)
        assertFalse(ModoSesion.Local.puedeAdherirseABar)
        assertNull(ModoSesion.Local.etiquetaHeader())
    }

    @Test
    fun identidad_cartaEditable_y_puedeAdherirse() {
        val modo = ModoSesion.Identidad(perfil, "phid1:a:b:sig", "tok")
        assertTrue(modo.cartaEditable)
        assertTrue(modo.puedeAdherirseABar)
        assertEquals("Ana", modo.etiquetaHeader())
        assertEquals("Ana", perfil.mote)
        assertEquals("AG", perfil.iniciales)
        assertEquals("tok", modo.token)
        assertEquals("phid1:a:b:sig", modo.qr)
        assertFalse(modo.credencialRevocada)
    }

    @Test
    fun identidad_qr_revocada_no_adhiere() {
        val modo = ModoSesion.Identidad(perfil, qr = null, token = "tok")
        assertTrue(modo.credencialRevocada)
        assertFalse(modo.puedeAdherirseABar)
        assertNull(modo.qr)
        assertTrue(modo.cartaEditable)
    }

    @Test
    fun establecimiento_pendiente_sigue_editable() {
        val modo = ModoSesion.Establecimiento(
            perfil = perfil,
            qr = "phid1:a:b:sig",
            token = "tok",
            barHost = "192.168.1.10",
            admitido = false,
        )
        assertTrue(modo.cartaEditable)
        assertTrue(modo.mapaEditable)
        assertTrue(modo.puedeAdherirseABar)
        assertEquals("192.168.1.10:8787", modo.etiquetaLocal())
        assertTrue(modo.esActivo)
        assertFalse(modo.esStandalone)
    }

    @Test
    fun establecimiento_admitido_no_editable() {
        val modo = ModoSesion.Establecimiento(
            perfil = perfil,
            qr = "phid1:a:b:sig",
            token = "tok",
            barHost = "192.168.1.10",
            admitido = true,
            nombreEstablecimiento = "Casa Pepe",
        )
        assertFalse(modo.cartaEditable)
        assertFalse(modo.mapaEditable)
        assertEquals("Casa Pepe", modo.etiquetaLocal())
    }

    @Test
    fun standalone_es_local_o_identidad() {
        assertTrue(ModoSesion.Local.esStandalone)
        assertFalse(ModoSesion.Local.esActivo)
        val identidad = ModoSesion.Identidad(perfil, "phid1:a:b:sig", "tok")
        assertTrue(identidad.esStandalone)
        assertFalse(identidad.esActivo)
    }

    @Test
    fun mote_usa_nick_si_existe() {
        val conNick = perfil.copy(nick = "Lucía")
        val modo = ModoSesion.Identidad(conNick, "phid1:a:b:sig", "tok")
        assertEquals("Lucía", conNick.mote)
        assertEquals("Lucía", modo.etiquetaHeader())
        assertEquals("Ana García", conNick.nombreCompleto)
    }

    @Test
    fun origen_default_es_real() {
        assertEquals(DataOrigin.Real, perfil.origen)
        assertEquals(DataOrigin.Test, perfil.copy(dataOrigin = DataOrigin.Test).origen)
        assertEquals(DataOrigin.Real, perfil.copy(dataOrigin = null).origen)
    }
}
