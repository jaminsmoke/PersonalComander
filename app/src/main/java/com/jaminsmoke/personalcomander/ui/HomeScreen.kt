package com.jaminsmoke.personalcomander.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.jaminsmoke.personalcomander.data.sesion.LanLocalAspecto
import com.jaminsmoke.personalcomander.data.sesion.LanLocalUi
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
    onOpenAuth: () -> Unit,
    onOpenPerfil: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
    sesionViewModel: SesionViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val oficio by viewModel.oficio.collectAsState()
    val lan by viewModel.lan.collectAsState()
    val modo by sesionViewModel.modo.collectAsState()
    val fotoSesion by sesionViewModel.foto.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scheme = MaterialTheme.colorScheme
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(lan.mensaje) {
        lan.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajeLan()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                sesionViewModel.revalidarTurno()
                viewModel.refrescarOficio()
                viewModel.sondearLan()
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
                        onClick = if (modo is ModoSesion.Local) onOpenAuth else null,
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
                LanRadarCard(
                    state = lan,
                    onPulsar = viewModel::alPulsarLocal,
                )
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
private fun LanRadarCard(
    state: LanRadarUiState,
    onPulsar: (LanLocalUi) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(scheme.surfaceContainer, scheme.surfaceContainerLowest)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        scheme.secondary.copy(alpha = 0.28f),
                        scheme.secondary.copy(alpha = 0.06f),
                    )
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.home_lan_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            if (!state.conSesion) {
                Text(
                    text = stringResource(R.string.home_lan_entrar),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            } else if (state.escaneando && state.locales.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.home_lan_escaneando),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            } else if (state.locales.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_lan_vacio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            } else {
                if (state.escaneando || state.ocupado) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                state.locales.forEach { local ->
                    LanLocalFila(
                        local = local,
                        enabled = !state.ocupado && !state.escaneando,
                        onClick = { onPulsar(local) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanLocalFila(
    local: LanLocalUi,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val color = when (local.aspecto) {
        LanLocalAspecto.APAGADO -> scheme.outline
        LanLocalAspecto.AMARILLO -> scheme.secondary
        LanLocalAspecto.VERDE -> scheme.tertiary
        LanLocalAspecto.ROJO -> scheme.error
    }
    val estado = stringResource(
        when (local.aspecto) {
            LanLocalAspecto.APAGADO -> R.string.home_lan_estado_apagado
            LanLocalAspecto.AMARILLO -> R.string.home_lan_estado_amarillo
            LanLocalAspecto.VERDE -> R.string.home_lan_estado_verde
            LanLocalAspecto.ROJO -> R.string.home_lan_estado_rojo
        },
    )
    val nombre = local.nombre.ifBlank { stringResource(R.string.home_lan_local_sin_nombre) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = estado,
                style = MaterialTheme.typography.bodySmall,
                color = color,
            )
        }
    }
}

@Composable
private fun OficioCard(
    state: OficioUiState,
    onVentana: (OficioVentana) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val ventanas = listOf(
        OficioVentana.DIA to R.string.home_oficio_dia,
        OficioVentana.SEMANA to R.string.home_oficio_semana,
        OficioVentana.MES to R.string.home_oficio_mes,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(scheme.surfaceContainer, scheme.surfaceContainerLowest)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        scheme.secondary.copy(alpha = 0.28f),
                        scheme.secondary.copy(alpha = 0.06f),
                    )
                ),
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_oficio_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ventanas.forEachIndexed { index, (ventana, label) ->
                    SegmentedButton(
                        selected = state.ventana == ventana,
                        onClick = { onVentana(ventana) },
                        shape = SegmentedButtonDefaults.itemShape(index, ventanas.size),
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            if (!state.conSesion) {
                Text(
                    text = stringResource(R.string.home_oficio_sin_sesion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            } else if (state.error != null) {
                Text(
                    text = stringResource(R.string.home_oficio_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                )
            }
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
            Text(
                text = stringResource(R.string.home_oficio_grafica),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            if (state.serie.isNotEmpty()) {
                key(state.ventana) {
                    OficioHorasChart(puntos = state.serie)
                }
            }
            if (state.conSesion && state.error == null &&
                state.horasSegundos == 0 && state.rondasServidas == 0
            ) {
                Text(
                    text = stringResource(R.string.home_oficio_vacio),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (state.cargando) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter),
                color = scheme.secondary,
            )
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
