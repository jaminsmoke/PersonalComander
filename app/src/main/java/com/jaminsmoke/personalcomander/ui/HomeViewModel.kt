package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.data.sesion.OficioPunto
import com.jaminsmoke.personalcomander.data.sesion.OficioVentana
import com.jaminsmoke.personalcomander.data.sesion.limites
import com.jaminsmoke.personalcomander.data.sesion.serie
import com.jaminsmoke.personalcomander.data.sesion.token
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class HomeUiState(
    val cargando: Boolean = true,
    val mesasTotales: Int = 0,
    val mesasOcupadas: Int = 0,
    val pedidosActivos: Int = 0,
    val totalHoy: Double = 0.0,
    val error: String? = null,
)

data class OficioUiState(
    val conSesion: Boolean = false,
    val cargando: Boolean = false,
    val ventana: OficioVentana = OficioVentana.DIA,
    val horasSegundos: Int = 0,
    val rondasServidas: Int = 0,
    val serie: List<OficioPunto> = emptyList(),
    val error: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PersonalComanderApp
    private val db = app.db
    private val sesion = app.sesion

    /** Emits a new value at start and after midnight, so totalHoy recalculates daily */
    private val _inicioDelDia = MutableStateFlow(todayStart())
    private val _ventana = MutableStateFlow(OficioVentana.DIA)
    private val _refrescoOficio = MutableStateFlow(0)
    private val _oficio = MutableStateFlow(OficioUiState())
    val oficio: StateFlow<OficioUiState> = _oficio.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val tomorrow = LocalDate.now().plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val msUntilMidnight = tomorrow - now + 60_000 // 1 min past midnight
                delay(msUntilMidnight.coerceAtLeast(60_000))
                _inicioDelDia.value = todayStart()
            }
        }
        viewModelScope.launch {
            combine(sesion.modo, _ventana, _refrescoOficio) { modo, ventana, _ ->
                Pair(modo.token, ventana)
            }.collectLatest { (token, ventana) ->
                cargarOficio(token, ventana)
            }
        }
    }

    fun setVentana(ventana: OficioVentana) {
        if (_ventana.value == ventana) return
        pintarEje(ventana, _oficio.value.conSesion)
        _ventana.value = ventana
    }

    fun refrescarOficio() {
        _refrescoOficio.value = _refrescoOficio.value + 1
    }

    val uiState: StateFlow<HomeUiState> = _inicioDelDia.flatMapLatest { inicio ->
        combine(
            db.mesaDao().observeCount(),
            db.mesaDao().observeOcupadas(),
            db.pedidoDao().observeActivos(),
            db.pedidoDao().observeTotalHoy(inicio)
        ) { totales, ocupadas, abiertos, total ->
            HomeUiState(
                cargando = false,
                mesasTotales = totales,
                mesasOcupadas = ocupadas,
                pedidosActivos = abiertos,
                totalHoy = total
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private suspend fun cargarOficio(token: String?, ventana: OficioVentana) {
        val zona = ZoneId.systemDefault()
        val bounds = ventana.limites(ZonedDateTime.now(zona))
        val eje = ventana.serie(emptyList(), bounds.desde, bounds.hasta, zona)
        if (token == null) {
            _oficio.value = OficioUiState(ventana = ventana, serie = eje)
            return
        }
        _oficio.value = _oficio.value.copy(
            conSesion = true,
            cargando = true,
            ventana = ventana,
            error = null,
            serie = eje,
        )
        val resumen = sesion.resumenOficio(bounds.desde, bounds.hasta)
        if (!resumen.ok || resumen.valor == null) {
            _oficio.value = _oficio.value.copy(
                cargando = false,
                error = resumen.error,
                serie = eje,
            )
            return
        }
        val jornadas = sesion.jornadasOficio(bounds.desde, bounds.hasta).valor.orEmpty()
        _oficio.value = OficioUiState(
            conSesion = true,
            ventana = ventana,
            horasSegundos = resumen.valor.horasSegundos,
            rondasServidas = resumen.valor.rondasServidas,
            serie = ventana.serie(jornadas, bounds.desde, bounds.hasta, zona),
        )
    }

    private fun pintarEje(ventana: OficioVentana, conSesion: Boolean) {
        val zona = ZoneId.systemDefault()
        val bounds = ventana.limites(ZonedDateTime.now(zona))
        _oficio.value = _oficio.value.copy(
            ventana = ventana,
            cargando = conSesion,
            error = null,
            horasSegundos = 0,
            rondasServidas = 0,
            serie = ventana.serie(emptyList(), bounds.desde, bounds.hasta, zona),
        )
    }

    companion object {
        fun todayStart(): Long =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
