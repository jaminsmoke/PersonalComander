package com.jaminsmoke.personalcomander.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jaminsmoke.personalcomander.data.MesaEstado

/** Color de acento / borde / badge según estado de mesa (tokens de marca). */
fun ColorScheme.mesaAccent(estado: MesaEstado): Color = when (estado) {
    MesaEstado.LIBRE -> tertiary
    MesaEstado.OCUPADA -> error
    MesaEstado.EN_COCINA -> secondary
}

/** Relleno de pieza en el board (token centralizado). */
fun mesaBoardFill(): Color = PcMesaFill

@Composable
fun mesaAccentColor(estado: MesaEstado): Color =
    MaterialTheme.colorScheme.mesaAccent(estado)
