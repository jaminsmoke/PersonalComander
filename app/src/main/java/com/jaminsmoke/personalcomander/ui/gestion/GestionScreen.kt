package com.jaminsmoke.personalcomander.ui.gestion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.ui.EmptyState
import com.jaminsmoke.personalcomander.ui.MenuScreen
import com.jaminsmoke.personalcomander.ui.components.BrandHeaderDensity
import com.jaminsmoke.personalcomander.ui.components.PcBrandHeader
import com.jaminsmoke.personalcomander.ui.components.PcPrimaryButton
import com.jaminsmoke.personalcomander.ui.components.PcSesionChip
import com.jaminsmoke.personalcomander.ui.components.PcTurnoIndicador
import com.jaminsmoke.personalcomander.ui.sesion.MembresiasIdentityBlock
import com.jaminsmoke.personalcomander.ui.sesion.SesionViewModel

private const val HUB_COLUMNAS = 2

@Composable
fun GestionScreen(
    abrir: String? = null,
    onOpenAjustes: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenPerfil: () -> Unit,
    sesionViewModel: SesionViewModel = viewModel(),
) {
    val modo by sesionViewModel.modo.collectAsState()
    val fotoBytes by sesionViewModel.foto.collectAsState()
    var seleccion by rememberSaveable(abrir) {
        mutableStateOf(GestionAcceso.fromNav(abrir)?.name)
    }
    val acceso = seleccion?.let { runCatching { GestionAcceso.valueOf(it) }.getOrNull() }

    when (acceso) {
        null -> GestionHub(
            onSeleccionar = { seleccion = it.name },
            modo = modo,
            fotoBytes = fotoBytes,
            onOpenAuth = onOpenAuth,
            onOpenPerfil = onOpenPerfil,
        )
        GestionAcceso.CARTA -> MenuScreen(onBack = { seleccion = null })
        GestionAcceso.LOCALES -> LocalesGestionScreen(
            onBack = { seleccion = null },
            onOpenAjustes = onOpenAjustes,
            onOpenAuth = onOpenAuth,
            sesionViewModel = sesionViewModel,
        )
        GestionAcceso.INVITACIONES -> InvitacionesGestionScreen(
            onBack = { seleccion = null },
            onOpenAuth = onOpenAuth,
            onOpenPerfil = onOpenPerfil,
            modo = modo,
        )
    }
}

@Composable
private fun GestionHub(
    onSeleccionar: (GestionAcceso) -> Unit,
    modo: ModoSesion,
    fotoBytes: ByteArray?,
    onOpenAuth: () -> Unit,
    onOpenPerfil: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PcBrandHeader(
                title = stringResource(R.string.gestion_titulo),
                density = BrandHeaderDensity.Compact,
                actions = {
                    PcSesionChip(
                        modo = modo,
                        fotoBytes = fotoBytes,
                        onEntrar = onOpenAuth,
                        onPerfil = onOpenPerfil,
                    )
                },
            )
        },
    ) { padding ->
        val filas = GestionAcceso.entries.chunked(HUB_COLUMNAS)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            filas.forEach { fila ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    fila.forEach { acceso ->
                        GestionTile(
                            acceso = acceso,
                            onClick = { onSeleccionar(acceso) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(HUB_COLUMNAS - fila.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GestionTile(
    acceso: GestionAcceso,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = acceso.icon,
                contentDescription = stringResource(acceso.labelRes),
                modifier = Modifier.size(40.dp),
                tint = scheme.secondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(acceso.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LocalesGestionScreen(
    onBack: () -> Unit,
    onOpenAjustes: () -> Unit,
    onOpenAuth: () -> Unit,
    sesionViewModel: SesionViewModel,
) {
    val modo by sesionViewModel.modo.collectAsState()
    val membresias by sesionViewModel.membresias.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PcBrandHeader(
                title = stringResource(R.string.gestion_locales),
                density = BrandHeaderDensity.Compact,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PcTurnoIndicador(
                    modo = modo,
                    onClick = {
                        if (modo is ModoSesion.Local) onOpenAuth() else onOpenAjustes()
                    },
                )
            }
            item { MembresiasIdentityBlock(membresias = membresias) }
            item {
                if (modo is ModoSesion.Local) {
                    PcPrimaryButton(
                        text = stringResource(R.string.sesion_entrar),
                        onClick = onOpenAuth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    PcPrimaryButton(
                        text = stringResource(R.string.gestion_locales_ir_turno),
                        onClick = onOpenAjustes,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitacionesGestionScreen(
    onBack: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenPerfil: () -> Unit,
    modo: ModoSesion,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PcBrandHeader(
                title = stringResource(R.string.gestion_invitaciones),
                density = BrandHeaderDensity.Compact,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        EmptyState(
            icon = Icons.Default.Email,
            title = stringResource(R.string.gestion_invitaciones_vacio_titulo),
            subtitle = stringResource(R.string.gestion_invitaciones_vacio_desc),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            actionLabel = stringResource(
                if (modo is ModoSesion.Local) R.string.sesion_entrar
                else R.string.gestion_invitaciones_ver_qr,
            ),
            onAction = if (modo is ModoSesion.Local) onOpenAuth else onOpenPerfil,
        )
    }
}
