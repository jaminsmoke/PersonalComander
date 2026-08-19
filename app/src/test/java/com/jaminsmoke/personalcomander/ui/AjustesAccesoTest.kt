package com.jaminsmoke.personalcomander.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AjustesAccesoTest {

    @Test
    fun fromNav_hub_o_vacio_es_nulo() {
        assertNull(AjustesAcceso.fromNav(null))
        assertNull(AjustesAcceso.fromNav(""))
        assertNull(AjustesAcceso.fromNav(AjustesAcceso.NAV_HUB))
        assertNull(AjustesAcceso.fromNav("desconocido"))
        assertNull(AjustesAcceso.fromNav("turno"))
        assertNull(AjustesAcceso.fromNav("avanzado"))
    }

    @Test
    fun fromNav_abre_tpv_y_copias() {
        assertEquals(AjustesAcceso.TPV, AjustesAcceso.fromNav("tpv"))
        assertEquals(AjustesAcceso.COPIAS, AjustesAcceso.fromNav("copias"))
    }
}
