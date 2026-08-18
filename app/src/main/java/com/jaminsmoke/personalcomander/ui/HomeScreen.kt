package com.jaminsmoke.personalcomander.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.data.sesion.OficioVentana
import com.jaminsmoke.personalcomander.data.sesion.formatoHorasOficio
import com.jaminsmoke.personalcomander.ui.components.BrandHeaderDensity
import com.jaminsmoke.personalcomander.ui.components.PcBrandHeader
import com.jaminsmoke.personalcomander.ui.components.PcSesionChip
import com.jaminsmoke.personalcomander.ui.components.PcTurnoIndicador
import com.jaminsmoke.personalcomander.ui.sesion.SesionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAjustes: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenPerfil: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
    sesionViewModel: SesionViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val oficio by viewModel.oficio.collectAsState()
    val modo by sesionViewModel.modo.collectAsState()
    val fotoSesion by sesionViewModel.foto.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scheme = MaterialTheme.colorScheme
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                sesionViewModel.revalidarTurno()
                viewModel.refrescarOficio()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = scheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PcBrandHeader(
                title = stringResource(R.string.home_title),
                density = BrandHeaderDensity.Compact,
                actions = {
                    PcSesionChip(
                        modo = modo,
                        fotoBytes = fotoSesion,
                        onEntrar = onOpenAuth,
                        onPerfil = onOpenPerfil,
                    )
                },
                supportingContent = {
                    PcTurnoIndicador(
                        modo = modo,
                        onClick = {
                            if (modo is ModoSesion.Local) onOpenAuth() else onOpenAjustes()
                        },
                    )
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
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
                OficioCard(
                    state = oficio,
                    onVentana = viewModel::setVentana,
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
private fun OficioCard(
    state: OficioUiState,
    onVentana: (OficioVentana) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.home_oficio_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OficioChip(
                etiqueta = stringResource(R.string.home_oficio_dia),
                seleccionado = state.ventana == OficioVentana.DIA,
                onClick = { onVentana(OficioVentana.DIA) },
            )
            OficioChip(
                etiqueta = stringResource(R.string.home_oficio_semana),
                seleccionado = state.ventana == OficioVentana.SEMANA,
                onClick = { onVentana(OficioVentana.SEMANA) },
            )
            OficioChip(
                etiqueta = stringResource(R.string.home_oficio_mes),
                seleccionado = state.ventana == OficioVentana.MES,
                onClick = { onVentana(OficioVentana.MES) },
            )
        }
        when {
            !state.conSesion -> Text(
                text = stringResource(R.string.home_oficio_sin_sesion),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            state.error != null -> Text(
                text = stringResource(R.string.home_oficio_error),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.error,
            )
            state.cargando && state.sinActividad -> ShimmerBox(height = 120, radius = 8)
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ResumenStat(
                        icono = Icons.Default.Schedule,
                        iconTint = scheme.secondary,
                        valor = formatoHorasOficio(state.horasSegundos),
                        etiqueta = stringResource(R.string.home_oficio_horas),
                        modifier = Modifier.weight(1f),
                    )
                    ResumenStat(
                        icono = Icons.Default.RoomService,
                        iconTint = scheme.tertiary,
                        valor = state.rondasServidas.toString(),
                        etiqueta = stringResource(R.string.home_oficio_rondas),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (state.sinActividad) {
                    Text(
                        text = stringResource(R.string.home_oficio_vacio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                } else if (state.horasPorDia.any { it.segundos > 0 }) {
                    Text(
                        text = stringResource(R.string.home_oficio_grafica),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    OficioHorasChart(puntos = state.horasPorDia)
                }
            }
        }
    }
}

@Composable
private fun OficioChip(
    etiqueta: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(etiqueta) },
    )
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
