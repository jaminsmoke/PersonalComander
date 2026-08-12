package com.jaminsmoke.personalcomander.ui.theme

import com.jaminsmoke.personalcomander.data.MesaVisualStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class MesaColorsTest {

    @Test
    fun mesaStatus_libre_verde() {
        assertEquals(PcMesaLibreFill, mesaStatusFill(MesaVisualStatus.LIBRE))
        assertEquals(PcMesaLibreAccent, mesaStatusAccent(MesaVisualStatus.LIBRE))
    }

    @Test
    fun mesaStatus_ocupada_amarillo() {
        assertEquals(PcMesaOcupadaFill, mesaStatusFill(MesaVisualStatus.OCUPADA))
        assertEquals(PcMesaOcupadaAccent, mesaStatusAccent(MesaVisualStatus.OCUPADA))
    }

    @Test
    fun mesaStatus_enCocina_naranja() {
        assertEquals(PcMesaEnCocinaFill, mesaStatusFill(MesaVisualStatus.EN_COCINA))
        assertEquals(PcMesaEnCocinaAccent, mesaStatusAccent(MesaVisualStatus.EN_COCINA))
    }

    @Test
    fun mesaStatus_reservada_morado() {
        assertEquals(PcMesaReservadaFill, mesaStatusFill(MesaVisualStatus.RESERVADA))
        assertEquals(PcMesaReservadaAccent, mesaStatusAccent(MesaVisualStatus.RESERVADA))
    }

    @Test
    fun mesaStatus_bloqueada_rojo() {
        assertEquals(PcMesaBloqueadaFill, mesaStatusFill(MesaVisualStatus.BLOQUEADA))
        assertEquals(PcMesaBloqueadaAccent, mesaStatusAccent(MesaVisualStatus.BLOQUEADA))
    }

    @Test
    fun mesaStatusOnFill_oscuroParaPasteles() {
        assertEquals(PcMesaOnFill, mesaStatusOnFill())
    }
}
