package com.jaminsmoke.personalcomander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.data.sesion.etiquetaHeader
import com.jaminsmoke.personalcomander.data.sesion.perfil

@Composable
fun PcSesionChip(
    modo: ModoSesion,
    onEntrar: () -> Unit,
    onPerfil: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (modo is ModoSesion.Local) {
        TextButton(onClick = onEntrar, modifier = modifier) {
            Text(stringResource(R.string.sesion_entrar))
        }
        return
    }
    val iniciales = modo.perfil?.iniciales ?: "?"
    val nombre = modo.etiquetaHeader().orEmpty()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onPerfil)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = iniciales,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondary,
                )
            }
            Text(
                text = nombre,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
            )
        }
    }
}
