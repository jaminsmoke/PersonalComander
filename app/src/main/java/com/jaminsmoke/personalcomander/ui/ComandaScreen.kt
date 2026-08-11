package com.jaminsmoke.personalcomander.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.MesaEstado
import com.jaminsmoke.personalcomander.data.PedidoEstado
import com.jaminsmoke.personalcomander.data.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComandaScreen(
    mesaId: Long,
    onBack: () -> Unit,
    viewModel: ComandaViewModel = viewModel(
        key = "comanda_$mesaId",
        factory = ComandaViewModel.factory(LocalContext.current.applicationContext, mesaId)
    )
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mesa ${state.mesa?.numero ?: mesaId}")
                        Text(
                            text = estadoLabel(state.mesa?.estado),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.busqueda,
                onValueChange = viewModel::setBusqueda,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("Buscar producto...") },
                singleLine = true
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.categoria == null,
                        onClick = { viewModel.setCategoria(null) },
                        label = { Text("Todas") }
                    )
                }
                items(state.categorias) { cat ->
                    FilterChip(
                        selected = state.categoria == cat,
                        onClick = { viewModel.setCategoria(if (state.categoria == cat) null else cat) },
                        label = { Text(cat) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.productos, key = { it.id }) { producto ->
                    ProductoRow(
                        producto = producto,
                        onClick = { viewModel.addProducto(producto) }
                    )
                }
            }

            ComandaPanel(
                lineas = state.lineas,
                total = state.total,
                pedidoEstado = state.pedido?.estado,
                onAumentar = viewModel::aumentarLinea,
                onDisminuir = viewModel::disminuirLinea,
                onEnviarACocina = viewModel::enviarACocina,
                onCerrarMesa = viewModel::cerrarMesa
            )
        }
    }
}

@Composable
private fun ProductoRow(producto: Producto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = producto.categoria,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = producto.precio.formatoEuro(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ComandaPanel(
    lineas: List<LineaPedido>,
    total: Double,
    pedidoEstado: PedidoEstado?,
    onAumentar: (LineaPedido) -> Unit,
    onDisminuir: (LineaPedido) -> Unit,
    onEnviarACocina: () -> Unit,
    onCerrarMesa: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Comanda",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (lineas.isEmpty()) {
                Text(
                    text = "Sin artículos. Toca un producto para añadirlo.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(lineas, key = { it.id }) { linea ->
                        LineaRow(
                            linea = linea,
                            onAumentar = { onAumentar(linea) },
                            onDisminuir = { onDisminuir(linea) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = total.formatoEuro(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val hayComanda = pedidoEstado != null && lineas.isNotEmpty()
                Button(
                    onClick = onEnviarACocina,
                    modifier = Modifier.weight(1f),
                    enabled = hayComanda && pedidoEstado == PedidoEstado.ABIERTA
                ) {
                    Text(
                        text = when (pedidoEstado) {
                            PedidoEstado.ENVIADA -> "En cocina ✓"
                            else -> "Enviar a cocina"
                        }
                    )
                }
                OutlinedButton(
                    onClick = onCerrarMesa,
                    modifier = Modifier.weight(1f),
                    enabled = pedidoEstado != null
                ) {
                    Text("Cerrar mesa")
                }
            }
        }
    }
}

@Composable
private fun LineaRow(
    linea: LineaPedido,
    onAumentar: () -> Unit,
    onDisminuir: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${linea.cantidad}×",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = linea.nombreProducto,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        Text(
            text = (linea.precioUnitario * linea.cantidad).formatoEuro(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onDisminuir, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Quitar",
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onAumentar, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun estadoLabel(estado: MesaEstado?): String = when (estado) {
    MesaEstado.LIBRE -> "Libre"
    MesaEstado.OCUPADA -> "Ocupada"
    MesaEstado.EN_COCINA -> "En cocina"
    null -> ""
}
