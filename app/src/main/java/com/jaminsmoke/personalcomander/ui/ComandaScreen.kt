@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.jaminsmoke.personalcomander.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val recognizer = remember { VozRecognizer(context) }
    DisposableEffect(Unit) { onDispose { recognizer.destruir() } }

    val iniciarVoz: () -> Unit = {
        textoParcial = null; viewModel.setEscuchandoVoz(true)
        recognizer.onParcial = { textoParcial = it }
        recognizer.onResultado = { textoParcial = null; viewModel.setEscuchandoVoz(false); viewModel.procesarVoz(it) }
        recognizer.onError = { textoParcial = null; viewModel.setEscuchandoVoz(false); viewModel.informar(mensajeErrorVoz(context, it)) }
        recognizer.empezar()
    }
    val detenerVoz: () -> Unit = { textoParcial = null; recognizer.detener(); viewModel.setEscuchandoVoz(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micPermissionGranted = granted; if (granted) iniciarVoz()
    }

    LaunchedEffect(state.feedbackVoz) { state.feedbackVoz?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it); viewModel.limpiarError() } }

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

            // Listening card
            if (state.escuchandoVoz) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(stringResource(R.string.comanda_listening), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            val parcial = textoParcial
                            if (parcial != null) {
                                Text(parcial, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            } else {
                                Text(stringResource(R.string.comanda_voice_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f))
                            }
                        }
                        OutlinedButton(onClick = detenerVoz) { Text(stringResource(R.string.comanda_cancel)) }
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
                        ScrollableTabRow(
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
