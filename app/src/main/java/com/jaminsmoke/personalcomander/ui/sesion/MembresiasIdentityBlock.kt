package com.jaminsmoke.personalcomander.ui.sesion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.sesion.MembresiaEstablecimiento

@Composable
fun MembresiasIdentityBlock(
    membresias: List<MembresiaEstablecimiento>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
