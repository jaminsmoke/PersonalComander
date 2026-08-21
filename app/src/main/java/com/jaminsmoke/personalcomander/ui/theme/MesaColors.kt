package com.jaminsmoke.personalcomander.ui.theme

import androidx.compose.ui.graphics.Color
import com.jaminsmoke.personalcomander.data.MesaVisualStatus
import com.jaminsmoke.personalcomander.data.ZonaColor

// ─── Colores de estado visual de mesa (intuitivos, independientes del theme) ─

/** Libre — verde. */
val PcMesaLibreFill = Color(0xFFA5D6A7)
val PcMesaLibreAccent = Color(0xFF1B5E20)

/** Ocupada — amarillo. */
val PcMesaOcupadaFill = Color(0xFFFFE082)
val PcMesaOcupadaAccent = Color(0xFFF57F17)

/** En cocina — naranja (en progreso). */
val PcMesaEnCocinaFill = Color(0xFFFFB74D)
val PcMesaEnCocinaAccent = Color(0xFFE65100)

/** Reservada — morado. */
val PcMesaReservadaFill = Color(0xFFCE93D8)
val PcMesaReservadaAccent = Color(0xFF6A1B9A)

/** Bloqueada — rojo. */
val PcMesaBloqueadaFill = Color(0xFFEF9A9A)
val PcMesaBloqueadaAccent = Color(0xFFB71C1C)

/** Texto/iconos sobre rellenos pastel de mesa. */
val PcMesaOnFill = Color(0xFF1A1A1A)

fun mesaStatusFill(status: MesaVisualStatus): Color = when (status) {
    MesaVisualStatus.LIBRE -> PcMesaLibreFill
    MesaVisualStatus.OCUPADA -> PcMesaOcupadaFill
    MesaVisualStatus.EN_COCINA -> PcMesaEnCocinaFill
    MesaVisualStatus.RESERVADA -> PcMesaReservadaFill
    MesaVisualStatus.BLOQUEADA -> PcMesaBloqueadaFill
}

fun mesaStatusAccent(status: MesaVisualStatus): Color = when (status) {
    MesaVisualStatus.LIBRE -> PcMesaLibreAccent
    MesaVisualStatus.OCUPADA -> PcMesaOcupadaAccent
    MesaVisualStatus.EN_COCINA -> PcMesaEnCocinaAccent
    MesaVisualStatus.RESERVADA -> PcMesaReservadaAccent
    MesaVisualStatus.BLOQUEADA -> PcMesaBloqueadaAccent
}

fun mesaStatusOnFill(): Color = PcMesaOnFill

// ─── Territorios de sala (paleta fija, tokens de espacio físico; mismo contrato que Bar) ─
// Fill semi-transparente + borde más opaco. Se mapean por nombre de `ZonaColor`.

val PcZonaAzul = Color(0xFF3B82F6)
val PcZonaVerde = Color(0xFF22C55E)
val PcZonaAmarillo = Color(0xFFEAB308)
val PcZonaNaranja = Color(0xFFF97316)
val PcZonaMorado = Color(0xFF8B5CF6)
val PcZonaRojo = Color(0xFFEF4444)

fun zonaColorFill(color: ZonaColor): Color = when (color) {
    ZonaColor.AZUL -> PcZonaAzul.copy(alpha = 0.22f)
    ZonaColor.VERDE -> PcZonaVerde.copy(alpha = 0.20f)
    ZonaColor.AMARILLO -> PcZonaAmarillo.copy(alpha = 0.24f)
    ZonaColor.NARANJA -> PcZonaNaranja.copy(alpha = 0.22f)
    ZonaColor.MORADO -> PcZonaMorado.copy(alpha = 0.22f)
    ZonaColor.ROJO -> PcZonaRojo.copy(alpha = 0.20f)
}

fun zonaColorAccent(color: ZonaColor): Color = when (color) {
    ZonaColor.AZUL -> PcZonaAzul.copy(alpha = 0.85f)
    ZonaColor.VERDE -> PcZonaVerde.copy(alpha = 0.85f)
    ZonaColor.AMARILLO -> PcZonaAmarillo.copy(alpha = 0.9f)
    ZonaColor.NARANJA -> PcZonaNaranja.copy(alpha = 0.85f)
    ZonaColor.MORADO -> PcZonaMorado.copy(alpha = 0.85f)
    ZonaColor.ROJO -> PcZonaRojo.copy(alpha = 0.85f)
}
