package com.jaminsmoke.personalcomander.ui.gestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestionAccesoTest {

    @Test
    fun fromNav_hub_o_vacio_es_nulo() {
        assertNull(GestionAcceso.fromNav(null))
        assertNull(GestionAcceso.fromNav(""))
        assertNull(GestionAcceso.fromNav(GestionAcceso.NAV_HUB))
        assertNull(GestionAcceso.fromNav("desconocido"))
    }

    @Test
    fun fromNav_abre_carta_locales_e_invitaciones() {
        assertEquals(GestionAcceso.CARTA, GestionAcceso.fromNav("carta"))
        assertEquals(GestionAcceso.CARTA, GestionAcceso.fromNav("Carta"))
        assertEquals(GestionAcceso.LOCALES, GestionAcceso.fromNav("locales"))
        assertEquals(GestionAcceso.INVITACIONES, GestionAcceso.fromNav("invitaciones"))
    }
}
