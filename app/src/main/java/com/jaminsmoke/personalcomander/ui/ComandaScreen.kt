@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.jaminsmoke.personalcomander.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.jaminsmoke.personalcomander.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.data.CartaModificadores
import com.jaminsmoke.personalcomander.data.CategoriaIcono
import com.jaminsmoke.personalcomander.data.GrupoConOpciones
import com.jaminsmoke.personalcomander.data.LineaEstado
import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.ModificadorElegido
import com.jaminsmoke.personalcomander.data.detalleModificadores
import com.jaminsmoke.personalcomander.data.esEditable
import com.jaminsmoke.personalcomander.data.sesion.RecogerLogica
import com.jaminsmoke.personalcomander.data.MesaVisualStatus
import com.jaminsmoke.personalcomander.data.PedidoEstado
import com.jaminsmoke.personalcomander.data.Producto
import com.jaminsmoke.personalcomander.data.mesaVisualStatus
import com.jaminsmoke.personalcomander.data.nombreVisible
import com.jaminsmoke.personalcomander.ui.components.PcPrimaryButton
import com.jaminsmoke.personalcomander.ui.components.PcSecondaryButton
import com.jaminsmoke.personalcomander.ui.theme.mesaStatusAccent

@SuppressLint("MissingPermission")
@Composable
fun ComandaScreen(
    mesaId: Long,
    onBack: () -> Unit,
    viewModel: ComandaViewModel = viewModel(
        key = "comanda_$mesaId",
        factory = ComandaViewModel.Factory(LocalContext.current.applicationContext as android.app.Application, mesaId)
    )
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    var micPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var textoParcial by remember { mutableStateOf<String?>(null) }
    var rmsActual by remember { mutableFloatStateOf(0f) }
    val recognizer = remember { VozRecognizer(context) }
    DisposableEffect(Unit) { onDispose { recognizer.destruir() } }

    // Detectar Bluetooth conectado (mejor esfuerzo, sin permiso BLUETOOTH_CONNECT)
    @Suppress("DEPRECATION")
    val btAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    val btConectado = remember {
        if (btAdapter?.isEnabled == true) {
            runCatching {
                btAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED
            }.getOrDefault(false)
        } else false
    }

    // Feedback háptico + sonoro para resultados de voz
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50) }
    DisposableEffect(Unit) { onDispose { toneGen.release() } }

    val feedbackHaptico: (String) -> Unit = remember(context) { { msg ->
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        fun vibrar(ms: Long) {
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(ms)
                }
            } catch (_: SecurityException) { }
        }
        fun vibrarDoble() {
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            } catch (_: SecurityException) { }
        }
        when {
            msg.contains("Añadido") || msg.contains("Added") -> {
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 150)
                vibrar(80)
            }
            msg.contains("Quitado") || msg.contains("Removed") || msg.contains("vaciada") || msg.contains("cleared") -> {
                toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 100)
                vibrar(40)
            }
            else -> {
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                vibrarDoble()
            }
        }
    } }

    val iniciarVoz: () -> Unit = {
        textoParcial = null; rmsActual = 0f; viewModel.setEscuchandoVoz(true)
        recognizer.onRms = { rmsActual = it }
        recognizer.onParcial = { textoParcial = it }
        recognizer.onResultado = {
            textoParcial = null; viewModel.setEscuchandoVoz(false)
            val cercana = recognizer.vozCercana || btConectado
            viewModel.procesarVoz(it, cercana)
        }
        recognizer.onError = { textoParcial = null; viewModel.setEscuchandoVoz(false); viewModel.informar(mensajeErrorVoz(context, it)) }
        recognizer.empezar()
    }
    val detenerVoz: () -> Unit = { textoParcial = null; recognizer.detener(); viewModel.setEscuchandoVoz(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micPermissionGranted = granted; if (granted) iniciarVoz()
    }

    LaunchedEffect(state.feedbackVoz) { state.feedbackVoz?.let { feedbackHaptico(it) } }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it); viewModel.limpiarError() } }

    val mesaCerrada by viewModel.mesaCerrada.collectAsState()
    val mostrarConfirmacion by viewModel.mostrarConfirmacionCierre.collectAsState()
    val mostrarUndo by viewModel.mostrarUndo.collectAsState()
    val tiempoRestanteUndo by viewModel.tiempoRestanteUndo.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(mesaCerrada) { if (mesaCerrada) onBack() }

    // Snackbar con undo cuando se cierra la mesa
    val undoMessage = stringResource(R.string.comanda_close_undo_snackbar)
    val undoLabel = stringResource(R.string.comanda_close_undo)
    LaunchedEffect(mostrarUndo) {
        if (mostrarUndo) {
            val result = snackbarHostState.showSnackbar(
                message = undoMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.reabrirMesa()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val visual = state.mesa?.let { mesaVisualStatus(it) }
            val accent = visual?.let { mesaStatusAccent(it) }
                ?: MaterialTheme.colorScheme.onSurfaceVariant
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.comanda_table_prefix, state.mesa?.nombreVisible(state.salaNombre) ?: "#$mesaId"),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            estadoVisualLabel(visual),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.btn_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search + mic row
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(state.busqueda, viewModel::setBusqueda, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.comanda_search_placeholder)) }, singleLine = true)
                IconButton(onClick = { if (micPermissionGranted) iniciarVoz() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, enabled = !state.escuchandoVoz) {
                    Icon(
                        Icons.Default.Mic,
                        stringResource(R.string.comanda_voice_talk),
                        tint = if (state.escuchandoVoz) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // Processing card
            if (state.procesandoVoz) {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            stringResource(R.string.comanda_processing),
                            Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Listening card — layout estable con animaciones suaves de color
            if (state.escuchandoVoz && !state.procesandoVoz) {
                val rmsAnim by animateFloatAsState(rmsActual, animationSpec = tween(150), label = "rms")
                val esCercana = rmsActual >= RMS_UMBRAL_CERCANIA
                val esLejana = !btConectado && rmsActual > 0f && rmsActual < RMS_UMBRAL_CERCANIA

                val targetColor = when {
                    btConectado -> MaterialTheme.colorScheme.tertiaryContainer
                    esCercana -> MaterialTheme.colorScheme.primaryContainer
                    esLejana -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val cardColor by animateColorAsState(targetColor, tween(300), label = "cardColor")

                val statusDot = when {
                    esCercana -> MaterialTheme.colorScheme.primary
                    esLejana -> MaterialTheme.colorScheme.error
                    btConectado -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                }
                val micTint = when {
                    esCercana || btConectado -> MaterialTheme.colorScheme.secondary
                    esLejana -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                        // Fila superior: icono, título, estado, cancelar
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, null, tint = micTint, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.comanda_listening),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            // Indicador sutil de estado
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(statusDot))
                            Spacer(Modifier.width(8.dp))
                            if (btConectado) {
                                Icon(Icons.Default.Bluetooth, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(Modifier.width(8.dp))
                            }
                            OutlinedButton(onClick = detenerVoz, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(stringResource(R.string.comanda_cancel), style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Texto parcial o hint
                        val parcial = textoParcial
                        Text(
                            parcial ?: stringResource(R.string.comanda_voice_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (parcial != null) 0.85f else 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        // Barras de RMS con altura fija para evitar saltos de layout
                        Spacer(Modifier.height(4.dp))
                        Row(
                            Modifier.fillMaxWidth().height(28.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            for (i in 0 until 5) {
                                val altura = ((rmsAnim / 15f).coerceIn(0.08f, 1f) * 28.dp.value).dp
                                val barColor = when {
                                    esCercana || btConectado -> MaterialTheme.colorScheme.secondary
                                    esLejana -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.outline
                                }
                                Box(Modifier.width(3.dp).height(altura).clip(RoundedCornerShape(2.dp)).background(barColor))
                            }
                        }

                        // Alerta de lejanía (altura fija, visibilidad condicional)
                        Box(Modifier.height(18.dp).padding(top = 2.dp)) {
                            if (esLejana) {
                                Text(
                                    stringResource(R.string.comanda_rms_low),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Voice feedback card
            state.feedbackVoz?.let { feedback ->
                VoiceFeedbackCard(
                    message = feedback,
                    type = parseFeedbackType(feedback),
                    visible = true,
                    onDismiss = { viewModel.limpiarFeedback() }
                )
            }

            // Category + Products area — adapts to screen width
            BoxWithConstraints(Modifier.weight(1f)) {
                val esAncho = maxWidth > 600.dp

                if (esAncho) {
                    // Tablet / landscape: sidebar + grid
                    Row(Modifier.fillMaxSize()) {
                        // Category sidebar
                        LazyColumn(
                            Modifier.width(150.dp).fillMaxSize().padding(vertical = 8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            item {
                                CategorySidebarItem(stringResource(R.string.comanda_all_categories), selected = state.categoria == null) { viewModel.setCategoria(null) }
                            }
                            items(state.categorias) { cat ->
                                CategorySidebarItem("${CategoriaIcono.de(cat)} $cat", selected = state.categoria == cat) {
                                    viewModel.setCategoria(if (state.categoria == cat) null else cat)
                                }
                            }
                        }

                        // Product grid (more columns on wide screens)
                        if (state.productos.isEmpty()) {
                            val esBusqueda = state.busqueda.isNotBlank() || state.categoria != null
                            EmptyState(
                                icon = if (esBusqueda) Icons.Default.SearchOff else Icons.Default.ShoppingCart,
                                title = stringResource(if (esBusqueda) R.string.empty_comanda_no_results else R.string.empty_comanda_no_products),
                                subtitle = stringResource(if (esBusqueda) R.string.empty_comanda_no_results_subtitle else R.string.empty_comanda_no_products_subtitle),
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                        val gridCols = if (state.categoria == null && state.busqueda.isBlank()) 3 else 4
                        if (state.categoria == null && state.busqueda.isBlank()) {
                            LazyColumn(
                                Modifier.weight(1f).fillMaxSize(),
                                contentPadding = PaddingValues(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                catalogoLista(state.productos, conCategoria = true) { viewModel.pedirProducto(it) }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridCols),
                                Modifier.weight(1f).fillMaxSize(),
                                contentPadding = PaddingValues(4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                catalogoGrid(state.productos) { viewModel.pedirProducto(it) }
                            }
                        }
                        } // end else (productos no vacíos)
                    }
                } else {
                    // Phone: tabs at top
                    Column(Modifier.fillMaxSize()) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = if (state.categoria != null) state.categorias.indexOf(state.categoria) + 1 else 0,
                            modifier = Modifier.fillMaxWidth(),
                            edgePadding = 12.dp
                        ) {
                            Tab(selected = state.categoria == null, onClick = { viewModel.setCategoria(null) }) {
                                Text(stringResource(R.string.comanda_all_categories), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    fontWeight = if (state.categoria == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (state.categoria == null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            state.categorias.forEach { cat ->
                                val emoji = CategoriaIcono.de(cat)
                                Tab(selected = state.categoria == cat, onClick = { viewModel.setCategoria(if (state.categoria == cat) null else cat) }) {
                                    Text("$emoji $cat", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        fontWeight = if (state.categoria == cat) FontWeight.Bold else FontWeight.Normal,
                                        color = if (state.categoria == cat) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        if (state.productos.isEmpty()) {
                            val esBusqueda = state.busqueda.isNotBlank() || state.categoria != null
                            EmptyState(
                                icon = if (esBusqueda) Icons.Default.SearchOff else Icons.Default.ShoppingCart,
                                title = stringResource(if (esBusqueda) R.string.empty_comanda_no_results else R.string.empty_comanda_no_products),
                                subtitle = stringResource(if (esBusqueda) R.string.empty_comanda_no_results_subtitle else R.string.empty_comanda_no_products_subtitle),
                                modifier = Modifier.weight(1f)
                            )
                        } else if (state.categoria == null && state.busqueda.isBlank()) {
                            LazyColumn(
                                Modifier.fillMaxWidth().weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                catalogoLista(state.productos, conCategoria = true) { viewModel.pedirProducto(it) }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                Modifier.fillMaxWidth().weight(1f),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                catalogoGrid(state.productos) { viewModel.pedirProducto(it) }
                            }
                        }
                    }
                }
            }

            // Bottom comanda panel
            ComandaPanel(
                state.lineas,
                state.total,
                state.pedido?.estado,
                state.ligadoAlBar,
                viewModel::aumentarLinea,
                viewModel::disminuirLinea,
                viewModel::marcarServida,
                viewModel::marcarTodasListasServidas,
                viewModel::enviarACocina,
                viewModel::solicitarCierre,
                viewModel::editarLinea,
            )
        }
    }

    // Diálogo de confirmación de cierre
    if (mostrarConfirmacion) {
        val mesaNombre = state.mesa?.nombreVisible(state.salaNombre) ?: "#$mesaId"
        val numLineas = state.lineas.size
        val total = state.total.formatoEuro()
        AlertDialog(
            onDismissRequest = viewModel::cancelarCierre,
            title = { Text(stringResource(R.string.comanda_close_confirm_title)) },
            text = { Text(stringResource(R.string.comanda_close_confirm_body, mesaNombre, numLineas, total)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarCierre) {
                    Text(stringResource(R.string.comanda_close_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarCierre) {
                    Text(stringResource(R.string.menu_cancel))
                }
            }
        )
    }

    state.hoja?.let { hoja ->
        HojaModificadoresSheet(
            hoja = hoja,
            grupos = state.gruposDe(hoja.producto.id),
            onDismiss = viewModel::cerrarHoja,
            onConfirm = viewModel::confirmarHoja,
        )
    }
}

@Composable
private fun ProductoRow(producto: Producto, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(producto.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = scheme.onSurface)
                Text("${CategoriaIcono.de(producto.categoria)} ${producto.categoria}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
            Text(producto.precio.formatoEuro(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = scheme.secondary)
            Icon(Icons.Default.Add, stringResource(R.string.btn_add), Modifier.padding(start = 6.dp).size(20.dp), tint = scheme.secondary)
        }
    }
}

@Composable
private fun ProductoGridCard(producto: Producto, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(CategoriaIcono.de(producto.categoria), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(producto.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = scheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(producto.precio.formatoEuro(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = scheme.secondary)
        }
    }
}

@Composable
private fun ComandaPanel(
    lineas: List<LineaPedido>,
    total: Double,
    pedidoEstado: PedidoEstado?,
    ligadoAlBar: Boolean,
    onAumentar: (LineaPedido) -> Unit,
    onDisminuir: (LineaPedido) -> Unit,
    onServir: (LineaPedido) -> Unit,
    onServirTodas: () -> Unit,
    onEnviarACocina: () -> Unit,
    onCerrarMesa: () -> Unit,
    onEditar: (LineaPedido) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val bloques = RecogerLogica.bloques(lineas)
    val hayPendientes = RecogerLogica.lineasAEnviar(lineas).isNotEmpty()
    Surface(color = scheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(stringResource(R.string.comanda_panel_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
            if (lineas.isEmpty()) {
                Text(stringResource(R.string.comanda_empty), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                LazyColumn(Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (bloques.estaRonda.isNotEmpty()) {
                        item(key = "hdr-ronda") {
                            Text(
                                stringResource(R.string.comanda_section_esta_ronda),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            )
                        }
                        items(bloques.estaRonda, key = { "r-${it.id}" }) { linea ->
                            LineaRow(
                                linea = linea,
                                editable = linea.estado.esEditable(),
                                enCola = linea.estado == LineaEstado.ENVIADA,
                                sePuedeServir = RecogerLogica.puedeMarcarServida(linea, ligadoAlBar),
                                onAumentar = { onAumentar(linea) },
                                onDisminuir = { onDisminuir(linea) },
                                onServir = { onServir(linea) },
                                onEditar = { onEditar(linea) },
                            )
                        }
                    }
                    if (bloques.paraRecoger.isNotEmpty()) {
                        item(key = "hdr-recoger") {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(R.string.comanda_section_para_recoger),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.secondary,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = onServirTodas, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                    Text(stringResource(R.string.comanda_mark_all_served))
                                }
                            }
                        }
                        items(bloques.paraRecoger, key = { "p-${it.id}" }) { linea ->
                            LineaRow(
                                linea = linea,
                                editable = false,
                                enCola = false,
                                sePuedeServir = true,
                                onAumentar = {},
                                onDisminuir = {},
                                onServir = { onServir(linea) },
                                onEditar = {},
                            )
                        }
                    }
                    if (bloques.servido.isNotEmpty()) {
                        item(key = "hdr-servido") {
                            Text(
                                stringResource(R.string.comanda_section_servido),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                            )
                        }
                        items(bloques.servido, key = { "s-${it.id}" }) { linea ->
                            LineaRow(
                                linea = linea,
                                editable = false,
                                enCola = false,
                                sePuedeServir = false,
                                onAumentar = {},
                                onDisminuir = {},
                                onServir = {},
                                onEditar = {},
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.comanda_total_label), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                Spacer(Modifier.weight(1f))
                Text(total.formatoEuro(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.secondary)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val hay = pedidoEstado != null && lineas.isNotEmpty()
                PcPrimaryButton(
                    text = if (hayPendientes) stringResource(R.string.comanda_send_to_kitchen)
                    else stringResource(R.string.comanda_in_kitchen),
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onEnviarACocina() },
                    enabled = hay && hayPendientes,
                    modifier = Modifier.weight(1f),
                )
                PcSecondaryButton(
                    text = stringResource(R.string.comanda_close_table),
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCerrarMesa() },
                    enabled = pedidoEstado != null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LineaRow(
    linea: LineaPedido,
    editable: Boolean,
    enCola: Boolean,
    sePuedeServir: Boolean,
    onAumentar: () -> Unit,
    onDisminuir: () -> Unit,
    onServir: () -> Unit,
    onEditar: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val detalle = linea.detalleModificadores()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${linea.cantidad}×", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = scheme.secondary)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .then(if (editable) Modifier.clickable(onClick = onEditar) else Modifier),
        ) {
            Text(
                linea.nombreProducto,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = scheme.onSurface,
            )
            if (detalle.isNotEmpty()) {
                Text(
                    detalle,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (enCola) {
            Text(
                stringResource(R.string.comanda_en_cola),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Text(
            (linea.precioUnitario * linea.cantidad).formatoEuro(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
        )
        when {
            editable -> {
                IconButton(onClick = onDisminuir, Modifier.size(32.dp)) {
                    Icon(Icons.Default.Clear, stringResource(R.string.btn_remove), Modifier.size(18.dp))
                }
                IconButton(onClick = onAumentar, Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, stringResource(R.string.btn_add), Modifier.size(18.dp), tint = scheme.secondary)
                }
            }
            sePuedeServir -> {
                IconButton(onClick = onServir, Modifier.size(32.dp)) {
                    Icon(Icons.Default.Check, stringResource(R.string.comanda_mark_served), Modifier.size(18.dp), tint = scheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun CategorySidebarItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) scheme.primaryContainer else scheme.surfaceContainer,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.4f)) else null,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) scheme.secondary else scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun estadoVisualLabel(status: MesaVisualStatus?): String = when (status) {
    MesaVisualStatus.LIBRE -> stringResource(R.string.mesas_free)
    MesaVisualStatus.OCUPADA -> stringResource(R.string.mesas_occupied)
    MesaVisualStatus.EN_COCINA -> stringResource(R.string.mesas_in_kitchen)
    MesaVisualStatus.RESERVADA -> stringResource(R.string.mesas_reserved)
    MesaVisualStatus.BLOQUEADA -> stringResource(R.string.mesas_blocked)
    null -> ""
}

private fun LazyListScope.catalogoLista(
    productos: List<Producto>,
    conCategoria: Boolean,
    onClick: (Producto) -> Unit,
) {
    if (conCategoria) {
        productos.groupBy { it.categoria }.forEach { (categoria, prods) ->
            stickyHeader(key = "cat-$categoria") {
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    Text(
                        "${CategoriaIcono.de(categoria)} $categoria",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }
            catalogoSubfamilias(prods, onClick)
        }
    } else {
        catalogoSubfamilias(productos, onClick)
    }
}

private fun LazyListScope.catalogoSubfamilias(
    productos: List<Producto>,
    onClick: (Producto) -> Unit,
) {
    CartaModificadores.agruparPorSubfamilia(productos).forEach { (sub, items) ->
        if (sub != null) {
            stickyHeader(key = "sub-$sub-${items.first().id}") {
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                    Text(
                        sub,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }
        items(items, key = { it.id }) { p -> ProductoRow(p, onClick = { onClick(p) }) }
    }
}

private fun LazyGridScope.catalogoGrid(
    productos: List<Producto>,
    onClick: (Producto) -> Unit,
) {
    CartaModificadores.agruparPorSubfamilia(productos).forEach { (sub, items) ->
        if (sub != null) {
            item(key = "gsub-$sub-${items.first().id}", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    sub,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }
        items(items, key = { it.id }) { p -> ProductoGridCard(p, onClick = { onClick(p) }) }
    }
}

@Composable
private fun HojaModificadoresSheet(
    hoja: HojaModificadores,
    grupos: List<GrupoConOpciones>,
    onDismiss: () -> Unit,
    onConfirm: (List<ModificadorElegido>, String, Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var elegidos by remember(hoja.lineaId, hoja.producto.id) { mutableStateOf(hoja.elegidos) }
    var nota by remember(hoja.lineaId, hoja.producto.id) { mutableStateOf(hoja.nota) }
    var cantidad by remember(hoja.lineaId, hoja.producto.id) { mutableIntStateOf(hoja.cantidad.coerceAtLeast(1)) }
    val puedeConfirmar = !CartaModificadores.faltanObligatorios(grupos, elegidos)
    val precio = CartaModificadores.precioUnitario(hoja.producto.precio, elegidos)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(hoja.producto.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.comanda_mod_precio_unidad, precio.formatoEuro()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            grupos.forEach { gc ->
                val titulo = buildString {
                    append(gc.grupo.nombre)
                    if (gc.grupo.obligatorio) append(" *")
                }
                Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gc.opciones.forEach { op ->
                        val sel = elegidos.any { it.opcionId == op.id }
                        val etiqueta = if (op.deltaPrecio == 0.0) op.nombre
                        else "${op.nombre} (${if (op.deltaPrecio > 0) "+" else ""}${op.deltaPrecio.formatoEuro()})"
                        FilterChip(
                            selected = sel,
                            onClick = {
                                val elegido = ModificadorElegido(
                                    grupoId = gc.grupo.id,
                                    grupoNombre = gc.grupo.nombre,
                                    opcionId = op.id,
                                    opcionNombre = op.nombre,
                                    deltaPrecio = op.deltaPrecio,
                                )
                                elegidos = if (gc.grupo.multiple) {
                                    if (sel) elegidos.filter { it.opcionId != op.id } else elegidos + elegido
                                } else {
                                    elegidos.filter { it.grupoId != gc.grupo.id } + elegido
                                }
                            },
                            label = { Text(etiqueta) },
                        )
                    }
                }
            }
            if (hoja.producto.permiteNota) {
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text(stringResource(R.string.comanda_mod_nota)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.comanda_mod_cantidad), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { if (cantidad > 1) cantidad-- }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.btn_remove))
                }
                Text("$cantidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { cantidad++ }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_add))
                }
            }
            if (!puedeConfirmar) {
                Text(
                    stringResource(R.string.comanda_mod_obligatorio),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = { onConfirm(elegidos, nota, cantidad) },
                enabled = puedeConfirmar,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.comanda_mod_confirm))
            }
        }
    }
}
