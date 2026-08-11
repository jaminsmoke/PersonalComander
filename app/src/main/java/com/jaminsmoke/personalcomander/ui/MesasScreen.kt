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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaEstado
import com.jaminsmoke.personalcomander.data.MesaForma
import kotlin.math.abs
import kotlin.math.roundToInt

// Grid system: all positions snap to multiples of CELL
private val CELL = 40.dp
private val CELL_F = 40f
private val CARD_WIDTH = 120.dp
private val CARD_W = 120f

/** Altura en dp de una carta según su forma (misma lógica que MesaCard) */
private fun mesaAltura(forma: MesaForma): Float = when (forma) {
    MesaForma.REDONDA -> CARD_W
    MesaForma.CUADRADA -> CARD_W
    MesaForma.RECTANGULAR -> CARD_W * 0.55f
    MesaForma.RECTANGULAR_XL -> CARD_W * 0.4f
}

/** Detecta si dos rectángulos (x,y,w,h) se solapan (AABB collision) */
private fun colisionan(
    x1: Float, y1: Float, w1: Float, h1: Float,
    x2: Float, y2: Float, w2: Float, h2: Float
): Boolean = x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasScreen(
    onOpenMesa: (Long) -> Unit,
    onOpenMenu: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: MesasViewModel = viewModel()
) {
    val mesas by viewModel.mesas.collectAsState(initial = emptyList())
    val zonaSeleccionada by viewModel.zona.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var mesaEditando by remember { mutableStateOf<Mesa?>(null) }
    var mesaBorrando by remember { mutableStateOf<Mesa?>(null) }
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

                // Board
                val scrollH = rememberScrollState()
                val scrollV = rememberScrollState()
                val maxX = ((mesasFiltradas.maxOfOrNull { it.posX } ?: 0f) + CARD_WIDTH.value + CELL_F * 2).coerceAtLeast(400f)
                val maxY = ((mesasFiltradas.maxOfOrNull { it.posY } ?: 0f) + CARD_WIDTH.value + CELL_F * 2).coerceAtLeast(600f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                        // Shimmer loading
                        if (mesas.isEmpty()) {
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

                                MesaCard(
                                    mesa = mesa,
                                    isDragging = isDragging,
                                    modifier = Modifier
                                        .offset(x = animX.dp, y = animY.dp)
                                        .width(CARD_WIDTH),
                                    onClick = {
                                        if (draggedMesa == null) onOpenMesa(mesa.id)
                                    },
                                    onEditClick = { mesaEditando = mesa },
                                    onDeleteClick = { mesaBorrando = mesa },
                                    onDragStart = { },
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
                                            // Snap to grid
                                            val snappedX = (rawX / CELL_F).roundToInt() * CELL_F
                                            val snappedY = (rawY / CELL_F).roundToInt() * CELL_F
                                            // Anti-colisión con bounding boxes (no solo centros)
                                            val draggedH = mesaAltura(dragged.forma)
                                            val occupied = mesas
                                                .filter { it.id != dragged.id }
                                                .map { Triple(it.posX, it.posY, mesaAltura(it.forma)) }
                                            val (finalX, finalY) = findNearestFreeCell(
                                                snappedX, snappedY, CARD_W, draggedH, occupied
                                            )
                                            viewModel.updatePosicion(dragged, finalX, finalY)
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
                            Box(
                                modifier = Modifier
                                    .offset(x = overlayX.dp, y = overlayY.dp)
                                    .graphicsLayer {
                                        scaleX = 1.08f
                                        scaleY = 1.08f
                                        shadowElevation = 16f
                                        alpha = 0.92f
                                    }
                                    .width(CARD_WIDTH)
                                    .zIndex(10f)
                            ) {
                                DragOverlayCard(mesa)
                            }
                        }
                    }

                    // Zoom indicator badge (bottom-left) — tap to reset
                    if (scale != 1f) {
                        val pct = (scale * 100).toInt()
                        Card(
                            onClick = { scale = 1f },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .zIndex(5f),
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

    // Create dialog
    if (crearVisible) {
        var createZona by remember { mutableStateOf(zonaSeleccionada ?: zonas.firstOrNull() ?: "") }
        var createForma by remember { mutableStateOf(MesaForma.CUADRADA) }
        var createCap by remember { mutableStateOf("4") }
        var createAlias by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { crearVisible = false },
            title = { Text(stringResource(R.string.mesas_create_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(createZona, { createZona = it }, label = { Text(stringResource(R.string.mesas_zone_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MesaCard(
    mesa: Mesa,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDragStart: () -> Unit,
    onDragStarted: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val color = when (mesa.estado) {
        MesaEstado.LIBRE -> Color(0xFFC8E6C9)
        MesaEstado.OCUPADA -> Color(0xFFFFE0B2)
        MesaEstado.EN_COCINA -> Color(0xFFB3E5FC)
    }
    val label = when (mesa.estado) {
        MesaEstado.LIBRE -> stringResource(R.string.mesas_free)
        MesaEstado.OCUPADA -> stringResource(R.string.mesas_occupied)
        MesaEstado.EN_COCINA -> stringResource(R.string.mesas_in_kitchen)
    }
    val tieneComanda = mesa.comandaActivaId != null

    val shapeRadius = when (mesa.forma) {
        MesaForma.REDONDA -> 999.dp
        MesaForma.CUADRADA -> 16.dp
        MesaForma.RECTANGULAR -> 14.dp
        MesaForma.RECTANGULAR_XL -> 12.dp
    }
    val cardHeight = when (mesa.forma) {
        MesaForma.REDONDA -> CARD_WIDTH
        MesaForma.CUADRADA -> CARD_WIDTH
        MesaForma.RECTANGULAR -> CARD_WIDTH * 0.55f
        MesaForma.RECTANGULAR_XL -> CARD_WIDTH * 0.4f
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var dragArrancado by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .height(cardHeight)
            .graphicsLayer {
                if (isDragging) alpha = 0.4f
            }
            .pointerInput(mesa.id, "tap") {
                detectTapGestures(onTap = { onClick() })
            }
            .pointerInput(mesa.id, "drag") {
                detectDragGesturesAfterLongPress(
                    onDragStart = { _ ->
                        menuExpanded = true
                        dragArrancado = false
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!dragArrancado) {
                            dragArrancado = true
                            menuExpanded = false
                            onDragStarted()
                        }
                        onDrag(dragAmount)
                    },
                    onDragEnd = {
                        if (dragArrancado) {
                            onDragEnd()
                            dragArrancado = false
                        }
                    },
                    onDragCancel = {
                        if (dragArrancado) {
                            onDragEnd()
                            dragArrancado = false
                        }
                    }
                )
            },
        shape = RoundedCornerShape(shapeRadius),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        val isRound = mesa.forma == MesaForma.REDONDA
        Box(
            Modifier
                .fillMaxSize()
                .padding(if (isRound) 16.dp else 8.dp),
            contentAlignment = if (isRound) Alignment.Center else Alignment.TopStart
        ) {
            Column(
                modifier = if (isRound) Modifier.align(Alignment.Center) else Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalAlignment = if (isRound) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(
                    text = mesa.nombreVisible,
                    style = if (isRound) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (mesa.alias != null) {
                    Text("Nº ${mesa.numero}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Bottom info inline (for round: below name)
                if (isRound) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${mesa.capacidad}p", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (tieneComanda) Box(Modifier.size(6.dp).background(Color(0xFFFF7043), CircleShape))
                        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
            // Bottom info for non-round: at bottom-end
            if (!isRound) {
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${mesa.capacidad}p", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (tieneComanda) Box(Modifier.size(8.dp).background(Color(0xFFFF7043), CircleShape))
                    Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                }
            }

            Box(Modifier.align(Alignment.TopEnd).size(24.dp)) {
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.mesas_menu_edit)) },
                        onClick = { menuExpanded = false; onEditClick() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.mesas_menu_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDeleteClick() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DragOverlayCard(mesa: Mesa) {
    val shapeRadius = when (mesa.forma) {
        MesaForma.REDONDA -> 999.dp
        MesaForma.CUADRADA -> 16.dp
        MesaForma.RECTANGULAR -> 14.dp
        MesaForma.RECTANGULAR_XL -> 12.dp
    }
    val cardHeight = when (mesa.forma) {
        MesaForma.REDONDA -> CARD_WIDTH
        MesaForma.CUADRADA -> CARD_WIDTH
        MesaForma.RECTANGULAR -> CARD_WIDTH * 0.55f
        MesaForma.RECTANGULAR_XL -> CARD_WIDTH * 0.4f
    }
    val color = when (mesa.estado) {
        MesaEstado.LIBRE -> Color(0xFFC8E6C9)
        MesaEstado.OCUPADA -> Color(0xFFFFE0B2)
        MesaEstado.EN_COCINA -> Color(0xFFB3E5FC)
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(cardHeight),
        shape = RoundedCornerShape(shapeRadius),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = mesa.nombreVisible,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Busca la posición libre más cercana con búsqueda en espiral.
 * Comprueba colisión de bounding boxes completos (no solo centros).
 * occupied: lista de (x, y, altura) de las mesas ya colocadas.
 */
private fun findNearestFreeCell(
    targetX: Float,
    targetY: Float,
    draggedW: Float,
    draggedH: Float,
    occupied: List<Triple<Float, Float, Float>>
): Pair<Float, Float> {
    fun hayColision(x: Float, y: Float): Boolean = occupied.any { (ox, oy, oh) ->
        colisionan(x, y, draggedW, draggedH, ox, oy, CARD_W, oh)
    }

    if (!hayColision(targetX, targetY)) return targetX to targetY

    var ring = 1
    while (ring < 50) {
        for (dx in -ring..ring) {
            for (dy in -ring..ring) {
                if (maxOf(abs(dx), abs(dy)) != ring) continue
                val cx = targetX + dx * CELL_F
                val cy = targetY + dy * CELL_F
                if (cx >= 0 && cy >= 0 && !hayColision(cx, cy)) {
                    return cx to cy
                }
            }
        }
        ring++
    }
    return targetX to targetY
}

private fun formaLabel(forma: MesaForma): String = when (forma) {
    MesaForma.REDONDA -> "\u2B55"
    MesaForma.CUADRADA -> "\uD83D\uDFE9"
    MesaForma.RECTANGULAR -> "\uD83D\uDFE6"
    MesaForma.RECTANGULAR_XL -> "\uD83D\uDFEA"
}

private fun zonaEmoji(zona: String): String = when {
    zona.contains("Terraza", ignoreCase = true) || zona.contains("terraza", ignoreCase = true) -> "\uD83C\uDF1E"
    zona.contains("Interior", ignoreCase = true) || zona.contains("Sal\u00F3n", ignoreCase = true) -> "\uD83C\uDFE0"
    zona.contains("Barra", ignoreCase = true) || zona.contains("Bar", ignoreCase = true) -> "\uD83C\uDF78"
    zona.contains("VIP", ignoreCase = true) || zona.contains("Reservado", ignoreCase = true) -> "\u2B50"
    else -> "\uD83D\uDCCD"
}
