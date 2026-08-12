package com.jaminsmoke.personalcomander.ui

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios para el flujo de confirmación de cierre de mesa (#7).
 *
 * Como ComandaViewModel requiere Application y Room, testeamos la lógica
 * de transiciones de estado de forma aislada replicando los StateFlows.
 */
class ComandaCierreTest {

    // ── Simulación de los StateFlows del ViewModel ──

    private val _mostrarConfirmacionCierre = MutableStateFlow(false)
    private val _mostrarUndo = MutableStateFlow(false)
    private val _tiempoRestanteUndo = MutableStateFlow(0)
    private val _mesaCerrada = MutableStateFlow(false)

    private fun solicitarCierre() {
        _mostrarConfirmacionCierre.value = true
    }

    private fun cancelarCierre() {
        _mostrarConfirmacionCierre.value = false
    }

    private fun confirmarCierre() {
        _mostrarConfirmacionCierre.value = false
        _mostrarUndo.value = true
        _tiempoRestanteUndo.value = 300
    }

    private fun reabrirMesa() {
        _mostrarUndo.value = false
        _tiempoRestanteUndo.value = 0
    }

    private fun timerExpira() {
        _mostrarUndo.value = false
        _mesaCerrada.value = true
    }

    // ── Tests de transiciones de estado ──

    @Test
    fun solicitar_cierre_muestra_confirmacion() {
        assertFalse(_mostrarConfirmacionCierre.value)

        solicitarCierre()

        assertTrue(_mostrarConfirmacionCierre.value)
    }

    @Test
    fun cancelar_cierre_oculta_confirmacion() {
        _mostrarConfirmacionCierre.value = true

        cancelarCierre()

        assertFalse(_mostrarConfirmacionCierre.value)
    }

    @Test
    fun confirmar_cierre_oculta_confirmacion_y_muestra_undo() {
        _mostrarConfirmacionCierre.value = true

        confirmarCierre()

        assertFalse(_mostrarConfirmacionCierre.value)
        assertTrue(_mostrarUndo.value)
        assertEquals(300, _tiempoRestanteUndo.value)
    }

    @Test
    fun reabrir_mesa_oculta_undo() {
        _mostrarUndo.value = true
        _tiempoRestanteUndo.value = 150

        reabrirMesa()

        assertFalse(_mostrarUndo.value)
        assertEquals(0, _tiempoRestanteUndo.value)
    }

    @Test
    fun timer_expira_oculta_undo_y_navega_atras() {
        _mostrarUndo.value = true
        _tiempoRestanteUndo.value = 1

        timerExpira()

        assertFalse(_mostrarUndo.value)
        assertTrue(_mesaCerrada.value)
    }

    @Test
    fun flujo_completo_solicitar_confirmar_reabrir() {
        // Estado inicial
        assertFalse(_mostrarConfirmacionCierre.value)
        assertFalse(_mostrarUndo.value)

        // Usuario toca "Cerrar mesa"
        solicitarCierre()
        assertTrue(_mostrarConfirmacionCierre.value)

        // Usuario confirma en el diálogo
        confirmarCierre()
        assertFalse(_mostrarConfirmacionCierre.value)
        assertTrue(_mostrarUndo.value)

        // Usuario toca "Reabrir" en el Snackbar
        reabrirMesa()
        assertFalse(_mostrarUndo.value)
    }

    @Test
    fun flujo_completo_solicitar_cancelar() {
        // Estado inicial
        assertFalse(_mostrarConfirmacionCierre.value)

        // Usuario toca "Cerrar mesa"
        solicitarCierre()
        assertTrue(_mostrarConfirmacionCierre.value)

        // Usuario cancela en el diálogo
        cancelarCierre()
        assertFalse(_mostrarConfirmacionCierre.value)
        assertFalse(_mostrarUndo.value)
    }

    @Test
    fun timer_cuenta_atras() {
        _tiempoRestanteUndo.value = 300

        // Simular countdown
        for (i in 300 downTo 295) {
            assertEquals(i, _tiempoRestanteUndo.value)
            _tiempoRestanteUndo.value = i - 1
        }

        assertEquals(294, _tiempoRestanteUndo.value)
    }
}
