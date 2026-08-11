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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaEstado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasScreen(
    onOpenMesa: (Long) -> Unit,
    onOpenMenu: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: MesasViewModel = viewModel()
) {
    val mesas by viewModel.mesas.collectAsState(initial = emptyList())
    val mensaje by viewModel.mensaje.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Personal Comander") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = "Gestionar menú"
                        )
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val cellSize: Dp = when {
                maxWidth > 800.dp -> 130.dp   // tablet landscape
                maxWidth > 500.dp -> 120.dp   // tablet portrait / phone landscape
                else -> 100.dp                // phone portrait
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
                    items(mesas, key = { it.id }) { mesa ->
                        MesaCard(mesa = mesa, onClick = { onOpenMesa(mesa.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MesaCard(mesa: Mesa, onClick: () -> Unit) {
    val color = when (mesa.estado) {
        MesaEstado.LIBRE -> Color(0xFFC8E6C9)
        MesaEstado.OCUPADA -> Color(0xFFFFE0B2)
        MesaEstado.EN_COCINA -> Color(0xFFB3E5FC)
    }
    val label = when (mesa.estado) {
        MesaEstado.LIBRE -> "Libre"
        MesaEstado.OCUPADA -> "Ocupada"
        MesaEstado.EN_COCINA -> "En cocina"
    }
    val tieneComanda = mesa.comandaActivaId != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mesa.numero.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (tieneComanda) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = Color(0xFFFF7043),
                                shape = CircleShape
                            )
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
