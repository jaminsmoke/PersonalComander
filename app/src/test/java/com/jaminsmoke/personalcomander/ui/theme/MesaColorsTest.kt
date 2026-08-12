package com.jaminsmoke.personalcomander.ui.theme

import com.jaminsmoke.personalcomander.data.MesaEstado
import org.junit.Assert.assertEquals
import org.junit.Test

class MesaColorsTest {

    @Test
    fun mesaStatus_libre_verde() {
        assertEquals(PcMesaLibreFill, mesaStatusFill(MesaEstado.LIBRE))
        assertEquals(PcMesaLibreAccent, mesaStatusAccent(MesaEstado.LIBRE))
    }

    @Test
    fun mesaStatus_ocupada_amarillo() {
        assertEquals(PcMesaOcupadaFill, mesaStatusFill(MesaEstado.OCUPADA))
        assertEquals(PcMesaOcupadaAccent, mesaStatusAccent(MesaEstado.OCUPADA))
    }

    @Test
    fun mesaStatus_enCocina_naranja() {
        assertEquals(PcMesaEnCocinaFill, mesaStatusFill(MesaEstado.EN_COCINA))
        assertEquals(PcMesaEnCocinaAccent, mesaStatusAccent(MesaEstado.EN_COCINA))
    }

    @Test
    fun mesaStatusOnFill_oscuroParaPasteles() {
        assertEquals(PcMesaOnFill, mesaStatusOnFill())
    }
}
