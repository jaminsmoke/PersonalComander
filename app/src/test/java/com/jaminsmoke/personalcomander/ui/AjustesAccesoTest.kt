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
    }

    @Test
    fun fromNav_abre_turno_tpv_copias_y_avanzado() {
        assertEquals(AjustesAcceso.TURNO, AjustesAcceso.fromNav("turno"))
        assertEquals(AjustesAcceso.TURNO, AjustesAcceso.fromNav("Turno"))
        assertEquals(AjustesAcceso.TPV, AjustesAcceso.fromNav("tpv"))
        assertEquals(AjustesAcceso.COPIAS, AjustesAcceso.fromNav("copias"))
        assertEquals(AjustesAcceso.AVANZADO, AjustesAcceso.fromNav("avanzado"))
    }
}
