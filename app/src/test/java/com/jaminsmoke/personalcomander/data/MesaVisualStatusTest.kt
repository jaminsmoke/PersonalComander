package com.jaminsmoke.personalcomander.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MesaVisualStatusTest {

    private fun mesa(
        estado: MesaEstado = MesaEstado.LIBRE,
        bloqueada: Boolean = false,
        reservaActivaId: Long? = null,
        comandaActivaId: Long? = null,
    ) = Mesa(
        id = 1,
        numero = 1,
        estado = estado,
        bloqueada = bloqueada,
        reservaActivaId = reservaActivaId,
        comandaActivaId = comandaActivaId,
    )

    @Test
    fun libre_sinHold() {
        assertEquals(MesaVisualStatus.LIBRE, mesaVisualStatus(mesa()))
    }

    @Test
    fun reservada_siPunteroYLibre() {
        assertEquals(
            MesaVisualStatus.RESERVADA,
            mesaVisualStatus(mesa(reservaActivaId = 9))
        )
    }

    @Test
    fun bloqueada_ganaSobreReserva() {
        assertEquals(
            MesaVisualStatus.BLOQUEADA,
            mesaVisualStatus(mesa(bloqueada = true, reservaActivaId = 9))
        )
    }

    @Test
    fun ocupada_ganaSobreBloqueoYReserva() {
        assertEquals(
            MesaVisualStatus.OCUPADA,
            mesaVisualStatus(
                mesa(
                    estado = MesaEstado.OCUPADA,
                    bloqueada = true,
                    reservaActivaId = 9,
                    comandaActivaId = 3,
                )
            )
        )
    }

    @Test
    fun enCocina_ganaSobreHold() {
        assertEquals(
            MesaVisualStatus.EN_COCINA,
            mesaVisualStatus(
                mesa(estado = MesaEstado.EN_COCINA, reservaActivaId = 2, comandaActivaId = 4)
            )
        )
    }
}
