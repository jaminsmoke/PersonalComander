package com.jaminsmoke.personalcomander.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaForma
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasScreen(
    onOpenMesa: (Long) -> Unit,
    onOpenMenu: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: MesasViewModel = viewModel()
) {
    val mesas by viewModel.mesas.collectAsState(initial = emptyList())
    val cargando by viewModel.cargando.collectAsState()
    val zonaSeleccionada by viewModel.zona.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var mesaEditando by remember { mutableStateOf<Mesa?>(null) }
    var mesaBorrando by remember { mutableStateOf<Mesa?>(null) }
    var mesaAislada by remember { mutableStateOf<Mesa?>(null) }
    var aisladaFinalX by remember { mutableStateOf(0f) }
    var aisladaFinalY by remember { mutableStateOf(0f) }
    var crearVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // Drag state
    var draggedMesa by remember { mutableStateOf<Mesa?>(null) }
    var dragBaseX by remember { mutableStateOf(0f) }
    var dragBaseY by remember { mutableStateOf(0f) }
    var dragPxX by remember { mutableStateOf(0f) }
    var dragPxY by remember { mutableStateOf(0f) }

    // Zoom state (pan is handled by scroll)
    var scale by remember { mutableStateOf(1f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    val zonas = remember(mesas) { mesas.map { it.zona }.distinct().filter { it.isNotBlank() } }
    val mesasFiltradas = remember(mesas, zonaSeleccionada) {
        if (zonaSeleccionada == null) mesas
        else mesas.filter { it.zona == zonaSeleccionada }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { crearVisible = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.btn_add))
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mesas_title)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.btn_back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.RestaurantMenu, stringResource(R.string.mesas_manage_menu))
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                // Zone tabs
                if (zonas.size > 1) {
                    ScrollableTabRow(
                        selectedTabIndex = zonas.indexOf(zonaSeleccionada).coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 12.dp, divider = {}, indicator = {}
                    ) {
                        val allLabel = stringResource(R.string.mesas_all_zones)
                        Tab(selected = zonaSeleccionada == null, onClick = { viewModel.setZona(null) }) {
                            Text(allLabel, Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                fontWeight = if (zonaSeleccionada == null) FontWeight.Bold else FontWeight.Normal,
                                color = if (zonaSeleccionada == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        zonas.forEach { zona ->
                            val emoji = zonaEmoji(zona)
                            Tab(selected = zonaSeleccionada == zona, onClick = { viewModel.setZona(if (zonaSeleccionada == zona) null else zona) }) {
                                Text("$emoji $zona", Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    fontWeight = if (zonaSeleccionada == zona) FontWeight.Bold else FontWeight.Normal,
                                    color = if (zonaSeleccionada == zona) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Board — canvas wraps content tightly with one extra column/row of room
                val scrollH = rememberScrollState()
                val scrollV = rememberScrollState()
                val PAD = CELL_F * 3f  // 120dp — just one card width of extra space
                val maxX = ((mesasFiltradas.maxOfOrNull { it.posX } ?: 0f) + CARD_W + PAD).coerceAtLeast(800f)
                val maxY = ((mesasFiltradas.maxOfOrNull { it.posY } ?: 0f) + CARD_W + PAD).coerceAtLeast(1200f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { viewportSize = it }
                        .horizontalScroll(scrollH)
                        .verticalScroll(scrollV)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = maxX.dp, height = maxY.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    // Wait for two-finger pinch
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var prevDist = 0f
                                    do {
                                        val event = awaitPointerEvent()
                                        val changes = event.changes.filter { it.pressed }
                                        if (changes.size >= 2) {
                                            val p1 = changes[0].position
                                            val p2 = changes[1].position
                                            val dist = (p1 - p2).getDistance()
                                            if (prevDist > 0f) {
                                                val zoom = dist / prevDist
                                                scale = (scale * zoom).coerceIn(0.5f, 2f)
                                            }
                                            prevDist = dist
                                        }
                                    } while (changes.any { it.pressed })
                                }
                            }
                            .drawBehind {
                                // Canvas border — thick frame so you know where the board ends
                                val borderW = 4.dp.toPx()
                                val half = borderW / 2f
                                drawRect(
                                    color = Color(0xFFB0B0B0),
                                    topLeft = Offset(half, half),
                                    size = Size(size.width - borderW, size.height - borderW),
                                    style = Stroke(width = borderW)
                                )
                                // Subtle dot grid for spatial reference
                                val spacing = CELL.toPx()
                                val dotColor = Color(0xFFD0D0D0)
                                val r = 1.5.dp.toPx()
                                var x = CELL.toPx()
                                while (x < size.width) {
                                    var y = CELL.toPx()
                                    while (y < size.height) {
                                        drawCircle(dotColor, r, Offset(x, y))
                                        y += spacing
                                    }
                                    x += spacing
                                }
                            }
                    ) {
                        // Shimmer loading — only while data hasn't arrived yet
                        if (cargando) {
                            for (i in 0 until 12) {
                                ShimmerBox(
                                    modifier = Modifier
                                        .offset(x = (CELL_F + (i % 4) * 160).dp, y = (CELL_F + (i / 4) * 160).dp)
                                        .width(CARD_WIDTH),
                                    height = 0,
                                    radius = 16
                                )
                            }
                        }

                        // Render each mesa with animated position
                        mesasFiltradas.forEach { mesa ->
                            key(mesa.id) {
                                val isDragging = draggedMesa?.id == mesa.id

                                // Animate position changes (smoothly interpolate when DB updates posX/posY)
                                val animX by animateFloatAsState(
                                    targetValue = mesa.posX,
                                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                                    label = "posX"
                                )
                                val animY by animateFloatAsState(
                                    targetValue = mesa.posY,
                                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                                    label = "posY"
                                )

                                val (mw, _) = mesaDims(mesa.forma, mesa.girada)
                                MesaCard(
                                    mesa = mesa,
                                    isDragging = isDragging,
                                    modifier = Modifier
                                        .offset(x = animX.dp, y = animY.dp)
                                        .width(mw.dp),
                                    onClick = {
                                        if (draggedMesa == null) onOpenMesa(mesa.id)
                                    },
                                    onEditClick = { mesaEditando = mesa },
                                    onDeleteClick = { mesaBorrando = mesa },
                                    onRotateClick = { viewModel.toggleGiro(mesa) },
                                    onDragStarted = {
                                        draggedMesa = mesa
                                        dragBaseX = animX
                                        dragBaseY = animY
                                        dragPxX = 0f
                                        dragPxY = 0f
                                    },
                                    onDrag = { deltaPx ->
                                        dragPxX += deltaPx.x
                                        dragPxY += deltaPx.y
                                    },
                                    onDragEnd = {
                                        draggedMesa?.let { dragged ->
                                            val deltaDpX = with(density) { dragPxX.toDp().value }
                                            val deltaDpY = with(density) { dragPxY.toDp().value }
                                            val rawX = dragBaseX + deltaDpX
                                            val rawY = dragBaseY + deltaDpY
                                            val snappedX = (rawX / CELL_F).roundToInt() * CELL_F
                                            val snappedY = (rawY / CELL_F).roundToInt() * CELL_F
                                            val (draggedW, draggedH) = mesaDims(dragged.forma, dragged.girada)
                                            val occupied = mesas.filter { it.id != dragged.id }.map {
                                                val (ow, oh) = mesaDims(it.forma, it.girada)
                                                listOf(it.posX, it.posY, ow, oh)
                                            }
                                            val (rawFinalX, rawFinalY) = findNearestFreeCell(
                                                snappedX, snappedY, draggedW, draggedH, occupied
                                            )
                                            // Clamp to board boundaries (keep mesa inside the bordered area)
                                            val clampX = rawFinalX.coerceIn(CELL_F, (maxX - CELL_F - draggedW).coerceAtLeast(CELL_F))
                                            val clampY = rawFinalY.coerceIn(CELL_F, (maxY - CELL_F - draggedH).coerceAtLeast(CELL_F))
                                            // Warning: mesa muy alejada del cluster
                                            if (isIsolated(clampX, clampY, dragged.id, mesas)) {
                                                mesaAislada = dragged
                                                aisladaFinalX = clampX
                                                aisladaFinalY = clampY
                                            } else {
                                                viewModel.updatePosicion(dragged, clampX, clampY)
                                            }
                                        }
                                        draggedMesa = null
                                        dragPxX = 0f
                                        dragPxY = 0f
                                    }
                                )
                            }
                        }

                        // Drag overlay
                        draggedMesa?.let { mesa ->
                            val overlayX = dragBaseX + with(density) { dragPxX.toDp().value }
                            val overlayY = dragBaseY + with(density) { dragPxY.toDp().value }
                            val (ow, _) = mesaDims(mesa.forma, mesa.girada)
                            Box(
                                modifier = Modifier
                                    .offset(x = overlayX.dp, y = overlayY.dp)
                                    .graphicsLayer {
                                        scaleX = 1.08f
                                        scaleY = 1.08f
                                        shadowElevation = 16f
                                        alpha = 0.92f
                                    }
                                    .width(ow.dp)
                                    .zIndex(10f)
                            ) {
                                DragOverlayCard(mesa)
                            }
                        }
                    }

                    // Zoom + auto-fit badges (bottom-left)
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).zIndex(5f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Auto-fit: center and scale to show all mesas
                        Card(
                            onClick = {
                                val ms = mesasFiltradas
                                if (ms.isNotEmpty() && viewportSize.width > 0) {
                                    val minX = ms.minOf { it.posX }
                                    val minY = ms.minOf { it.posY }
                                    val maxXf = ms.maxOf { it.posX } + CARD_W
                                    val maxYf = ms.maxOf { it.posY } + CARD_W
                                    val cw = maxXf - minX + 80f
                                    val ch = maxYf - minY + 80f
                                    val vw = with(density) { viewportSize.width.toDp().value }
                                    val vh = with(density) { viewportSize.height.toDp().value }
                                    scale = minOf(vw / cw, vh / ch, 2f).coerceIn(0.5f, 2f)
                                    coroutineScope.launch {
                                        scrollH.animateScrollTo(with(density) { ((minX - 40f) * scale).dp.roundToPx() }.coerceAtLeast(0))
                                        scrollV.animateScrollTo(with(density) { ((minY - 40f) * scale).dp.roundToPx() }.coerceAtLeast(0))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                "\u2316",  // ⌖ fit-to-screen symbol
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        // Zoom % badge — tap to reset
                        if (scale != 1f) {
                            val pct = (scale * 100).toInt()
                            Card(
                                onClick = { scale = 1f },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Text(
                                    "$pct%",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create dialog
    if (crearVisible) {
        var createZona by remember { mutableStateOf(zonaSeleccionada ?: zonas.firstOrNull() ?: "") }
        var createForma by remember { mutableStateOf(MesaForma.CUADRADA) }
        var createCap by remember { mutableStateOf("4") }
        var createAlias by remember { mutableStateOf("") }
        var zonaExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { crearVisible = false },
            title = { Text(stringResource(R.string.mesas_create_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Zone: dropdown with existing zones + free text for new ones
                    ExposedDropdownMenuBox(
                        expanded = zonaExpanded,
                        onExpandedChange = { zonaExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = createZona,
                            onValueChange = { createZona = it; zonaExpanded = true },
                            label = { Text(stringResource(R.string.mesas_zone_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zonaExpanded) }
                        )
                        val filtered = if (createZona.isBlank()) zonas
                            else zonas.filter { it.contains(createZona, ignoreCase = true) }
                        ExposedDropdownMenu(
                            expanded = zonaExpanded && filtered.isNotEmpty(),
                            onDismissRequest = { zonaExpanded = false }
                        ) {
                            filtered.forEach { zona ->
                                DropdownMenuItem(
                                    text = { Text("${zonaEmoji(zona)} $zona") },
                                    onClick = { createZona = zona; zonaExpanded = false }
                                )
                            }
                        }
                    }
                    Text(stringResource(R.string.mesas_shape_label), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MesaForma.entries.forEach { forma ->
                            FilterChip(selected = createForma == forma, onClick = { createForma = forma; createCap = forma.capacidadDefecto.toString() },
                                label = { Text(formaLabel(forma)) })
                        }
                    }
                    OutlinedTextField(createCap, { createCap = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.mesas_capacidad_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(createAlias, { createAlias = it }, label = { Text(stringResource(R.string.mesas_alias_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createMesa(createZona.ifBlank { "General" }, createForma, createCap.toIntOrNull() ?: 4, createAlias.ifBlank { null })
                    crearVisible = false
                }) { Text(stringResource(R.string.mesas_create_btn)) }
            },
            dismissButton = { TextButton(onClick = { crearVisible = false }) { Text(stringResource(R.string.menu_cancel)) } }
        )
    }

    // Edit dialog
    mesaEditando?.let { mesa ->
        var editAlias by remember(mesa) { mutableStateOf(mesa.alias ?: "") }
        var editCap by remember(mesa) { mutableStateOf(mesa.capacidad.toString()) }
        var editForma by remember(mesa) { mutableStateOf(mesa.forma) }
        AlertDialog(
            onDismissRequest = { mesaEditando = null },
            title = { Text(stringResource(R.string.mesas_alias_title, mesa.nombreVisible)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editAlias, { editAlias = it }, label = { Text(stringResource(R.string.mesas_alias_label)) }, placeholder = { Text(mesa.numero.toString()) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editCap, { editCap = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.mesas_capacidad_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.mesas_shape_label), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MesaForma.entries.forEach { forma ->
                            FilterChip(
                                selected = editForma == forma,
                                onClick = { editForma = forma },
                                label = { Text(formaLabel(forma)) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateConfig(mesa, editAlias.ifBlank { null }, editCap.toIntOrNull() ?: mesa.capacidad, editForma)
                    mesaEditando = null
                }) { Text(stringResource(R.string.menu_save)) }
            },
            dismissButton = { TextButton(onClick = { mesaEditando = null }) { Text(stringResource(R.string.menu_cancel)) } }
        )
    }

    // Isolated mesa rescue dialog
    mesaAislada?.let { mesa ->
        AlertDialog(
            onDismissRequest = { mesaAislada = null },
            title = { Text(stringResource(R.string.mesas_isolated_title, mesa.nombreVisible)) },
            text = { Text(stringResource(R.string.mesas_isolated_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePosicion(mesa, aisladaFinalX, aisladaFinalY)
                    mesaAislada = null
                }) { Text(stringResource(R.string.mesas_isolated_keep)) }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        viewModel.deleteMesa(mesa)
                        mesaAislada = null
                    }) { Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = {
                        val safe = safePosition(mesas.filter { it.id != mesa.id })
                        viewModel.updatePosicion(mesa, safe.first, safe.second)
                        mesaAislada = null
                    }) { Text(stringResource(R.string.mesas_isolated_bring)) }
                }
            }
        )
    }

    // Delete confirmation
    mesaBorrando?.let { mesa ->
        AlertDialog(
            onDismissRequest = { mesaBorrando = null },
            title = { Text(stringResource(R.string.mesas_delete_title)) },
            text = { Text(stringResource(R.string.mesas_delete_confirm, mesa.nombreVisible)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMesa(mesa)
                    mesaBorrando = null
                }) { Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { mesaBorrando = null }) { Text(stringResource(R.string.menu_cancel)) } }
        )
    }
}
