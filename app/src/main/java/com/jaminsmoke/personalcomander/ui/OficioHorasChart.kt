package com.jaminsmoke.personalcomander.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaminsmoke.personalcomander.data.sesion.HorasDiaPunto
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OficioHorasChart(
    puntos: List<HorasDiaPunto>,
    modifier: Modifier = Modifier,
) {
    if (puntos.isEmpty()) return
    val modelProducer = remember { CartesianChartModelProducer() }
    val etiquetas = remember(puntos) {
        val fmt = DateTimeFormatter.ofPattern("d", Locale.getDefault())
        puntos.map { it.fecha.format(fmt) }
    }
    LaunchedEffect(puntos) {
        modelProducer.runTransaction {
            columnSeries {
                series(puntos.map { it.segundos / 3600.0 })
            }
        }
    }
    val bottomFormatter = remember(etiquetas) {
        object : CartesianValueFormatter {
            override fun format(
                context: CartesianMeasuringContext,
                value: Double,
                verticalAxisPosition: Axis.Position.Vertical?,
            ): CharSequence = etiquetas.getOrNull(value.toInt()) ?: ""
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter),
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
    )
}
