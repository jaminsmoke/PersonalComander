package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.AppDatabase
import com.jaminsmoke.personalcomander.data.LineaEstado
import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.esEditable
import com.jaminsmoke.personalcomander.data.normalizarNombre
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaEstado
import com.jaminsmoke.personalcomander.data.Pedido
import com.jaminsmoke.personalcomander.data.PedidoEstado
import com.jaminsmoke.personalcomander.data.Producto
import com.jaminsmoke.personalcomander.data.sesion.BarLanCliente
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.data.sesion.RecogerLogica
import com.jaminsmoke.personalcomander.data.sesion.RondaLanMapper
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ComandaUiState(
    val mesa: Mesa? = null,
    val salaNombre: String = "",
    val pedido: Pedido? = null,
    val lineas: List<LineaPedido> = emptyList(),
    val categorias: List<String> = emptyList(),
    val productos: List<Producto> = emptyList(),
    val busqueda: String = "",
    val categoria: String? = null,
    val escuchandoVoz: Boolean = false,
    val procesandoVoz: Boolean = false,
    val feedbackVoz: String? = null,
    val error: String? = null,
    val ligadoAlBar: Boolean = false,
) {
    val total: Double get() = lineas.sumOf { it.precioUnitario * it.cantidad }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ComandaViewModel(
    application: Application,
    private val mesaId: Long
) : AndroidViewModel(application) {

    private val db = (application as PersonalComanderApp).db
    private val sesion = (application as PersonalComanderApp).sesion
    private val ctx = getApplication<Application>()

    private val mutex = Mutex()
    private var cerrada = false

    private val _mesaCerrada = MutableStateFlow(false)
    val mesaCerrada: StateFlow<Boolean> = _mesaCerrada.asStateFlow()

    private val _mostrarConfirmacionCierre = MutableStateFlow(false)
    val mostrarConfirmacionCierre: StateFlow<Boolean> = _mostrarConfirmacionCierre.asStateFlow()

    private val _mostrarUndo = MutableStateFlow(false)
    val mostrarUndo: StateFlow<Boolean> = _mostrarUndo.asStateFlow()

    private val _tiempoRestanteUndo = MutableStateFlow(0)
    val tiempoRestanteUndo: StateFlow<Int> = _tiempoRestanteUndo.asStateFlow()

    private var undoJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val FEEDBACK_VOZ_TIMEOUT_MS = 5000L
    }

    private val pedido: StateFlow<Pedido?> = db.pedidoDao().observeActivo(mesaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val lineas = pedido.flatMapLatest { p ->
        if (p == null) flowOf(emptyList())
        else db.lineaPedidoDao().observeForPedido(p.id)
    }

    private val _busqueda = MutableStateFlow("")
    private val _categoria = MutableStateFlow<String?>(null)
    private val _escuchandoVoz = MutableStateFlow(false)
    private val _procesandoVoz = MutableStateFlow(false)
    private val _feedbackVoz = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ComandaUiState> = run {
        val datos = combine(
            db.mesaDao().observeById(mesaId),
            pedido,
            lineas,
            db.productoDao().observeAll(),
            db.salaDao().observeAll(),
        ) { mesa, p, ls, prods, salas ->
            ComandaData(
                mesa, p, ls, prods,
                salas.find { it.id == mesa?.salaId }?.nombre.orEmpty(),
            )
        }
            .distinctUntilChanged()

        val _snackState = combine(_feedbackVoz, _error, _procesandoVoz) { f, e, p -> SnackState(f, e, p) }

        val sinBar = combine(datos, _busqueda, _categoria, _escuchandoVoz, _snackState) { d, busqueda, categoria, escuchando, ss ->
            val cats = d.productos.map { it.categoria }.distinct()
            val porCategoria = d.productos.filter { categoria == null || it.categoria == categoria }
            val filtrados = if (busqueda.isBlank()) porCategoria
            else porCategoria
                .mapNotNull { p -> coincidenciaBusqueda(busqueda, p)?.let { it to p } }
                .sortedWith(compareBy({ it.first }, { it.second.nombre }))
                .map { it.second }
            ComandaUiState(
                mesa = d.mesa, salaNombre = d.salaNombre, pedido = d.pedido, lineas = d.lineas,
                categorias = cats, productos = filtrados,
                busqueda = busqueda, categoria = categoria,
                escuchandoVoz = escuchando, procesandoVoz = ss.procesando, feedbackVoz = ss.feedbackVoz, error = ss.error
            )
        }
        combine(sinBar, sesion.modo) { state, modo ->
            state.copy(ligadoAlBar = modo is ModoSesion.Establecimiento)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ComandaUiState())
    }

    private data class SnackState(val feedbackVoz: String?, val error: String?, val procesando: Boolean = false)
    private data class ComandaData(
        val mesa: Mesa?, val pedido: Pedido?,
        val lineas: List<LineaPedido>, val productos: List<Producto>,
        val salaNombre: String,
    )

    fun setBusqueda(valor: String) { _busqueda.value = valor }
    fun setCategoria(categoria: String?) { _categoria.value = categoria }
    fun setEscuchandoVoz(valor: Boolean) { _escuchandoVoz.value = valor }

    fun informar(mensaje: String?) {
        _feedbackVoz.value = mensaje
        if (mensaje != null) clearFeedbackVoz()
    }

    fun limpiarError() { _error.value = null }

    fun limpiarFeedback() {
        feedbackJob?.cancel()
        _feedbackVoz.value = null
    }

    fun procesarVoz(texto: String, vozCercana: Boolean = true) {
        viewModelScope.launch {
            _procesandoVoz.value = true
            try {
                if (!vozCercana) {
                    _feedbackVoz.value = ctx.getString(R.string.comanda_voice_far, texto)
                    clearFeedbackVoz()
                    _procesandoVoz.value = false
                    return@launch
                }

                val accion = extraerAccion(texto)

                when (accion) {
                    is AccionVoz.Anadir -> procesarAnadir(accion.texto, texto)
                    is AccionVoz.Quitar -> procesarQuitar(accion.texto, texto)
                }
            } catch (e: Exception) {
                _error.value = ctx.getString(R.string.error_add_product, e.message ?: e.javaClass.simpleName)
            } finally {
                _procesandoVoz.value = false
            }
        }
    }

    private suspend fun procesarAnadir(comanda: String, textoOriginal: String) {
        val productos = db.productoDao().getAllDisponibles()
        val resultado = parsearComanda(comanda, productos)
        if (resultado.lineas.isNotEmpty()) {
            addProductosBatch(resultado.lineas)
        }
        val resumen = resultado.lineas.joinToString(", ") { "${it.cantidad}× ${it.producto.nombre}" }
        val pendientes = resultado.noEntendido.joinToString(" ")
        _feedbackVoz.value = when {
            resumen.isEmpty() && pendientes.isNotEmpty() -> ctx.getString(R.string.comanda_voice_unrecognized, textoOriginal, pendientes)
            resumen.isEmpty() -> ctx.getString(R.string.comanda_voice_not_understood, textoOriginal)
            pendientes.isNotEmpty() -> ctx.getString(R.string.comanda_voice_partial, textoOriginal, resumen, pendientes)
            else -> ctx.getString(R.string.comanda_voice_added, textoOriginal, resumen)
        }
        clearFeedbackVoz()
    }

    private suspend fun procesarQuitar(comanda: String, textoOriginal: String) {
        val p = db.pedidoDao().getActivo(mesaId) ?: run {
            _feedbackVoz.value = ctx.getString(R.string.comanda_voice_no_order)
            clearFeedbackVoz()
            return
        }
        val lineas = db.lineaPedidoDao().getForPedido(p.id)
        if (lineas.isEmpty()) {
            _feedbackVoz.value = ctx.getString(R.string.comanda_voice_no_order)
            clearFeedbackVoz()
            return
        }
        val pendientes = lineas.filter { it.estado == LineaEstado.PENDIENTE }
        if (pendientes.isEmpty()) {
            _feedbackVoz.value = ctx.getString(R.string.comanda_voice_sent_cant_remove)
            clearFeedbackVoz()
            return
        }

        val primeraPalabra = comanda.split(" ").first()
        if (primeraPalabra == "todo" || primeraPalabra == "todos" || primeraPalabra == "todas") {
            removeAllLineas(pendientes)
            _feedbackVoz.value = ctx.getString(R.string.comanda_voice_removed_all)
            clearFeedbackVoz()
            return
        }

        val resultado = parsearQuitar(comanda, pendientes)
        if (resultado.lineas.isNotEmpty()) {
            removeLineasBatch(resultado.lineas, pendientes)
        }
        val resumen = resultado.lineas.joinToString(", ") { "${it.cantidad}× ${it.nombreProducto}" }
        val noEntendido = resultado.noEntendido.joinToString(" ")
        _feedbackVoz.value = when {
            resumen.isEmpty() && noEntendido.isNotEmpty() -> ctx.getString(R.string.comanda_voice_remove_unrecognized, textoOriginal, noEntendido)
            resumen.isEmpty() -> ctx.getString(R.string.comanda_voice_not_understood, textoOriginal)
            noEntendido.isNotEmpty() -> ctx.getString(R.string.comanda_voice_remove_partial, textoOriginal, resumen, noEntendido)
            else -> ctx.getString(R.string.comanda_voice_removed, textoOriginal, resumen)
        }
        clearFeedbackVoz()
    }

    private suspend fun marcarOcupada(mesaId: Long, comandaId: Long) {
        convertirReservaSiActiva(mesaId)
        db.mesaDao().updateEstado(mesaId, MesaEstado.OCUPADA, comandaId)
    }

    private suspend fun convertirReservaSiActiva(mesaId: Long) {
        val mesa = db.mesaDao().getById(mesaId) ?: return
        val rid = mesa.reservaActivaId ?: return
        db.reservaDao().marcarConvertida(rid, System.currentTimeMillis())
        db.mesaDao().setReservaActiva(mesaId, null)
    }

    private suspend fun addProductosBatch(lineasVoz: List<LineaVoz>) {
        mutex.withLock {
            db.withTransaction {
                if (cerrada) return@withTransaction
                var p = db.pedidoDao().getActivo(mesaId)
                if (p == null) {
                    val nuevoId = db.pedidoDao().insert(Pedido(mesaId = mesaId, creadoEn = System.currentTimeMillis()))
                    marcarOcupada(mesaId, nuevoId)
                    p = Pedido(id = nuevoId, mesaId = mesaId)
                } else if (p.estado == PedidoEstado.ENVIADA) {
                    db.pedidoDao().update(p.copy(estado = PedidoEstado.ABIERTA))
                    marcarOcupada(mesaId, p.id)
                }
                val lineas = db.lineaPedidoDao().getForPedido(p.id)
                for (lv in lineasVoz) {
                    val existente = RecogerLogica.lineaPendienteDelProducto(lineas, lv.producto.id)
                    if (existente != null) {
                        db.lineaPedidoDao().update(existente.copy(cantidad = existente.cantidad + lv.cantidad))
                    } else {
                        db.lineaPedidoDao().insert(LineaPedido(
                            pedidoId = p.id, productoId = lv.producto.id,
                            nombreProducto = lv.producto.nombre, precioUnitario = lv.producto.precio, cantidad = lv.cantidad
                        ))
                    }
                }
            }
        }
    }

    private suspend fun removeLineasBatch(quitadas: List<LineaQuitar>, lineas: List<LineaPedido>) {
        val cambios = resolverQuitar(quitadas, lineas)
        mutex.withLock {
            db.withTransaction {
                for ((linea, nuevaCantidad) in cambios) {
                    if (nuevaCantidad != null) {
                        db.lineaPedidoDao().update(linea.copy(cantidad = nuevaCantidad))
                    } else {
                        db.lineaPedidoDao().delete(linea)
                    }
                }
            }
        }
    }

    private suspend fun removeAllLineas(lineas: List<LineaPedido>) {
        mutex.withLock {
            db.withTransaction {
                for (l in lineas) db.lineaPedidoDao().delete(l)
            }
        }
    }

    private var feedbackJob: kotlinx.coroutines.Job? = null

    private fun clearFeedbackVoz() {
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            kotlinx.coroutines.delay(FEEDBACK_VOZ_TIMEOUT_MS)
            _feedbackVoz.value = null
        }
    }

    fun addProducto(producto: Producto, cantidad: Int = 1) {
        if (cerrada) return
        viewModelScope.launch {
            mutex.withLock {
                db.withTransaction {
                    try {
                        val p = db.pedidoDao().getActivo(mesaId)
                        val pedidoActivo = if (p == null) {
                            if (cerrada) return@withTransaction
                            val nuevoId = db.pedidoDao().insert(Pedido(mesaId = mesaId, creadoEn = System.currentTimeMillis()))
                            marcarOcupada(mesaId, nuevoId)
                            Pedido(id = nuevoId, mesaId = mesaId)
                        } else {
                            if (p.estado == PedidoEstado.ENVIADA) {
                                db.pedidoDao().update(p.copy(estado = PedidoEstado.ABIERTA))
                                marcarOcupada(mesaId, p.id)
                            }
                            p
                        }

                        val lineas = db.lineaPedidoDao().getForPedido(pedidoActivo.id)
                        val existente = RecogerLogica.lineaPendienteDelProducto(lineas, producto.id)
                        if (existente != null) {
                            db.lineaPedidoDao().update(existente.copy(cantidad = existente.cantidad + cantidad))
                        } else {
                            db.lineaPedidoDao().insert(LineaPedido(
                                pedidoId = pedidoActivo.id, productoId = producto.id,
                                nombreProducto = producto.nombre, precioUnitario = producto.precio, cantidad = cantidad
                            ))
                        }
                    } catch (e: Exception) {
                        _error.value = ctx.getString(R.string.error_add_product, e.message ?: e.javaClass.simpleName)
                    }
                }
            }
        }
    }

    fun aumentarLinea(linea: LineaPedido) {
        if (!linea.estado.esEditable()) return
        viewModelScope.launch {
            mutex.withLock {
                try { db.lineaPedidoDao().update(linea.copy(cantidad = linea.cantidad + 1)) }
                catch (e: Exception) { _error.value = ctx.getString(R.string.error_increase_quantity, e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    fun disminuirLinea(linea: LineaPedido) {
        if (!linea.estado.esEditable()) return
        viewModelScope.launch {
            mutex.withLock {
                try {
                    if (linea.cantidad > 1) db.lineaPedidoDao().update(linea.copy(cantidad = linea.cantidad - 1))
                    else db.lineaPedidoDao().delete(linea)
                } catch (e: Exception) { _error.value = ctx.getString(R.string.error_decrease_quantity, e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    fun marcarServida(linea: LineaPedido) {
        val ligado = sesion.modo.value is ModoSesion.Establecimiento
        if (!RecogerLogica.puedeMarcarServida(linea, ligado)) return
        viewModelScope.launch {
            mutex.withLock {
                try { db.lineaPedidoDao().update(linea.copy(estado = LineaEstado.SERVIDA)) }
                catch (e: Exception) { _error.value = ctx.getString(R.string.error_add_product, e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    fun marcarTodasListasServidas() {
        viewModelScope.launch {
            mutex.withLock {
                val p = db.pedidoDao().getActivo(mesaId) ?: return@withLock
                val ligado = sesion.modo.value is ModoSesion.Establecimiento
                val lineas = db.lineaPedidoDao().getForPedido(p.id)
                for (linea in lineas) {
                    if (RecogerLogica.puedeMarcarServida(linea, ligado)) {
                        db.lineaPedidoDao().update(linea.copy(estado = LineaEstado.SERVIDA))
                    }
                }
            }
        }
    }

    fun enviarACocina() {
        viewModelScope.launch {
            val modoActual = sesion.modo.value
            if (modoActual is ModoSesion.Establecimiento && !modoActual.sesionTrabajo) {
                _error.value = ctx.getString(R.string.sesion_ronda_sin_jornada)
                return@launch
            }
            val envio = mutex.withLock {
                try {
                    db.withTransaction {
                        val p = db.pedidoDao().getActivo(mesaId) ?: return@withTransaction null
                        val mesa = db.mesaDao().getById(mesaId) ?: return@withTransaction null
                        val nombreSala = db.salaDao().getById(mesa.salaId)?.nombre.orEmpty()
                        val lineas = db.lineaPedidoDao().getForPedido(p.id)
                        val pendientes = RecogerLogica.lineasAEnviar(lineas)
                        if (pendientes.isEmpty()) return@withTransaction null
                        db.pedidoDao().update(p.copy(estado = PedidoEstado.ENVIADA))
                        db.mesaDao().updateEstado(mesaId, MesaEstado.EN_COCINA, p.id)
                        EnvioLocal(p.id, mesa, nombreSala, pendientes)
                    }
                } catch (e: Exception) {
                    _error.value = ctx.getString(R.string.error_send_to_kitchen, e.message ?: e.javaClass.simpleName)
                    null
                }
            } ?: return@launch
            enviarRondaSiEstablecimiento(envio)
        }
    }

    private suspend fun enviarRondaSiEstablecimiento(envio: EnvioLocal) {
        val modo = sesion.modo.value
        val actualizadas = if (modo is ModoSesion.Establecimiento) {
            val codigoBar = db.productoDao().getAllIncluyendoOcultos()
                .mapNotNull { p -> p.codigoBar?.takeIf { it.isNotBlank() }?.let { p.id to it } }
                .toMap()
            val ticketsLan = if (envio.lineas.isNotEmpty()) {
                val ronda = RondaLanMapper.desdePedido(
                    pedidoId = envio.pedidoId,
                    mesa = envio.mesa,
                    nombreSala = envio.nombreSala,
                    lineas = envio.lineas,
                    camarero = modo.perfil.nombreCompleto,
                    creadoEn = System.currentTimeMillis(),
                    codigoBarPorProductoId = codigoBar,
                )
                val resultado = withContext(Dispatchers.IO) {
                    BarLanCliente.postRonda(modo.barHost, modo.barPuerto, ronda)
                }
                if (!resultado.ok) {
                    if (resultado.codigo == 403) {
                        sesion.marcarJornadaCortada()
                        _error.value = ctx.getString(R.string.sesion_ronda_sin_jornada)
                    } else {
                        _error.value = ctx.getString(R.string.error_send_ronda_bar)
                    }
                }
                resultado.tickets
            } else {
                emptyList()
            }
            RecogerLogica.asignarTickets(envio.lineas, ticketsLan, codigoBar)
        } else {
            envio.lineas.map { it.copy(estado = LineaEstado.LISTA) }
        }
        mutex.withLock {
            for (linea in actualizadas) {
                db.lineaPedidoDao().update(linea)
            }
        }
    }

    private data class EnvioLocal(
        val pedidoId: Long,
        val mesa: Mesa,
        val nombreSala: String,
        val lineas: List<LineaPedido>,
    )

    fun solicitarCierre() {
        _mostrarConfirmacionCierre.value = true
    }

    fun cancelarCierre() {
        _mostrarConfirmacionCierre.value = false
    }

    fun confirmarCierre() {
        _mostrarConfirmacionCierre.value = false
        cerrarMesa()
    }

    fun reabrirMesa() {
        viewModelScope.launch {
            mutex.withLock {
                db.withTransaction {
                    try {
                        val p = db.pedidoDao().getLastCerrado(mesaId) ?: return@withTransaction
                        db.pedidoDao().update(p.copy(estado = PedidoEstado.ABIERTA, cerradoEn = null))
                        marcarOcupada(mesaId, p.id)
                        cerrada = false
                        _mostrarUndo.value = false
                        undoJob?.cancel()
                    } catch (e: Exception) {
                        _error.value = ctx.getString(R.string.error_close_table, e.message ?: e.javaClass.simpleName)
                    }
                }
            }
        }
    }

    private fun cerrarMesa() {
        viewModelScope.launch {
            mutex.withLock {
                db.withTransaction {
                    try {
                        val p = db.pedidoDao().getActivo(mesaId) ?: return@withTransaction
                        db.pedidoDao().update(p.copy(estado = PedidoEstado.CERRADA, cerradoEn = System.currentTimeMillis()))
                        db.mesaDao().updateEstado(mesaId, MesaEstado.LIBRE, null)
                        cerrada = true
                        // No navegamos automáticamente — mostramos undo en su lugar
                        _mostrarUndo.value = true
                        _tiempoRestanteUndo.value = 300 // 5 minutos
                        // Iniciar countdown
                        undoJob?.cancel()
                        undoJob = viewModelScope.launch {
                            for (i in 300 downTo 0) {
                                _tiempoRestanteUndo.value = i
                                kotlinx.coroutines.delay(1000L)
                            }
                            // Timer expiró — navegamos atrás
                            _mostrarUndo.value = false
                            _mesaCerrada.value = true
                        }
                    } catch (e: Exception) {
                        _error.value = ctx.getString(R.string.error_close_table, e.message ?: e.javaClass.simpleName)
                    }
                }
            }
        }
    }

    class Factory(private val app: Application, private val mesaId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ComandaViewModel(app, mesaId) as T
    }
}

/**
 * Resuelve qué líneas modificar/eliminar dado un comando de quitar.
 * Pura, sin dependencias de BD — testeable.
 * @return lista de pares (linea original, nueva cantidad o null si se elimina)
 */
fun resolverQuitar(
    quitadas: List<LineaQuitar>,
    lineas: List<LineaPedido>
): List<Pair<LineaPedido, Int?>> = buildList {
    val resto = lineas.toMutableList()
    for (lq in quitadas) {
        val idx = resto.indexOfFirst {
            normalizarNombre(it.nombreProducto) == normalizarNombre(lq.nombreProducto)
        }
        if (idx == -1) continue
        val linea = resto[idx]
        if (linea.cantidad > lq.cantidad) {
            add(linea to (linea.cantidad - lq.cantidad))
            resto[idx] = linea.copy(cantidad = linea.cantidad - lq.cantidad)
        } else {
            add(linea to null)
            resto.removeAt(idx)
        }
    }
}
