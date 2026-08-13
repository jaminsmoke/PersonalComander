package com.jaminsmoke.personalcomander.ui.sesion

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.data.sesion.qr
import com.jaminsmoke.personalcomander.ui.components.BrandHeaderDensity
import com.jaminsmoke.personalcomander.ui.components.GlassCard
import com.jaminsmoke.personalcomander.ui.components.PcBrandHeader
import com.jaminsmoke.personalcomander.ui.components.PcSecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onBack: () -> Unit,
    viewModel: SesionViewModel = viewModel(),
) {
    val modo by viewModel.modo.collectAsState()
    val perfil = when (modo) {
        is ModoSesion.Identidad -> (modo as ModoSesion.Identidad).perfil
        is ModoSesion.Sala -> (modo as ModoSesion.Sala).perfil
        ModoSesion.Local -> null
    }
    val qr = modo.qr
    val qrBmp = remember(qr) { qr?.let { qrImageBitmap(it) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PcBrandHeader(
                title = stringResource(R.string.sesion_perfil_title),
                density = BrandHeaderDensity.Compact,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (perfil == null) {
                Text(stringResource(R.string.sesion_sin_cuenta), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(perfil.nombreCompleto, style = MaterialTheme.typography.headlineSmall)
                Text(perfil.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                perfil.telefono?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when (val actual = modo) {
                    is ModoSesion.Sala -> Text(
                        text = stringResource(
                            if (actual.admitido) R.string.sesion_modo_sala_admitido else R.string.sesion_modo_sala_pendiente,
                            "${actual.barHost}:${actual.barPuerto}",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    is ModoSesion.Identidad -> Text(
                        text = stringResource(R.string.sesion_modo_identidad),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    else -> Unit
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.sesion_qr_titulo), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.sesion_qr_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (qrBmp != null) {
                            Image(
                                bitmap = qrBmp,
                                contentDescription = stringResource(R.string.sesion_qr_desc_img),
                                modifier = Modifier.size(240.dp),
                            )
                        }
                    }
                }
                if (modo is ModoSesion.Sala) {
                    PcSecondaryButton(
                        text = stringResource(R.string.sesion_desconectar_bar),
                        onClick = { viewModel.desconectarBar() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                PcSecondaryButton(
                    text = stringResource(R.string.sesion_cerrar),
                    onClick = {
                        viewModel.cerrarSesion()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
