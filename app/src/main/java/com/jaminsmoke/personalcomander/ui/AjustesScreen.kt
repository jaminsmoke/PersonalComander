package com.jaminsmoke.personalcomander.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.jaminsmoke.personalcomander.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.data.ServidorDescubierto
import com.jaminsmoke.personalcomander.data.TpvPrograma
import com.jaminsmoke.personalcomander.data.sesion.BarLanCliente
import com.jaminsmoke.personalcomander.data.sesion.MembresiaEstablecimiento
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.data.sesion.cartaEditable
import com.jaminsmoke.personalcomander.data.sesion.etiquetaLocal
import com.jaminsmoke.personalcomander.ui.components.BrandHeaderDensity
import com.jaminsmoke.personalcomander.ui.components.GlassCard
import com.jaminsmoke.personalcomander.ui.components.PcBrandHeader
import com.jaminsmoke.personalcomander.ui.components.PcPrimaryButton
import com.jaminsmoke.personalcomander.ui.components.PcSecondaryButton
import com.jaminsmoke.personalcomander.ui.components.PcSesionChip
import com.jaminsmoke.personalcomander.ui.sesion.MembresiasIdentityBlock
import com.jaminsmoke.personalcomander.ui.sesion.SesionViewModel

private const val HUB_COLUMNAS = 2

@Composable
fun AjustesScreen(
    abrir: String? = null,
    onOpenAuth: () -> Unit = {},
    onOpenPerfil: () -> Unit = {},
) {
    var seleccion by rememberSaveable(abrir) {
        mutableStateOf(AjustesAcceso.fromNav(abrir)?.name)
    }
    val acceso = seleccion?.let { runCatching { AjustesAcceso.valueOf(it) }.getOrNull() }

    when (acceso) {
        null -> AjustesHub(
            onSeleccionar = { seleccion = it.name },
            onOpenAuth = onOpenAuth,
            onOpenPerfil = onOpenPerfil,
        )
        AjustesAcceso.TURNO -> TurnoAjustesScreen(
            onBack = { seleccion = null },
            onOpenAuth = onOpenAuth,
        )
        AjustesAcceso.TPV -> TpvAjustesScreen(onBack = { seleccion = null })
        AjustesAcceso.COPIAS -> CopiasAjustesScreen(onBack = { seleccion = null })
        AjustesAcceso.AVANZADO -> AvanzadoAjustesScreen(
            onBack = { seleccion = null },
            onOpenAuth = onOpenAuth,
            onOpenPerfil = onOpenPerfil,
        )
    }
}

@Composable
private fun AjustesHub(
    onSeleccionar: (AjustesAcceso) -> Unit,
    onOpenAuth: () -> Unit,
    onOpenPerfil: () -> Unit,
    sesionViewModel: SesionViewModel = viewModel(),
) {
    val modo by sesionViewModel.modo.collectAsState()
    val fotoSesion by sesionViewModel.foto.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PcBrandHeader(
                title = stringResource(R.string.ajustes_title),
                density = BrandHeaderDensity.Compact,
                actions = {
                    PcSesionChip(
                        modo = modo,
                        fotoBytes = fotoSesion,
                        onEntrar = onOpenAuth,
                        onPerfil = onOpenPerfil,
                    )
                },
            )
        },
    ) { padding ->
        val filas = AjustesAcceso.entries.chunked(HUB_COLUMNAS)
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
                        AjustesTile(
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
private fun AjustesTile(
    acceso: AjustesAcceso,
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
private fun AjustesSeccionScaffold(
    title: String,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PcBrandHeader(
                title = title,
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content,
    )
}

@Composable
private fun TurnoAjustesScreen(
    onBack: () -> Unit,
    onOpenAuth: () -> Unit,
    sesionViewModel: SesionViewModel = viewModel(),
) {
    val sesionMensaje by sesionViewModel.mensaje.collectAsState()
    val modo by sesionViewModel.modo.collectAsState()
    val bares by sesionViewModel.bares.collectAsState()
    val escaneandoBares by sesionViewModel.escaneando.collectAsState()
    val busySesion by sesionViewModel.busy.collectAsState()
    val membresias by sesionViewModel.membresias.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sesionMensaje) {
        sesionMensaje?.let {
            snackbarHostState.showSnackbar(it)
            sesionViewModel.limpiarMensaje()
        }
    }

    AjustesSeccionScaffold(
        title = stringResource(R.string.sesion_sala_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.sesion_sala_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                SalaCard(
                    modo = modo,
                    membresias = membresias,
                    bares = bares,
                    escaneando = escaneandoBares,
                    busy = busySesion,
                    onBuscar = sesionViewModel::buscarBares,
                    onConectar = sesionViewModel::conectarBar,
                    onDesconectar = sesionViewModel::desconectarBar,
                    onIniciarJornada = sesionViewModel::iniciarJornada,
                    onCortarJornada = sesionViewModel::cortarJornada,
                    onIrLogin = onOpenAuth,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TpvAjustesScreen(
    onBack: () -> Unit,
    viewModel: AjustesViewModel = viewModel(),
    sesionViewModel: SesionViewModel = viewModel(),
) {
    val sync by viewModel.sync.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val modo by sesionViewModel.modo.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val cartaEditable = modo.cartaEditable

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    AjustesSeccionScaffold(
        title = stringResource(R.string.ajustes_sync_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = if (cartaEditable) {
                        stringResource(R.string.ajustes_sync_desc)
                    } else {
                        stringResource(R.string.sesion_tpv_bloqueado)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                SincronizarCard(
                    state = sync,
                    enabled = cartaEditable,
                    onPrograma = viewModel::setPrograma,
                    onHost = viewModel::setHost,
                    onPuerto = viewModel::setPuerto,
                    onRuta = viewModel::setRuta,
                    onElegirServidor = viewModel::elegirServidor,
                    onBuscar = viewModel::buscarServidores,
                    onSincronizar = viewModel::sincronizar,
                )
            }
        }
    }
}

@Composable
private fun CopiasAjustesScreen(
    onBack: () -> Unit,
    viewModel: AjustesViewModel = viewModel(),
    sesionViewModel: SesionViewModel = viewModel(),
) {
    val mensaje by viewModel.mensaje.collectAsState()
    val modo by sesionViewModel.modo.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val cartaEditable = modo.cartaEditable
    val importPreview by viewModel.importPreview.collectAsState()
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelarImportacion() },
            title = { Text(stringResource(R.string.ajustes_import_preview_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.ajustes_import_preview_desc))
                    Text(
                        "🆕 ${preview.nuevos} productos nuevos",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "🔄 ${preview.actualizados} productos actualizados",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        "⏭️ ${preview.ignorados} productos ignorados (nombres vacíos)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (preview.categoriasMapeadas.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🗂️ Categorías detectadas:",
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        )
                        preview.categoriasMapeadas.take(10).forEach { (origen, destino) ->
                            Text(
                                "  $origen → $destino",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (preview.categoriasMapeadas.size > 10) {
                            Text(
                                "  … y ${preview.categoriasMapeadas.size - 10} más",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { viewModel.confirmarImportacion(it) }
                }) {
                    Text(
                        stringResource(R.string.ajustes_import_confirm),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarImportacion() }) {
                    Text(stringResource(R.string.menu_cancel))
                }
            },
        )
    }

    val crearArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportar(uri) }
    val abrirArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            viewModel.importar(uri)
        }
    }

    AjustesSeccionScaffold(
        title = stringResource(R.string.ajustes_backup_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.ajustes_backup_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                BackupCard(
                    enabled = cartaEditable,
                    onExportar = {
                        crearArchivo.launch("personalcomander_productos.json")
                    },
                    onImportar = {
                        abrirArchivo.launch(
                            arrayOf("application/json", "text/plain", "application/octet-stream"),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AvanzadoAjustesScreen(
    onBack: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenPerfil: () -> Unit,
    sesionViewModel: SesionViewModel = viewModel(),
) {
    val sesionMensaje by sesionViewModel.mensaje.collectAsState()
    val modo by sesionViewModel.modo.collectAsState()
    val identityUrl by sesionViewModel.identityUrl.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sesionMensaje) {
        sesionMensaje?.let {
            snackbarHostState.showSnackbar(it)
            sesionViewModel.limpiarMensaje()
        }
    }

    AjustesSeccionScaffold(
        title = stringResource(R.string.sesion_cuenta_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.sesion_cuenta_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                CuentaCard(
                    identityUrl = identityUrl,
                    onIdentityUrl = sesionViewModel::setIdentityUrl,
                    onAbrirCuenta = if (modo is ModoSesion.Local) onOpenAuth else onOpenPerfil,
                    abrirLabel = stringResource(
                        if (modo is ModoSesion.Local) R.string.sesion_entrar else R.string.sesion_perfil_title,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SincronizarCard(
    state: AjustesSyncState,
    enabled: Boolean,
    onPrograma: (TpvPrograma) -> Unit,
    onHost: (String) -> Unit,
    onPuerto: (String) -> Unit,
    onRuta: (String) -> Unit,
    onElegirServidor: (ServidorDescubierto) -> Unit,
    onBuscar: () -> Unit,
    onSincronizar: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.ajustes_program_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TpvPrograma.entries) { programa ->
                    FilterChip(
                        selected = state.programa == programa,
                        onClick = { onPrograma(programa) },
                        enabled = enabled,
                        label = { Text(programa.nombre) }
                    )
                }
            }
            OutlinedTextField(
                value = state.host,
                onValueChange = onHost,
                enabled = enabled,
                label = { Text(stringResource(R.string.lbl_ip_server)) },
                placeholder = { Text("192.168.1.50") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.puerto,
                    onValueChange = onPuerto,
                    enabled = enabled,
                    label = { Text(stringResource(R.string.lbl_port)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.ruta,
                    onValueChange = onRuta,
                    enabled = enabled,
                    label = { Text(stringResource(R.string.lbl_route)) },
                    singleLine = true,
                    modifier = Modifier.weight(1.6f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PcSecondaryButton(
                    text = stringResource(R.string.ajustes_search_network),
                    onClick = onBuscar,
                    enabled = enabled && !state.escaneando,
                    icon = if (state.escaneando) null else Icons.Default.Search,
                    modifier = Modifier.weight(1f),
                )
                PcPrimaryButton(
                    text = stringResource(R.string.btn_sync),
                    onClick = onSincronizar,
                    enabled = enabled && !state.sincronizando,
                    icon = if (state.sincronizando) null else Icons.Default.CloudDownload,
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.escaneando) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(height = 16, radius = 4)
                    ShimmerBox(height = 40, radius = 8)
                    ShimmerBox(height = 40, radius = 8)
                }
            } else if (state.sincronizando) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(height = 12, radius = 4)
                    ShimmerBox(height = 20, radius = 4)
                }
            } else if (state.servidores.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.ajustes_servers_found),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.servidores.forEach { servidor ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onElegirServidor(servidor) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${servidor.ip}:${servidor.puerto}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = servidor.etiqueta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onElegirServidor(servidor) }, enabled = enabled) {
                            Text(stringResource(R.string.btn_use))
                        }
                    }
                }
            }
            state.mensaje?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BackupCard(
    enabled: Boolean,
    onExportar: () -> Unit,
    onImportar: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PcSecondaryButton(
                text = stringResource(R.string.btn_export),
                onClick = onExportar,
                enabled = enabled,
                icon = Icons.Default.FileDownload,
                modifier = Modifier.weight(1f),
            )
            PcSecondaryButton(
                text = stringResource(R.string.btn_import),
                onClick = onImportar,
                enabled = enabled,
                icon = Icons.Default.CloudUpload,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CuentaCard(
    identityUrl: String,
    onIdentityUrl: (String) -> Unit,
    onAbrirCuenta: () -> Unit,
    abrirLabel: String,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = identityUrl,
                onValueChange = onIdentityUrl,
                label = { Text(stringResource(R.string.sesion_identity_url)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            PcPrimaryButton(
                text = abrirLabel,
                onClick = onAbrirCuenta,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SalaCard(
    modo: ModoSesion,
    membresias: List<MembresiaEstablecimiento>,
    bares: List<ServidorDescubierto>,
    escaneando: Boolean,
    busy: Boolean,
    onBuscar: () -> Unit,
    onConectar: (String, Int) -> Unit,
    onDesconectar: () -> Unit,
    onIniciarJornada: () -> Unit,
    onCortarJornada: () -> Unit,
    onIrLogin: () -> Unit,
) {
    var barHost by remember {
        mutableStateOf(
            if (modo is ModoSesion.Establecimiento) modo.barHost else "10.0.2.2",
        )
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (modo) {
                ModoSesion.Local -> {
                    Text(
                        text = stringResource(R.string.sesion_sala_login_primero),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PcSecondaryButton(
                        text = stringResource(R.string.sesion_entrar),
                        onClick = onIrLogin,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is ModoSesion.Establecimiento -> {
                    Text(
                        text = stringResource(
                            when {
                                modo.sesionTrabajo -> R.string.sesion_modo_sala_admitido
                                modo.admitido -> R.string.sesion_modo_sala_sin_jornada
                                else -> R.string.sesion_modo_sala_pendiente
                            },
                            modo.etiquetaLocal(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    MembresiasIdentityBlock(membresias = membresias)
                    if (modo.admitido && !modo.sesionTrabajo) {
                        PcPrimaryButton(
                            text = stringResource(R.string.sesion_empezar_jornada),
                            onClick = onIniciarJornada,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (modo.sesionTrabajo) {
                        PcSecondaryButton(
                            text = stringResource(R.string.sesion_terminar_jornada),
                            onClick = onCortarJornada,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    PcSecondaryButton(
                        text = stringResource(R.string.sesion_desconectar_bar),
                        onClick = onDesconectar,
                        icon = Icons.Default.LinkOff,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is ModoSesion.Identidad -> {
                    MembresiasIdentityBlock(membresias = membresias)
                    if (modo.qr == null) {
                        Text(
                            text = stringResource(R.string.sesion_qr_revocada),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    OutlinedTextField(
                        value = barHost,
                        onValueChange = { barHost = it },
                        label = { Text(stringResource(R.string.sesion_bar_host)) },
                        placeholder = { Text("10.0.2.2") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PcSecondaryButton(
                            text = stringResource(R.string.sesion_buscar_bares),
                            onClick = onBuscar,
                            enabled = !escaneando && !busy,
                            icon = if (escaneando) null else Icons.Default.Search,
                            modifier = Modifier.weight(1f),
                        )
                        PcPrimaryButton(
                            text = stringResource(R.string.sesion_conectar_bar),
                            onClick = { onConectar(barHost.trim(), BarLanCliente.PUERTO) },
                            enabled = barHost.isNotBlank() && !busy && modo.qr != null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (escaneando) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (bares.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.ajustes_servers_found),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        bares.forEach { servidor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        barHost = servidor.ip
                                        onConectar(servidor.ip, servidor.puerto)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "${servidor.ip}:${servidor.puerto}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = {
                                    barHost = servidor.ip
                                    onConectar(servidor.ip, servidor.puerto)
                                }) {
                                    Text(stringResource(R.string.btn_use))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
