package com.jaminsmoke.personalcomander.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.ui.components.BrandHeaderDensity
import com.jaminsmoke.personalcomander.ui.components.PcBrandHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenMesas: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenAjustes: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = scheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PcBrandHeader(
                title = stringResource(R.string.home_title),
                density = BrandHeaderDensity.Hero,
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.home_summary_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.onSurface,
                )
            }
            item {
                if (state.cargando) {
                    ShimmerBox(height = 160, radius = 12)
                } else {
                    ResumenDiaCard(state)
                }
            }
            item {
                Text(
                    text = stringResource(R.string.home_quick_access),
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                )
            }
            item {
                HomeAcceso(
                    titulo = stringResource(R.string.home_tables_card),
                    descripcion = stringResource(R.string.home_tables_desc),
                    icono = Icons.Default.GridView,
                    accent = scheme.secondary,
                    onClick = onOpenMesas,
                )
            }
            item {
                HomeAcceso(
                    titulo = stringResource(R.string.home_menu_card),
                    descripcion = stringResource(R.string.home_menu_desc),
                    icono = Icons.AutoMirrored.Filled.MenuBook,
                    accent = scheme.tertiary,
                    onClick = onOpenMenu,
                )
            }
            item {
                HomeAcceso(
                    titulo = stringResource(R.string.home_settings_card),
                    descripcion = stringResource(R.string.home_settings_desc),
                    icono = Icons.Default.Settings,
                    accent = scheme.primary,
                    onClick = onOpenAjustes,
                )
            }
        }
    }
}

@Composable
private fun ResumenDiaCard(state: HomeUiState) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(scheme.primaryContainer, scheme.surfaceContainerLowest)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        scheme.secondary.copy(alpha = 0.35f),
                        scheme.secondary.copy(alpha = 0.08f),
                    )
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.home_revenue_label).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = state.totalHoy.formatoEuro(),
                    style = MaterialTheme.typography.displayMedium,
                    color = scheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ResumenStat(
                    icono = Icons.Default.TableRestaurant,
                    iconTint = scheme.primary,
                    valor = "${state.mesasOcupadas} / ${state.mesasTotales}",
                    etiqueta = stringResource(R.string.home_tables_label),
                    modifier = Modifier.weight(1f),
                )
                ResumenStat(
                    icono = Icons.AutoMirrored.Filled.ReceiptLong,
                    iconTint = scheme.tertiary,
                    valor = state.pedidosActivos.toString(),
                    etiqueta = stringResource(R.string.home_orders_label),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ResumenStat(
    icono: ImageVector,
    iconTint: Color,
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
        }
    }
}

@Composable
private fun HomeAcceso(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    accent: Color,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.primaryContainer.copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = descripcion,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}
