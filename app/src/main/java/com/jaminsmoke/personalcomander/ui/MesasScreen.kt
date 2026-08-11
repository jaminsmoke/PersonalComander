package com.jaminsmoke.personalcomander.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import com.jaminsmoke.personalcomander.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaEstado
import com.jaminsmoke.personalcomander.data.MesaForma

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
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Zone tabs
            if (zonas.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = zonas.indexOf(zonaSeleccionada).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 12.dp,
                    divider = {},
                    indicator = {}
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

            // Table grid
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val cellSize: Dp = when {
                    maxWidth > 800.dp -> 140.dp
                    maxWidth > 500.dp -> 120.dp
                    else -> 100.dp
                }
                if (mesas.isEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = cellSize),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(16) {
                            ShimmerBox(modifier = Modifier.aspectRatio(1f), height = 0, radius = 16)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = cellSize),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mesasFiltradas, key = { it.id }) { mesa ->
                            MesaCard(
                                mesa = mesa,
                                onClick = { onOpenMesa(mesa.id) },
                                onLongPress = { mesaEditando = mesa }
                            )
                        }
                    }
                }
            }
        }
    }

    // Alias edit dialog
    mesaEditando?.let { mesa ->
        var aliasText by remember(mesa) { mutableStateOf(mesa.alias ?: "") }
        var capacidadText by remember(mesa) { mutableStateOf(mesa.capacidad.toString()) }
        AlertDialog(
            onDismissRequest = { mesaEditando = null },
            title = { Text(stringResource(R.string.mesas_alias_title, mesa.nombreVisible)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.mesas_alias_desc))
                    OutlinedTextField(
                        value = aliasText,
                        onValueChange = { aliasText = it },
                        label = { Text(stringResource(R.string.mesas_alias_label)) },
                        placeholder = { Text(mesa.numero.toString()) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = capacidadText,
                        onValueChange = { capacidadText = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.mesas_capacidad_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cap = capacidadText.toIntOrNull() ?: mesa.capacidad
                    viewModel.updateConfig(mesa, aliasText.ifBlank { null }, cap)
                    mesaEditando = null
                }) { Text(stringResource(R.string.menu_save)) }
            },
            dismissButton = {
                TextButton(onClick = { mesaEditando = null }) {
                    Text(stringResource(R.string.menu_cancel))
                }
            }
        )
    }
}

@Composable
private fun MesaCard(mesa: Mesa, onClick: () -> Unit, onLongPress: () -> Unit) {
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

    val (aspecto, shapeRadius) = when (mesa.forma) {
        MesaForma.REDONDA -> 1f to 999.dp
        MesaForma.CUADRADA -> 1f to 16.dp
        MesaForma.RECTANGULAR -> 0.55f to 14.dp
        MesaForma.RECTANGULAR_XL -> 0.4f to 12.dp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspecto)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(shapeRadius),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Box(Modifier.fillMaxSize().padding(if (mesa.forma == MesaForma.REDONDA) 12.dp else 10.dp)) {
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = mesa.nombreVisible,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (mesa.alias != null) {
                    Text(
                        text = "Nº ${mesa.numero}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${mesa.capacidad}p",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tieneComanda) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFFF7043), CircleShape)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End
                )
            }

            // Edit alias button (top right)
            IconButton(
                onClick = onLongPress,
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
            ) {
                Icon(Icons.Default.Edit, stringResource(R.string.btn_edit), Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

private fun zonaEmoji(zona: String): String = when {
    zona.contains("Terraza", ignoreCase = true) || zona.contains("terraza", ignoreCase = true) -> "\uD83C\uDF1E"
    zona.contains("Interior", ignoreCase = true) || zona.contains("Salón", ignoreCase = true) -> "\uD83C\uDFE0"
    zona.contains("Barra", ignoreCase = true) || zona.contains("Bar", ignoreCase = true) -> "\uD83C\uDF78"
    zona.contains("VIP", ignoreCase = true) || zona.contains("Reservado", ignoreCase = true) -> "\u2B50"
    else -> "\uD83D\uDCCD"
}
