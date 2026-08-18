package com.jaminsmoke.personalcomander.ui.sesion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.sesion.EstadoLaboral
import com.jaminsmoke.personalcomander.data.sesion.MembresiaEstablecimiento
import com.jaminsmoke.personalcomander.data.sesion.estadoLaboral

@Composable
fun MembresiasIdentityBlock(
    membresias: List<MembresiaEstablecimiento>,
    modifier: Modifier = Modifier,
) {
    val estado = estadoLaboral(membresias)
    val libre = estado is EstadoLaboral.Libre
    val etiqueta = when (estado) {
        EstadoLaboral.Libre -> stringResource(R.string.sesion_estado_libre)
        is EstadoLaboral.Trabajador -> stringResource(
            R.string.sesion_estado_trabajador,
            estado.nombres.joinToString(" · "),
        )
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.sesion_membresias_titulo),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(R.string.sesion_membresias_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = if (libre) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (libre) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        if (membresias.isEmpty()) {
            Text(
                stringResource(R.string.sesion_membresias_vacio),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            membresias.forEach { m ->
                Text(
                    stringResource(R.string.sesion_membresia_linea, m.nombre, rolVisible(m.rol)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun rolVisible(rol: String): String = when (rol.lowercase()) {
    "dueno", "dueño" -> stringResource(R.string.sesion_rol_dueno)
    "staff" -> stringResource(R.string.sesion_rol_staff)
    else -> rol
}
