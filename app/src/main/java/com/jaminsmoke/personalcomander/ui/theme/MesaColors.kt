package com.jaminsmoke.personalcomander.ui.theme

import androidx.compose.ui.graphics.Color
import com.jaminsmoke.personalcomander.data.MesaEstado

// ─── Colores de estado de mesa (intuitivos, independientes del theme) ──────
// Solo estados actuales del modelo: LIBRE / OCUPADA / EN_COCINA.
// Reservas u otros estados → ítem kanban aparte (extender MesaEstado).

/** Libre — verde. */
val PcMesaLibreFill = Color(0xFFA5D6A7)
val PcMesaLibreAccent = Color(0xFF1B5E20)

/** Ocupada — amarillo. */
val PcMesaOcupadaFill = Color(0xFFFFE082)
val PcMesaOcupadaAccent = Color(0xFFF57F17)

/** En cocina — naranja (en progreso). */
val PcMesaEnCocinaFill = Color(0xFFFFB74D)
val PcMesaEnCocinaAccent = Color(0xFFE65100)

/** Texto/iconos sobre rellenos pastel de mesa. */
val PcMesaOnFill = Color(0xFF1A1A1A)

fun mesaStatusFill(estado: MesaEstado): Color = when (estado) {
    MesaEstado.LIBRE -> PcMesaLibreFill
    MesaEstado.OCUPADA -> PcMesaOcupadaFill
    MesaEstado.EN_COCINA -> PcMesaEnCocinaFill
}

fun mesaStatusAccent(estado: MesaEstado): Color = when (estado) {
    MesaEstado.LIBRE -> PcMesaLibreAccent
    MesaEstado.OCUPADA -> PcMesaOcupadaAccent
    MesaEstado.EN_COCINA -> PcMesaEnCocinaAccent
}

fun mesaStatusOnFill(): Color = PcMesaOnFill
