@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.jaminsmoke.personalcomander.data.CategoriaIcono
import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.MesaEstado
import com.jaminsmoke.personalcomander.data.PedidoEstado
import com.jaminsmoke.personalcomander.data.Producto

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

    LaunchedEffect(state.feedbackVoz) { state.feedbackVoz?.let { feedbackHaptico(it); snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it); viewModel.limpiarError() } }

    val mesaCerrada by viewModel.mesaCerrada.collectAsState()
    LaunchedEffect(mesaCerrada) { if (mesaCerrada) onBack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.comanda_table_prefix, state.mesa?.numero ?: mesaId))
                        Text(estadoLabel(state.mesa?.estado), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.btn_back)) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search + mic row
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(state.busqueda, viewModel::setBusqueda, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.comanda_search_placeholder)) }, singleLine = true)
                IconButton(onClick = { if (micPermissionGranted) iniciarVoz() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, enabled = !state.escuchandoVoz) {
                    Icon(Icons.Default.Mic, stringResource(R.string.comanda_voice_talk), tint = if (state.escuchandoVoz) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }

            // Processing card
            if (state.procesandoVoz) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(stringResource(R.string.comanda_processing), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
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
                    esCercana || btConectado -> MaterialTheme.colorScheme.primary
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
                                    esCercana || btConectado -> MaterialTheme.colorScheme.primary
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
                        val gridCols = if (state.categoria == null && state.busqueda.isBlank()) 3 else 4
                        if (state.categoria == null && state.busqueda.isBlank()) {
                            val agrupados = state.productos.groupBy { it.categoria }
                            LazyColumn(
                                Modifier.weight(1f).fillMaxSize(),
                                contentPadding = PaddingValues(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                agrupados.forEach { (categoria, prods) ->
                                    stickyHeader {
                                        Text("${CategoriaIcono.de(categoria)} $categoria", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
                                    }
                                    items(prods, key = { it.id }) { p -> ProductoRow(p, onClick = { viewModel.addProducto(p) }) }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridCols),
                                Modifier.weight(1f).fillMaxSize(),
                                contentPadding = PaddingValues(4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(state.productos, key = { it.id }) { p -> ProductoGridCard(p, onClick = { viewModel.addProducto(p) }) }
                            }
                        }
                    }
                } else {
                    // Phone: tabs at top
                    Column(Modifier.fillMaxSize()) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = state.categorias.indexOf(state.categoria).coerceAtLeast(0),
                            modifier = Modifier.fillMaxWidth(),
                            edgePadding = 12.dp, divider = {}, indicator = {}
                        ) {
                            Tab(selected = state.categoria == null, onClick = { viewModel.setCategoria(null) }) {
                                Text(stringResource(R.string.comanda_all_categories), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    fontWeight = if (state.categoria == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (state.categoria == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            state.categorias.forEach { cat ->
                                val emoji = CategoriaIcono.de(cat)
                                Tab(selected = state.categoria == cat, onClick = { viewModel.setCategoria(if (state.categoria == cat) null else cat) }) {
                                    Text("$emoji $cat", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        fontWeight = if (state.categoria == cat) FontWeight.Bold else FontWeight.Normal,
                                        color = if (state.categoria == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        if (state.categoria == null && state.busqueda.isBlank()) {
                            val agrupados = state.productos.groupBy { it.categoria }
                            LazyColumn(
                                Modifier.fillMaxWidth().weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                agrupados.forEach { (categoria, prods) ->
                                    stickyHeader {
                                        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                                            Text("${CategoriaIcono.de(categoria)} $categoria", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                                        }
                                    }
                                    items(prods, key = { it.id }) { p -> ProductoRow(p, onClick = { viewModel.addProducto(p) }) }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                Modifier.fillMaxWidth().weight(1f),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.productos, key = { it.id }) { p -> ProductoGridCard(p, onClick = { viewModel.addProducto(p) }) }
                            }
                        }
                    }
                }
            }

            // Bottom comanda panel
            ComandaPanel(state.lineas, state.total, state.pedido?.estado, viewModel::aumentarLinea, viewModel::disminuirLinea, viewModel::enviarACocina, viewModel::cerrarMesa)
        }
    }
}

@Composable
private fun ProductoRow(producto: Producto, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(producto.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${CategoriaIcono.de(producto.categoria)} ${producto.categoria}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(producto.precio.formatoEuro(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.Add, stringResource(R.string.btn_add), Modifier.padding(start = 6.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ProductoGridCard(producto: Producto, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(CategoriaIcono.de(producto.categoria), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(producto.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(producto.precio.formatoEuro(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ComandaPanel(
    lineas: List<LineaPedido>, total: Double, pedidoEstado: PedidoEstado?,
    onAumentar: (LineaPedido) -> Unit, onDisminuir: (LineaPedido) -> Unit,
    onEnviarACocina: () -> Unit, onCerrarMesa: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(stringResource(R.string.comanda_panel_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (lineas.isEmpty()) Text(stringResource(R.string.comanda_empty), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
            else LazyColumn(Modifier.heightIn(max = 160.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(lineas, key = { it.id }) { linea -> LineaRow(linea, { onAumentar(linea) }, { onDisminuir(linea) }) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.comanda_total_label), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(total.formatoEuro(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val hay = pedidoEstado != null && lineas.isNotEmpty()
                Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onEnviarACocina() }, Modifier.weight(1f), enabled = hay && pedidoEstado == PedidoEstado.ABIERTA) {
                    Text(if (pedidoEstado == PedidoEstado.ENVIADA) stringResource(R.string.comanda_in_kitchen) else stringResource(R.string.comanda_send_to_kitchen))
                }
                OutlinedButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCerrarMesa() }, Modifier.weight(1f), enabled = pedidoEstado != null) { Text(stringResource(R.string.comanda_close_table)) }
            }
        }
    }
}

@Composable
private fun LineaRow(linea: LineaPedido, onAumentar: () -> Unit, onDisminuir: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${linea.cantidad}×", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(linea.nombreProducto, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        Text((linea.precioUnitario * linea.cantidad).formatoEuro(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onDisminuir, Modifier.size(32.dp)) { Icon(Icons.Default.Clear, stringResource(R.string.btn_remove), Modifier.size(18.dp)) }
        IconButton(onClick = onAumentar, Modifier.size(32.dp)) { Icon(Icons.Default.Add, stringResource(R.string.btn_add), Modifier.size(18.dp)) }
    }
}

@Composable
private fun CategorySidebarItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun estadoLabel(estado: MesaEstado?): String = when (estado) {
    MesaEstado.LIBRE -> stringResource(R.string.mesas_free)
    MesaEstado.OCUPADA -> stringResource(R.string.mesas_occupied)
    MesaEstado.EN_COCINA -> stringResource(R.string.mesas_in_kitchen)
    null -> ""
}
