package com.jaminsmoke.personalcomander.ui.gestion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.sesion.InvitacionCamarero
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.ui.EmptyState
import com.jaminsmoke.personalcomander.ui.MenuScreen
import com.jaminsmoke.personalcomander.ui.components.BrandHeaderDensity
import com.jaminsmoke.personalcomander.ui.components.GlassCard
import com.jaminsmoke.personalcomander.ui.components.PcBrandHeader
import com.jaminsmoke.personalcomander.ui.components.PcPrimaryButton
import com.jaminsmoke.personalcomander.ui.components.PcSecondaryButton
import com.jaminsmoke.personalcomander.ui.components.PcSesionChip
import com.jaminsmoke.personalcomander.ui.components.PcTurnoIndicador
import com.jaminsmoke.personalcomander.ui.sesion.MembresiasIdentityBlock
import com.jaminsmoke.personalcomander.ui.sesion.SesionViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

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
            sesionViewModel = sesionViewModel,
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
    sesionViewModel: SesionViewModel,
) {
    val modo by sesionViewModel.modo.collectAsState()
    val invitaciones by sesionViewModel.invitaciones.collectAsState()
    val busy by sesionViewModel.busy.collectAsState()
    val mensaje by sesionViewModel.mensaje.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmacion by remember { mutableStateOf<Pair<InvitacionCamarero, Boolean>?>(null) }

    LaunchedEffect(modo) {
        if (modo !is ModoSesion.Local) sesionViewModel.cargarInvitaciones()
    }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            sesionViewModel.limpiarMensaje()
        }
    }

    confirmacion?.let { (inv, aceptar) ->
        AlertDialog(
            onDismissRequest = { confirmacion = null },
            title = {
                Text(
                    stringResource(
                        if (aceptar) R.string.gestion_invitaciones_aceptar_titulo
                        else R.string.gestion_invitaciones_rechazar_titulo,
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        if (aceptar) R.string.gestion_invitaciones_aceptar_cuerpo
                        else R.string.gestion_invitaciones_rechazar_cuerpo,
                        inv.establecimientoNombre,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmacion = null
                        if (aceptar) sesionViewModel.aceptarInvitacion(inv.id)
                        else sesionViewModel.rechazarInvitacion(inv.id)
                    },
                ) {
                    Text(
                        stringResource(
                            if (aceptar) R.string.gestion_invitaciones_aceptar
                            else R.string.gestion_invitaciones_rechazar,
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmacion = null }) {
                    Text(stringResource(R.string.menu_cancel))
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                actions = if (modo !is ModoSesion.Local) {
                    {
                        IconButton(
                            onClick = { sesionViewModel.cargarInvitaciones() },
                            enabled = !busy,
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.gestion_invitaciones_refrescar),
                            )
                        }
                    }
                } else {
                    null
                },
            )
        },
    ) { padding ->
        if (modo is ModoSesion.Local) {
            EmptyState(
                icon = Icons.Default.Email,
                title = stringResource(R.string.gestion_invitaciones_vacio_titulo),
                subtitle = stringResource(R.string.gestion_invitaciones_vacio_desc),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                actionLabel = stringResource(R.string.sesion_entrar),
                onAction = onOpenAuth,
            )
            return@Scaffold
        }

        val pendientes = invitaciones.filter { it.esPendiente }
        val anteriores = invitaciones.filter { !it.esPendiente }

        when {
            busy && invitaciones.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            invitaciones.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Email,
                    title = stringResource(R.string.gestion_invitaciones_vacio_titulo),
                    subtitle = stringResource(R.string.gestion_invitaciones_vacio_desc),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    actionLabel = stringResource(R.string.gestion_invitaciones_ver_qr),
                    onAction = onOpenPerfil,
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (pendientes.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.gestion_invitaciones_pendientes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        items(pendientes, key = { it.id }) { inv ->
                            InvitacionCard(
                                invitacion = inv,
                                enabled = !busy,
                                onAceptar = { confirmacion = inv to true },
                                onRechazar = { confirmacion = inv to false },
                            )
                        }
                    }
                    if (anteriores.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.gestion_invitaciones_anteriores),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.gestion_invitaciones_anteriores_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(anteriores, key = { it.id }) { inv ->
                            InvitacionCard(
                                invitacion = inv,
                                enabled = false,
                                onAceptar = null,
                                onRechazar = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitacionCard(
    invitacion: InvitacionCamarero,
    enabled: Boolean,
    onAceptar: (() -> Unit)?,
    onRechazar: (() -> Unit)?,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                invitacion.establecimientoNombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                rolInvitacionVisible(invitacion.rol),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (invitacion.esPendiente) {
                Text(
                    stringResource(R.string.gestion_invitaciones_caduca, fechaInvitacion(invitacion.expiraEn)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    estadoInvitacionVisible(invitacion.estado),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (onAceptar != null && onRechazar != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PcSecondaryButton(
                        text = stringResource(R.string.gestion_invitaciones_rechazar),
                        onClick = onRechazar,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                    PcPrimaryButton(
                        text = stringResource(R.string.gestion_invitaciones_aceptar),
                        onClick = onAceptar,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun rolInvitacionVisible(rol: String): String = when (rol.lowercase()) {
    "dueno", "dueño" -> stringResource(R.string.sesion_rol_dueno)
    "staff" -> stringResource(R.string.sesion_rol_staff)
    else -> rol
}

@Composable
private fun estadoInvitacionVisible(estado: String): String = when (estado.lowercase()) {
    "aceptada" -> stringResource(R.string.gestion_invitaciones_estado_aceptada)
    "rechazada" -> stringResource(R.string.gestion_invitaciones_estado_rechazada)
    "expirada" -> stringResource(R.string.gestion_invitaciones_estado_expirada)
    "revocada" -> stringResource(R.string.gestion_invitaciones_estado_revocada)
    else -> estado
}

private fun fechaInvitacion(iso: String): String {
    return try {
        OffsetDateTime.parse(iso).toLocalDate().format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()),
        )
    } catch (_: Exception) {
        iso
    }
}
