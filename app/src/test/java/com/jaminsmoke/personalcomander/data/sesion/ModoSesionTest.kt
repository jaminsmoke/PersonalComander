package com.jaminsmoke.personalcomander.data.sesion

import com.jaminsmoke.personalcomander.data.sesion.esActivo
import com.jaminsmoke.personalcomander.data.sesion.esJornada
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
        assertEquals(VisibleOtrosEstablecimientos.Nunca, perfil.visibleDirectorio)
        assertEquals("tok", modo.token)
        assertEquals("phid1:a:b:sig", modo.qr)
        assertNull(modo.fichaUrl)
        assertEquals("phid1:a:b:sig", modo.qrVisible)
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
        assertFalse(modo.esJornada)
        assertFalse(modo.esStandalone)
    }

    @Test
    fun establecimiento_jornada_es_permiso_de_ronda() {
        val ligado = ModoSesion.Establecimiento(
            perfil = perfil,
            qr = "phid1:a:b:sig",
            token = "tok",
            barHost = "192.168.1.10",
            admitido = true,
            nombreEstablecimiento = "Casa Pepe",
        )
        assertTrue(ligado.esActivo)
        assertFalse(ligado.esJornada)
        val jornada = ligado.copy(sesionTrabajo = true)
        assertTrue(jornada.esJornada)
        assertTrue(jornada.esActivo)
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
    fun qrVisible_usa_ficha_http_y_no_inventa() {
        assertEquals("phid1:a:b:sig", qrVisibleDe("phid1:a:b:sig", null))
        assertEquals(
            "https://ficha.example/ficha?qr=phid1:a:b:sig",
            qrVisibleDe("phid1:a:b:sig", "https://ficha.example/ficha?qr=phid1:a:b:sig"),
        )
        assertEquals("phid1:a:b:sig", qrVisibleDe("phid1:a:b:sig", "javascript:alert(1)"))
        assertNull(qrVisibleDe(null, "   "))
    }

    @Test
    fun normalizarFichaUrl_legacy_ficha_a_canonica() {
        val qr = "phid1:a:b:sig"
        assertEquals(
            "https://web.camareros.siberia.solutions/camareros?qr=$qr",
            normalizarFichaUrl("https://ficha.siberia.solutions/ficha?qr=$qr"),
        )
        assertEquals(
            "https://web.camareros.siberia.solutions/camareros?qr=$qr",
            normalizarFichaUrl("http://ficha.siberia.solutions/camareros?qr=$qr"),
        )
        assertEquals(
            "https://web.camareros.siberia.solutions/camareros?qr=$qr",
            normalizarFichaUrl("https://web.camareros.siberia.solutions/camareros?qr=$qr"),
        )
        assertEquals(
            "https://ficha.example/ficha?qr=$qr",
            normalizarFichaUrl("https://ficha.example/ficha?qr=$qr"),
        )
        assertNull(normalizarFichaUrl(null))
        assertNull(normalizarFichaUrl("  "))
        assertEquals(
            "https://ficha.siberia.solutions/negocio?slug=x",
            normalizarFichaUrl("https://ficha.siberia.solutions/negocio?slug=x"),
        )
    }

    @Test
    fun qrVisible_reescribe_ficha_legacy() {
        val qr = "phid1:a:b:sig"
        val canonica = "https://web.camareros.siberia.solutions/camareros?qr=$qr"
        assertEquals(canonica, qrVisibleDe(qr, "https://ficha.siberia.solutions/ficha?qr=$qr"))
        val modo: ModoSesion = ModoSesion.Identidad(perfil, qr, "tok", "https://ficha.siberia.solutions/ficha?qr=$qr")
        assertEquals(canonica, modo.fichaUrl)
        assertEquals(canonica, modo.qrVisible)
    }

    @Test
    fun origen_default_es_real() {
        assertEquals(DataOrigin.Real, perfil.origen)
        assertEquals(DataOrigin.Test, perfil.copy(dataOrigin = DataOrigin.Test).origen)
        assertEquals(DataOrigin.Real, perfil.copy(dataOrigin = null).origen)
    }
}
