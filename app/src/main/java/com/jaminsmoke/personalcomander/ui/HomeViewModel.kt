package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.ServidorDescubierto
import com.jaminsmoke.personalcomander.data.sesion.IdentityJson
import com.jaminsmoke.personalcomander.data.sesion.LanLocalAspecto
import com.jaminsmoke.personalcomander.data.sesion.LanLocalUi
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.data.sesion.OficioPunto
import com.jaminsmoke.personalcomander.data.sesion.OficioVentana
import com.jaminsmoke.personalcomander.data.sesion.PresenciaLan
import com.jaminsmoke.personalcomander.data.sesion.PresenciaLanOyente
import com.jaminsmoke.personalcomander.data.sesion.etiquetaLocal
import com.jaminsmoke.personalcomander.data.sesion.limites
import com.jaminsmoke.personalcomander.data.sesion.qr
import com.jaminsmoke.personalcomander.data.sesion.serie
import com.jaminsmoke.personalcomander.data.sesion.token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

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

data class LanRadarUiState(
    val conSesion: Boolean = false,
    val escaneando: Boolean = false,
    val ocupado: Boolean = false,
    val locales: List<LanLocalUi> = emptyList(),
    val mensaje: String? = null,
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

    private val _lan = MutableStateFlow(LanRadarUiState())
    val lan: StateFlow<LanRadarUiState> = _lan.asStateFlow()
    private var radarJob: Job? = null
    private val extrasBeacon = ConcurrentHashMap<String, Pair<ServidorDescubierto, Long>>()
    private val wakeRadar = Channel<Unit>(Channel.CONFLATED)
    private var scanPendiente = true

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

    fun iniciarRadar() {
        if (radarJob?.isActive == true) return
        scanPendiente = true
        extrasBeacon.clear()
        sesion.limpiarScanLan()
        radarJob = viewModelScope.launch {
            launch(Dispatchers.IO) {
                PresenciaLanOyente.escuchar(getApplication()) { host, anuncio ->
                    val clave = "$host:${anuncio.puertoHttp}"
                    if (anuncio.activo) {
                        extrasBeacon[clave] =
                            ServidorDescubierto(host, anuncio.puertoHttp) to System.currentTimeMillis()
                    } else {
                        extrasBeacon.remove(clave)
                    }
                    wakeRadar.trySend(Unit)
                }
            }
            while (isActive) {
                refrescarRadar(escanearSubred = scanPendiente)
                withTimeoutOrNull(PresenciaLan.HEARTBEAT_MS) {
                    wakeRadar.receive()
                }
            }
        }
    }

    fun pararRadar() {
        radarJob?.cancel()
        radarJob = null
        extrasBeacon.clear()
        sesion.limpiarScanLan()
    }

    override fun onCleared() {
        pararRadar()
        super.onCleared()
    }

    fun sondearLan() {
        viewModelScope.launch { refrescarRadar(escanearSubred = true) }
    }

    private fun extrasVivos(): List<ServidorDescubierto> {
        val ahora = System.currentTimeMillis()
        extrasBeacon.entries.removeIf { ahora - it.value.second > PresenciaLan.TTL_MS }
        return extrasBeacon.values.map { it.first }
    }

    private suspend fun refrescarRadar(escanearSubred: Boolean) {
        val modo = sesion.modo.value
        val conSesion = modo !is ModoSesion.Local && modo.qr != null
        if (!conSesion) {
            _lan.value = LanRadarUiState(conSesion = false)
            scanPendiente = true
            return
        }
        if (escanearSubred) {
            _lan.value = _lan.value.copy(conSesion = true, escaneando = true)
        } else {
            _lan.value = _lan.value.copy(conSesion = true)
        }
        val locales = sesion.sondearLan(extras = extrasVivos(), escanearSubred = escanearSubred)
        if (escanearSubred) scanPendiente = false
        _lan.value = _lan.value.copy(escaneando = false, conSesion = true, locales = locales)
    }

    fun limpiarMensajeLan() {
        _lan.value = _lan.value.copy(mensaje = null)
    }

    fun alPulsarLocal(item: LanLocalUi) {
        if (_lan.value.ocupado || _lan.value.escaneando) return
        when (item.aspecto) {
            LanLocalAspecto.APAGADO ->
                _lan.value = _lan.value.copy(mensaje = getApplication<Application>().getString(R.string.home_lan_apagado))
            LanLocalAspecto.ROJO -> sondearLan()
            LanLocalAspecto.AMARILLO -> pedirJornada(item)
            LanLocalAspecto.VERDE -> cortarJornada()
        }
    }

    private fun pedirJornada(item: LanLocalUi) {
        viewModelScope.launch {
            _lan.value = _lan.value.copy(ocupado = true, mensaje = null)
            val r = sesion.pedirJornada(item.host, item.puerto)
            val ctx = getApplication<Application>()
            val vigente = sesion.modo.value as? ModoSesion.Establecimiento
            val nombre = vigente?.etiquetaLocal()?.ifBlank { null }
                ?: item.nombre.ifBlank { ctx.getString(R.string.home_lan_local_sin_nombre) }
            val mensaje = when {
                !r.ok -> ctx.getString(R.string.sesion_jornada_rechazada)
                r.nodoViejo -> ctx.getString(R.string.sesion_jornada_nodo_viejo)
                r.sesionActiva -> {
                    val libro = IdentityJson.establecimientoIdPorHealth(
                        vigente?.nombreEstablecimiento,
                        sesion.membresias.value,
                        vigente?.establecimientoId,
                    )
                    if (libro == null) {
                        ctx.getString(R.string.sesion_jornada_sin_libro)
                    } else {
                        ctx.getString(R.string.sesion_jornada_iniciada, nombre)
                    }
                }
                else -> ctx.getString(R.string.sesion_jornada_pendiente_bar)
            }
            _lan.value = _lan.value.copy(ocupado = false, mensaje = mensaje)
            refrescarOficio()
            sondearLan()
        }
    }

    private fun cortarJornada() {
        viewModelScope.launch {
            _lan.value = _lan.value.copy(ocupado = true, mensaje = null)
            sesion.cortarJornada()
            _lan.value = _lan.value.copy(
                ocupado = false,
                mensaje = getApplication<Application>().getString(R.string.sesion_jornada_terminada),
            )
            refrescarOficio()
            sondearLan()
        }
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
