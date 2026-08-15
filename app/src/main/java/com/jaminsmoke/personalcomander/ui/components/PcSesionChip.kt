package com.jaminsmoke.personalcomander.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.data.sesion.etiquetaHeader
import com.jaminsmoke.personalcomander.data.sesion.etiquetaLocal
import com.jaminsmoke.personalcomander.data.sesion.perfil

@Composable
fun AvatarCamarero(
    iniciales: String,
    fotoBytes: ByteArray?,
    size: Dp,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(fotoBytes) {
        if (fotoBytes == null || fotoBytes.isEmpty()) null
        else BitmapFactory.decodeByteArray(fotoBytes, 0, fotoBytes.size)?.asImageBitmap()
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = iniciales,
                style = if (size >= 64.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
    }
}

@Composable
fun PcSesionChip(
    modo: ModoSesion,
    onEntrar: () -> Unit,
    onPerfil: () -> Unit,
    modifier: Modifier = Modifier,
    fotoBytes: ByteArray? = null,
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
            AvatarCamarero(
                iniciales = iniciales,
                fotoBytes = fotoBytes,
                size = 32.dp,
                contentDescription = stringResource(R.string.sesion_avatar_desc),
            )
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

@Composable
fun PcTurnoIndicador(
    modo: ModoSesion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val texto = when (val m = modo) {
        ModoSesion.Local, is ModoSesion.Identidad ->
            stringResource(R.string.sesion_turno_standalone)
        is ModoSesion.Establecimiento ->
            when {
                m.sesionTrabajo -> stringResource(R.string.sesion_turno_activo, m.etiquetaLocal())
                m.admitido -> stringResource(R.string.sesion_turno_en_nodo, m.etiquetaLocal())
                else -> stringResource(R.string.sesion_turno_nodo_pendiente, m.etiquetaLocal())
            }
    }
    val color = if (modo is ModoSesion.Establecimiento && modo.sesionTrabajo) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onClick)
                .semantics { contentDescription = texto },
        )
    }
}
