package com.jaminsmoke.personalcomander.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaEstado
import com.jaminsmoke.personalcomander.data.MesaForma
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

// Grid system: all positions snap to multiples of CELL
internal val CELL = 40.dp
internal val CELL_F = 40f
internal val CARD_WIDTH = 120.dp
internal val CARD_W = 120f

// Área estándar del plano de cada zona (dp): todas las zonas comparten el
// mismo grid fijo. 50×65 celdas de 40dp — suficiente para cualquier
// distribución sin quedar corto, y encaja en pantalla con auto-fit.
internal const val ZONA_ANCHO = 2000f
internal const val ZONA_ALTO = 2600f
internal const val MIN_BOARD_SCALE = 0.08f
internal const val MAX_BOARD_SCALE = 3f

/** Escala necesaria para encajar por completo el grid, conservando un margen visible. */
internal fun calcularEscalaAjuste(
    viewportW: Float,
    viewportH: Float,
    contentW: Float,
    contentH: Float,
    padding: Float = 0f
): Float {
    if (viewportW <= 0f || viewportH <= 0f || contentW <= 0f || contentH <= 0f) return 1f
    val availableW = (viewportW - padding * 2f).coerceAtLeast(1f)
    val availableH = (viewportH - padding * 2f).coerceAtLeast(1f)
    return minOf(availableW / contentW, availableH / contentH)
        .coerceIn(MIN_BOARD_SCALE, MAX_BOARD_SCALE)
}

/**
 * Limita el pan a los bordes del grid. Si el grid cabe en el viewport, lo
 * mantiene centrado en ese eje en lugar de pegarlo a la esquina superior.
 */
internal fun limitarPan(pan: Float, viewport: Float, content: Float): Float {
    if (viewport <= 0f || content <= 0f) return 0f
    return if (content <= viewport) {
        (viewport - content) / 2f
    } else {
        pan.coerceIn(viewport - content, 0f)
    }
}

/** Pan que conserva bajo los dedos el mismo punto del board al hacer zoom. */
internal fun panTrasZoom(
    pan: Float,
    focoAnterior: Float,
    focoActual: Float,
    ratio: Float
): Float = focoActual - (focoAnterior - pan) * ratio

/** Altura en dp de una carta según su forma */
internal fun mesaAltura(forma: MesaForma): Float = when (forma) {
    MesaForma.REDONDA -> CARD_W
    MesaForma.CUADRADA -> CARD_W
    MesaForma.RECTANGULAR -> CARD_W * 0.55f
    MesaForma.RECTANGULAR_XL -> CARD_W * 0.4f
}

internal fun esRectangular(forma: MesaForma) = forma == MesaForma.RECTANGULAR || forma == MesaForma.RECTANGULAR_XL

/** Dimensiones reales (w,h) de una mesa considerando si está girada */
internal fun mesaDims(forma: MesaForma, girada: Boolean): Pair<Float, Float> {
    if (girada && esRectangular(forma)) return mesaAltura(forma) to CARD_W
    return CARD_W to mesaAltura(forma)
}

/** Color de fondo de una carta según su estado */
internal fun mesaColor(estado: MesaEstado): Color = when (estado) {
    MesaEstado.LIBRE -> Color(0xFFC8E6C9)
    MesaEstado.OCUPADA -> Color(0xFFFFE0B2)
    MesaEstado.EN_COCINA -> Color(0xFFB3E5FC)
}

/** Radio de esquinas de una carta según su forma */
internal fun mesaShapeRadius(forma: MesaForma): Dp = when (forma) {
    MesaForma.REDONDA -> 999.dp
    MesaForma.CUADRADA -> 16.dp
    MesaForma.RECTANGULAR -> 14.dp
    MesaForma.RECTANGULAR_XL -> 12.dp
}

/** Detecta si dos rectángulos (x,y,w,h) se solapan (AABB collision) */
internal fun colisionan(
    x1: Float, y1: Float, w1: Float, h1: Float,
    x2: Float, y2: Float, w2: Float, h2: Float
): Boolean = x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MesaCard(
    mesa: Mesa,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDragStarted: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onRotateClick: () -> Unit,
    onPointerActive: (Boolean) -> Unit
) {
    val color = mesaColor(mesa.estado)
    val label = when (mesa.estado) {
        MesaEstado.LIBRE -> stringResource(R.string.mesas_free)
        MesaEstado.OCUPADA -> stringResource(R.string.mesas_occupied)
        MesaEstado.EN_COCINA -> stringResource(R.string.mesas_in_kitchen)
    }
    val tieneComanda = mesa.comandaActivaId != null

    val shapeRadius = mesaShapeRadius(mesa.forma)
    val (cardWf, cardHf) = mesaDims(mesa.forma, mesa.girada)
    val cardHeight = cardHf.dp

    var menuExpanded by remember { mutableStateOf(false) }
    var dragArrancado by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .height(cardHeight)
            .graphicsLayer {
                if (isDragging) alpha = 0.4f
            }
            .pointerInput(mesa.id, "camera-guard") {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPointerActive(true)
                    try {
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    } finally {
                        onPointerActive(false)
                    }
                }
            }
            .pointerInput(mesa.id, "tap") {
                detectTapGestures(onTap = { onClick() })
            }
            .pointerInput(mesa.id, "drag") {
                detectDragGesturesAfterLongPress(
                    onDragStart = { _ ->
                        menuExpanded = true
                        dragArrancado = false
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
                    Text(mesa.idZona, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
                    if (esRectangular(mesa.forma)) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mesas_menu_rotate)) },
                            onClick = { menuExpanded = false; onRotateClick() },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DragOverlayCard(mesa: Mesa) {
    val shapeRadius = mesaShapeRadius(mesa.forma)
    val (cardWf, cardHf) = mesaDims(mesa.forma, mesa.girada)
    val cardHeight = cardHf.dp
    val color = mesaColor(mesa.estado)

    Card(
        modifier = Modifier.width(cardWf.dp).height(cardHeight),
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
 * Respeta los límites del grid de la zona: nunca devuelve una posición
 * que saque la mesa fuera de los bordes (x+w > limiteX, y+h > limiteY).
 */
internal fun findNearestFreeCell(
    targetX: Float,
    targetY: Float,
    draggedW: Float,
    draggedH: Float,
    occupied: List<List<Float>>,
    limiteX: Float = ZONA_ANCHO,
    limiteY: Float = ZONA_ALTO
): Pair<Float, Float> {
    // Rango permitido alineado a celdas completas: [CELL_F, maxGrid].
    val maxX = maxOf(CELL_F, floor((limiteX - draggedW - CELL_F) / CELL_F) * CELL_F)
    val maxY = maxOf(CELL_F, floor((limiteY - draggedH - CELL_F) / CELL_F) * CELL_F)
    val safeX = ((targetX / CELL_F).roundToInt() * CELL_F).coerceIn(CELL_F, maxX)
    val safeY = ((targetY / CELL_F).roundToInt() * CELL_F).coerceIn(CELL_F, maxY)

    fun hayColision(x: Float, y: Float): Boolean = occupied.any { o ->
        colisionan(x, y, draggedW, draggedH, o[0], o[1], o[2], o[3])
    }

    if (!hayColision(safeX, safeY)) return safeX to safeY

    var ring = 1
    while (ring < 50) {
        for (dx in -ring..ring) {
            for (dy in -ring..ring) {
                if (maxOf(abs(dx), abs(dy)) != ring) continue
                val cx = safeX + dx * CELL_F
                val cy = safeY + dy * CELL_F
                if (cx in CELL_F..maxX && cy in CELL_F..maxY && !hayColision(cx, cy)) {
                    return cx to cy
                }
            }
        }
        ring++
    }
    return safeX to safeY
}

/** True si el rectángulo (x,y,w,h) se sale de los límites del grid de la zona */
internal fun estaFueraDeLimites(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    limiteX: Float = ZONA_ANCHO,
    limiteY: Float = ZONA_ALTO
): Boolean =
    x < CELL_F || y < CELL_F || x + w > limiteX - CELL_F || y + h > limiteY - CELL_F

/**
 * Clamp duro: devuelve la posición alineada al grid más cercana DENTRO de los
 * límites de la zona. No busca celda libre — solo recorta al borde válido.
 */
internal fun clampAlBorde(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    limiteX: Float = ZONA_ANCHO,
    limiteY: Float = ZONA_ALTO
): Pair<Float, Float> {
    val maxX = maxOf(CELL_F, floor((limiteX - w - CELL_F) / CELL_F) * CELL_F)
    val maxY = maxOf(CELL_F, floor((limiteY - h - CELL_F) / CELL_F) * CELL_F)
    val cX = ((x / CELL_F).roundToInt() * CELL_F).coerceIn(CELL_F, maxX)
    val cY = ((y / CELL_F).roundToInt() * CELL_F).coerceIn(CELL_F, maxY)
    return cX to cY
}

/**
 * Repara posiciones legacy fuera del grid y solapes, conservando todas las
 * mesas dentro del espacio fijo de su zona.
 */
internal fun normalizarMesasEnGrid(mesas: List<Mesa>): Map<Long, Offset> {
    val posiciones = linkedMapOf<Long, Offset>()
    val ocupadas = mutableListOf<List<Float>>()

    mesas.sortedWith(compareBy<Mesa> { it.indiceZona }.thenBy { it.id }).forEach { mesa ->
        val (w, h) = mesaDims(mesa.forma, mesa.girada)
        val (x, y) = findNearestFreeCell(mesa.posX, mesa.posY, w, h, ocupadas)
        posiciones[mesa.id] = Offset(x, y)
        ocupadas += listOf(x, y, w, h)
    }
    return posiciones
}

/** Detecta si una mesa está demasiado lejos del cluster (Manhattan > 500dp) */
internal fun isIsolated(x: Float, y: Float, draggedId: Long, allMesas: List<Mesa>): Boolean {
    val others = allMesas.filter { it.id != draggedId }
    if (others.isEmpty()) return false
    return others.minOf { abs(x - it.posX) + abs(y - it.posY) } > 500f
}

/**
 * Posición segura cerca del cluster (borde inferior-derecho de las demás
 * mesas), buscando celda libre dentro de los límites del grid.
 */
internal fun traerCerca(allMesas: List<Mesa>): Pair<Float, Float> {
    if (allMesas.isEmpty()) return CELL_F to CELL_F
    val maxX = allMesas.maxOf { it.posX } + CARD_W + CELL_F
    val avgY = allMesas.map { it.posY }.average().toFloat()
    val targetX = (maxX / CELL_F).roundToInt() * CELL_F
    val targetY = (avgY / CELL_F).roundToInt() * CELL_F
    val ocupadas = allMesas.map {
        val (ow, oh) = mesaDims(it.forma, it.girada)
        listOf(it.posX, it.posY, ow, oh)
    }
    return findNearestFreeCell(targetX, targetY, CARD_W, CARD_W, ocupadas)
}

internal fun formaLabel(forma: MesaForma): String = when (forma) {
    MesaForma.REDONDA -> "\u2B55"
    MesaForma.CUADRADA -> "\uD83D\uDFE9"
    MesaForma.RECTANGULAR -> "\uD83D\uDFE6"
    MesaForma.RECTANGULAR_XL -> "\uD83D\uDFEA"
}

internal fun zonaEmoji(zona: String): String = when {
    zona.contains("Terraza", ignoreCase = true) || zona.contains("terraza", ignoreCase = true) -> "\uD83C\uDF1E"
    zona.contains("Interior", ignoreCase = true) || zona.contains("Sal\u00F3n", ignoreCase = true) -> "\uD83C\uDFE0"
    zona.contains("Barra", ignoreCase = true) || zona.contains("Bar", ignoreCase = true) -> "\uD83C\uDF78"
    zona.contains("VIP", ignoreCase = true) || zona.contains("Reservado", ignoreCase = true) -> "\u2B50"
    else -> "\uD83D\uDCCD"
}
