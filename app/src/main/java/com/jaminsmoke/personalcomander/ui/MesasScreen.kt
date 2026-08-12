package com.jaminsmoke.personalcomander.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.key
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaEstado
import com.jaminsmoke.personalcomander.data.MesaForma
import com.jaminsmoke.personalcomander.ui.components.PcGoldFab
import com.jaminsmoke.personalcomander.ui.components.StatusChip
import com.jaminsmoke.personalcomander.ui.theme.PcBoardCanvas
import com.jaminsmoke.personalcomander.ui.theme.PcBoardGrid
import com.jaminsmoke.personalcomander.ui.theme.PcBoardGridMajor
import com.jaminsmoke.personalcomander.ui.theme.PcComandaDot
import com.jaminsmoke.personalcomander.ui.theme.mesaAccent
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val undoMsg = stringResource(R.string.mesas_undo_snackbar)
    val undoAct = stringResource(R.string.mesas_undo_move)
    val occupiedMsg = stringResource(R.string.mesas_undo_cell_occupied)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Undo state: guarda la posición anterior del último movimiento
    var undoMesaId by remember { mutableStateOf<Long?>(null) }
    var undoPrevX by remember { mutableFloatStateOf(0f) }
    var undoPrevY by remember { mutableFloatStateOf(0f) }

    // Toggle vista: lista de tarjetas (todas las mesas) vs canvas individual por zona
    var vistaLista by remember { mutableStateOf(zonaSeleccionada == null) }
    val mostrarLista = zonaSeleccionada == null || vistaLista

    // Zoom + pan state (cámara libre 2D)
    var scale by remember { mutableStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var boardAutoFitado by remember { mutableStateOf(false) }

    LaunchedEffect(zonaSeleccionada) {
        if (zonaSeleccionada != null) vistaLista = false
        // Reset de cámara al cambiar de zona (evita zoom/pan heredados)
        scale = 1f
        panX = 0f
        panY = 0f
        boardAutoFitado = false
    }

    // Drag state + optimistic positions (previene snap-back mientras Room actualiza)
    val optimisticPos = remember { androidx.compose.runtime.mutableStateMapOf<Long, Offset>() }
    val mesasConPuntero = remember { androidx.compose.runtime.mutableStateMapOf<Long, Boolean>() }
    var draggedMesa by remember { mutableStateOf<Mesa?>(null) }
    var dragBaseX by remember { mutableStateOf(0f) }
    var dragBaseY by remember { mutableStateOf(0f) }
    var dragPxX by remember { mutableStateOf(0f) }
    var dragPxY by remember { mutableStateOf(0f) }

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
            PcGoldFab(onClick = { crearVisible = true })
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
                    if (zonaSeleccionada != null) {
                        IconButton(onClick = { vistaLista = !vistaLista }) {
                            Icon(
                                if (vistaLista) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                                if (vistaLista) stringResource(R.string.mesas_view_board) else stringResource(R.string.mesas_view_list)
                            )
                        }
                    }
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
                    PrimaryScrollableTabRow(
                        selectedTabIndex = if (zonaSeleccionada != null) zonas.indexOf(zonaSeleccionada) + 1 else 0,
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 12.dp
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

                // Empty state: no hay mesas y no está cargando
                if (!cargando && mesasFiltradas.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.TableChart,
                        title = stringResource(R.string.empty_mesas_title),
                        subtitle = stringResource(R.string.empty_mesas_subtitle),
                        modifier = Modifier.weight(1f),
                        actionLabel = stringResource(R.string.empty_mesas_action),
                        onAction = { crearVisible = true }
                    )
                } else if (!mostrarLista) {
                // Board fijo compartido por todas las zonas: ninguna mesa ni la
                // cámara amplían el espacio acordado de 2000×2600dp.
                val maxX = ZONA_ANCHO
                val maxY = ZONA_ALTO

                // Clampa el pan para que el contenido no se pierda fuera de la vista:
                // centrado cuando cabe y limitado a sus bordes cuando es más grande.
                // Bounds vivos para el gesto (pointerInput no se recrea en recomposición).
                val boundsState = rememberUpdatedState(maxX to maxY)
                fun clampPan(targetScale: Float = scale) {
                    val (mx, my) = boundsState.value
                    val contentW = with(density) { mx.dp.toPx() } * targetScale
                    val contentH = with(density) { my.dp.toPx() } * targetScale
                    val edgeMargin = with(density) { CAMERA_EDGE_MARGIN.toPx() }
                    panX = limitarPan(panX, viewportSize.width.toFloat(), contentW, edgeMargin)
                    panY = limitarPan(panY, viewportSize.height.toFloat(), contentH, edgeMargin)
                }

                // Auto-fit: encuadra el grid completo de la zona en el viewport
                val autoFit = {
                    if (viewportSize.width > 0 && viewportSize.height > 0) {
                        val contentW = with(density) { ZONA_ANCHO.dp.toPx() }
                        val contentH = with(density) { ZONA_ALTO.dp.toPx() }
                        val fitPadding = with(density) { 12.dp.toPx() }
                        val newScale = calcularEscalaAjuste(
                            viewportSize.width.toFloat(), viewportSize.height.toFloat(),
                            contentW, contentH, fitPadding
                        )
                        scale = newScale
                        panX = (viewportSize.width - contentW * newScale) / 2f
                        panY = (viewportSize.height - contentH * newScale) / 2f
                        clampPan(newScale)
                    }
                }

                val zoomBy = { factor: Float ->
                    if (viewportSize.width > 0 && viewportSize.height > 0) {
                        val oldScale = scale
                        val newScale = (oldScale * factor).coerceIn(MIN_BOARD_SCALE, MAX_BOARD_SCALE)
                        val focusX = viewportSize.width / 2f
                        val focusY = viewportSize.height / 2f
                        val ratio = newScale / oldScale
                        panX = panTrasZoom(panX, focusX, focusX, ratio)
                        panY = panTrasZoom(panY, focusY, focusY, ratio)
                        scale = newScale
                        clampPan(newScale)
                    }
                }

                // Auto-fit automático la primera vez que se muestra el board con datos
                LaunchedEffect(viewportSize, mostrarLista, mesasFiltradas) {
                    if (!mostrarLista && viewportSize.width > 0 && mesasFiltradas.isNotEmpty() && !boardAutoFitado) {
                        autoFit()
                        boardAutoFitado = true
                    }
                }

                val scheme = MaterialTheme.colorScheme
                // Plano claro (sepia); viewport exterior oscuro — contraste con mesas PcMesaFill.
                val boardCanvasColor = PcBoardCanvas
                val boardGridColor = PcBoardGrid
                val boardMajorColor = PcBoardGridMajor
                val boardGlowColor = scheme.secondary.copy(alpha = 0.18f)
                val boardBorderColor = scheme.secondary.copy(alpha = 0.55f)
                val boardCoreColor = scheme.secondary.copy(alpha = 0.85f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.surfaceContainerLowest)
                        .clipToBounds()
                        .onSizeChanged { viewportSize = it }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                // Escuchamos en Final: el drag de una mesa tiene
                                // prioridad y la cámara solo usa movimiento libre.
                                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Final)
                                    val changes = event.changes.filter { it.pressed }
                                    if (changes.isEmpty()) break
                                    when {
                                        mesasConPuntero.isEmpty() &&
                                            changes.size >= 2 && changes.none { it.isConsumed } -> {
                                            val c1 = changes[0]
                                            val c2 = changes[1]
                                            val prevDist = (c1.previousPosition - c2.previousPosition).getDistance()
                                            val dist = (c1.position - c2.position).getDistance()
                                            if (prevDist > 0f && dist > 0f) {
                                                val prevCentroid = (c1.previousPosition + c2.previousPosition) / 2f
                                                val centroid = (c1.position + c2.position) / 2f
                                                val oldS = scale
                                                val newS = (oldS * (dist / prevDist))
                                                    .coerceIn(MIN_BOARD_SCALE, MAX_BOARD_SCALE)
                                                val ratio = newS / oldS
                                                panX = panTrasZoom(panX, prevCentroid.x, centroid.x, ratio)
                                                panY = panTrasZoom(panY, prevCentroid.y, centroid.y, ratio)
                                                scale = newS
                                                clampPan(newS)
                                            }
                                            changes.forEach { it.consume() }
                                        }
                                        mesasConPuntero.isEmpty() && changes.size == 1 -> {
                                            val c = changes[0]
                                            if (!c.isConsumed) {
                                                val delta = c.position - c.previousPosition
                                                if (delta.x != 0f || delta.y != 0f) {
                                                    panX += delta.x
                                                    panY += delta.y
                                                    clampPan()
                                                    c.consume()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            // `requiredSize` es esencial: el mundo conserva sus
                            // 2000×2600dp antes de que la cámara lo transforme.
                            // `wrapContentSize` evita que Compose centre internamente
                            // el excedente: el origen del mundo permanece en (0,0).
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .requiredSize(width = maxX.dp, height = maxY.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0f, 0f)
                                translationX = panX
                                translationY = panY
                                scaleX = scale
                                scaleY = scale
                            }
                            .drawWithCache {
                                val spacing = CELL.toPx()
                                val majorSpacing = spacing * 5f
                                val canvasColor = boardCanvasColor
                                val gridColor = boardGridColor
                                val majorColor = boardMajorColor
                                val glowColor = boardGlowColor
                                val accentColor = boardBorderColor
                                val coreColor = boardCoreColor
                                val dotRadius = (1.15.dp.toPx() / scale).coerceIn(0.7f, 7f)
                                val majorStroke = (0.65.dp.toPx() / scale).coerceIn(0.5f, 5f)
                                val glowW = (12.dp.toPx() / scale).coerceIn(4f, 90f)
                                val accentW = (2.dp.toPx() / scale).coerceIn(1f, 24f)
                                val coreW = (1.dp.toPx() / scale).coerceIn(0.8f, 10f)

                                onDrawBehind {
                                    drawRect(canvasColor)

                                    // Guías mayores: bloques de 5×5 celdas.
                                    var majorX = majorSpacing
                                    while (majorX < size.width) {
                                        drawLine(majorColor, Offset(majorX, 0f), Offset(majorX, size.height), majorStroke)
                                        majorX += majorSpacing
                                    }
                                    var majorY = majorSpacing
                                    while (majorY < size.height) {
                                        drawLine(majorColor, Offset(0f, majorY), Offset(size.width, majorY), majorStroke)
                                        majorY += majorSpacing
                                    }

                                    // Puntos de anclaje de cada celda.
                                    var x = spacing
                                    while (x < size.width) {
                                        var y = spacing
                                        while (y < size.height) {
                                            drawCircle(gridColor, dotRadius, Offset(x, y))
                                            y += spacing
                                        }
                                        x += spacing
                                    }

                                    drawRect(
                                        color = glowColor,
                                        topLeft = Offset(glowW / 2f, glowW / 2f),
                                        size = Size(size.width - glowW, size.height - glowW),
                                        style = Stroke(width = glowW)
                                    )
                                    drawRect(
                                        color = accentColor,
                                        topLeft = Offset(accentW / 2f, accentW / 2f),
                                        size = Size(size.width - accentW, size.height - accentW),
                                        style = Stroke(width = accentW)
                                    )
                                    drawRect(
                                        color = coreColor,
                                        topLeft = Offset(coreW / 2f, coreW / 2f),
                                        size = Size(size.width - coreW, size.height - coreW),
                                        style = Stroke(width = coreW)
                                    )
                                }
                            }
                    ) {
                        // Shimmer loading — only while data hasn't arrived yet
                        if (cargando) {
                            for (i in 0 until 12) {
                                MesaShimmerBox(
                                    modifier = Modifier
                                        .offset(x = (CELL_F + (i % 4) * 160).dp, y = (CELL_F + (i / 4) * 160).dp)
                                )
                            }
                        }

                        // Render each mesa with animated position
                        mesasFiltradas.forEach { mesa ->
                            key(mesa.id) {
                                val isDragging = draggedMesa?.id == mesa.id

                                // Limpiar posición optimista cuando la BD confirme el cambio
                                LaunchedEffect(mesa.posX, mesa.posY) {
                                    optimisticPos[mesa.id]?.let { opt ->
                                        if (abs(opt.x - mesa.posX) < 1f && abs(opt.y - mesa.posY) < 1f) {
                                            optimisticPos.remove(mesa.id)
                                        }
                                    }
                                }

                                // Usar posición optimista mientras la BD actualiza, si no la real
                                val targetX = optimisticPos[mesa.id]?.x ?: mesa.posX
                                val targetY = optimisticPos[mesa.id]?.y ?: mesa.posY

                                // Animate position changes (smoothly interpolate when DB updates posX/posY)
                                val animX by animateFloatAsState(
                                    targetValue = targetX,
                                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                                    label = "posX"
                                )
                                val animY by animateFloatAsState(
                                    targetValue = targetY,
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
                                    onPointerActive = { active ->
                                        if (active) mesasConPuntero[mesa.id] = true
                                        else mesasConPuntero.remove(mesa.id)
                                    },
                                    onDragStarted = {
                                        draggedMesa = mesa
                                        dragBaseX = targetX
                                        dragBaseY = targetY
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
                                            val occupied = mesasFiltradas.filter { it.id != dragged.id }.map {
                                                val (ow, oh) = mesaDims(it.forma, it.girada)
                                                listOf(it.posX, it.posY, ow, oh)
                                            }
                                            // El borde del grid es duro: incluso si el dedo sale
                                            // del viewport, la mesa termina en una celda válida.
                                            val (boundedX, boundedY) = clampAlBorde(
                                                snappedX, snappedY, draggedW, draggedH
                                            )
                                            val (rawFinalX, rawFinalY) = findNearestFreeCell(
                                                boundedX, boundedY, draggedW, draggedH, occupied
                                            )
                                    // Warning: mesa muy alejada del cluster (solo contra su zona visible)
                                    if (isIsolated(rawFinalX, rawFinalY, dragged.id, mesasFiltradas)) {
                                        mesaAislada = dragged
                                        aisladaFinalX = rawFinalX
                                        aisladaFinalY = rawFinalY
                                    } else if (rawFinalX != targetX || rawFinalY != targetY) {
                                        // Guardar posición anterior para undo
                                        undoMesaId = dragged.id
                                        undoPrevX = targetX
                                        undoPrevY = targetY
                                        optimisticPos[dragged.id] = Offset(rawFinalX, rawFinalY)
                                        viewModel.updatePosicion(dragged, rawFinalX, rawFinalY)
                                        // Mostrar Snackbar con undo
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = undoMsg,
                                                actionLabel = undoAct,
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed && undoMesaId == dragged.id) {
                                                // Verificar si la celda anterior está ocupada
                                                val ocupadas = mesasFiltradas.filter { it.id != dragged.id }.map {
                                                    val (ow, oh) = mesaDims(it.forma, it.girada)
                                                    listOf(it.posX, it.posY, ow, oh)
                                                }
                                                val (mw, mh) = mesaDims(dragged.forma, dragged.girada)
                                                val libre = !ocupadas.any { (ox, oy, ow, oh) ->
                                                    rawFinalX < ox + ow && rawFinalX + mw > ox &&
                                                    rawFinalY < oy + oh && rawFinalY + mh > oy
                                                }
                                                if (libre) {
                                                    optimisticPos[dragged.id] = Offset(undoPrevX, undoPrevY)
                                                    viewModel.updatePosicion(dragged, undoPrevX, undoPrevY)
                                                } else {
                                                    snackbarHostState.showSnackbar(occupiedMsg)
                                                }
                                                undoMesaId = null
                                            }
                                        }
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

                    // Controles de cámara siempre visibles y con objetivos táctiles de 48dp.
                    Card(
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).zIndex(5f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { zoomBy(1f / 1.25f) },
                                enabled = scale > MIN_BOARD_SCALE
                            ) {
                                Icon(Icons.Default.ZoomOut, stringResource(R.string.mesas_zoom_out))
                            }
                            Text(
                                "${(scale * 100).roundToInt()}%",
                                modifier = Modifier.width(52.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { zoomBy(1.25f) },
                                enabled = scale < MAX_BOARD_SCALE
                            ) {
                                Icon(Icons.Default.ZoomIn, stringResource(R.string.mesas_zoom_in))
                            }
                            IconButton(onClick = { autoFit() }) {
                                Icon(Icons.Default.FitScreen, stringResource(R.string.mesas_fit_grid))
                            }
                        }
                    }
                }
                } else {
                    MesasListaView(
                        mesas = mesasFiltradas,
                        onOpenMesa = onOpenMesa,
                        onEdit = { mesaEditando = it },
                        onDelete = { mesaBorrando = it }
                    )
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
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
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
                    OutlinedTextField(editAlias, { editAlias = it }, label = { Text(stringResource(R.string.mesas_alias_label)) }, placeholder = { Text(mesa.idZona) }, singleLine = true, modifier = Modifier.fillMaxWidth())
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

    // Mesa aislada dentro del grid — modal de confirmación
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
                        val (mw, mh) = mesaDims(mesa.forma, mesa.girada)
                        val (tx, ty) = traerCerca(
                            mesasFiltradas.filter { it.id != mesa.id }, mw, mh
                        )
                        viewModel.updatePosicion(mesa, tx, ty)
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

/**
 * Vista de lista: todas las mesas organizadas por zona en tarjetas.
 * Muestra el ID de zona (B1, T2…) o el alias del usuario si existe.
 */
@Composable
private fun MesasListaView(
    mesas: List<Mesa>,
    onOpenMesa: (Long) -> Unit,
    onEdit: (Mesa) -> Unit,
    onDelete: (Mesa) -> Unit
) {
    val porZona = mesas.groupBy { it.zona }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        porZona.forEach { (zona, mesasZona) ->
            item(key = "zona_$zona") {
                Text(
                    "${zonaEmoji(zona)} $zona",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
            items(mesasZona, key = { it.id }) { mesa ->
                MesaListaCard(mesa, onOpenMesa, onEdit, onDelete)
            }
        }
    }
}

@Composable
private fun MesaListaCard(
    mesa: Mesa,
    onOpenMesa: (Long) -> Unit,
    onEdit: (Mesa) -> Unit,
    onDelete: (Mesa) -> Unit
) {
    val accent = MaterialTheme.colorScheme.mesaAccent(mesa.estado)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMesa(mesa.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(accent, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    mesa.nombreVisible,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${formaLabel(mesa.forma)} ${mesa.capacidad}p · ${mesaEstadoLabel(mesa)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(text = mesaEstadoLabel(mesa), accent = accent)
            if (mesa.comandaActivaId != null) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(8.dp).background(PcComandaDot, CircleShape))
            }
            IconButton(onClick = { onEdit(mesa) }) {
                Icon(Icons.Default.Edit, stringResource(R.string.mesas_menu_edit))
            }
            IconButton(onClick = { onDelete(mesa) }) {
                Icon(Icons.Default.Delete, stringResource(R.string.mesas_menu_delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun mesaEstadoLabel(mesa: Mesa): String = when (mesa.estado) {
    MesaEstado.LIBRE -> stringResource(R.string.mesas_free)
    MesaEstado.OCUPADA -> stringResource(R.string.mesas_occupied)
    MesaEstado.EN_COCINA -> stringResource(R.string.mesas_in_kitchen)
}
