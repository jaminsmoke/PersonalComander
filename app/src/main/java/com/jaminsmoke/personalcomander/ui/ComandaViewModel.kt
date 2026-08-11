package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.AppDatabase
import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaEstado
import com.jaminsmoke.personalcomander.data.Pedido
import com.jaminsmoke.personalcomander.data.PedidoEstado
import com.jaminsmoke.personalcomander.data.Producto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ComandaUiState(
    val mesa: Mesa? = null,
    val pedido: Pedido? = null,
    val lineas: List<LineaPedido> = emptyList(),
    val categorias: List<String> = emptyList(),
    val productos: List<Producto> = emptyList(),
    val busqueda: String = "",
    val categoria: String? = null,
    val escuchandoVoz: Boolean = false,
    val feedbackVoz: String? = null,
    val error: String? = null
) {
    val total: Double get() = lineas.sumOf { it.precioUnitario * it.cantidad }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ComandaViewModel(
    application: Application,
    private val mesaId: Long
) : AndroidViewModel(application) {

    private val db = (application as PersonalComanderApp).db
    private val ctx = getApplication<Application>()

    private val mutex = Mutex()
    private var cerrada = false

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
    private val _feedbackVoz = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ComandaUiState> = run {
        val datos = combine(
            db.mesaDao().observeById(mesaId),
            pedido,
            lineas,
            db.productoDao().observeAll()
        ) { mesa, p, ls, prods -> ComandaData(mesa, p, ls, prods) }

        // Merge feedbackVoz + error into one snackbar flow (max 5 flows in combine)
        val _snackbar = combine(_feedbackVoz, _error) { f, e -> e ?: f }

        combine(datos, _busqueda, _categoria, _escuchandoVoz, _snackbar) { d, busqueda, categoria, escuchando, snackbar ->
            val cats = d.productos.map { it.categoria }.distinct()
            val porCategoria = d.productos.filter { categoria == null || it.categoria == categoria }
            val filtrados = if (busqueda.isBlank()) porCategoria
            else porCategoria
                .mapNotNull { p -> coincidenciaBusqueda(busqueda, p)?.let { it to p } }
                .sortedWith(compareBy({ it.first }, { it.second.nombre }))
                .map { it.second }
            ComandaUiState(
                mesa = d.mesa, pedido = d.pedido, lineas = d.lineas,
                categorias = cats, productos = filtrados,
                busqueda = busqueda, categoria = categoria,
                escuchandoVoz = escuchando, feedbackVoz = snackbar, error = snackbar
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ComandaUiState())
    }

    private data class ComandaData(
        val mesa: Mesa?, val pedido: Pedido?,
        val lineas: List<LineaPedido>, val productos: List<Producto>
    )

    fun setBusqueda(valor: String) { _busqueda.value = valor }
    fun setCategoria(categoria: String?) { _categoria.value = categoria }
    fun setEscuchandoVoz(valor: Boolean) { _escuchandoVoz.value = valor }

    fun informar(mensaje: String?) {
        _feedbackVoz.value = mensaje
        if (mensaje != null) clearFeedbackVoz()
    }

    fun limpiarError() { _error.value = null }

    fun procesarVoz(texto: String) {
        viewModelScope.launch {
            try {
                val productos = db.productoDao().getAllDisponibles()
                val resultado = parsearComanda(texto, productos)
                resultado.lineas.forEach { linea -> addProducto(linea.producto, linea.cantidad) }
                val resumen = resultado.lineas.joinToString(", ") { "${it.cantidad}× ${it.producto.nombre}" }
                val pendientes = resultado.noEntendido.joinToString(" ")
                _feedbackVoz.value = when {
                    resumen.isEmpty() && pendientes.isNotEmpty() -> ctx.getString(R.string.comanda_voice_unrecognized, texto, pendientes)
                    resumen.isEmpty() -> ctx.getString(R.string.comanda_voice_not_understood, texto)
                    pendientes.isNotEmpty() -> ctx.getString(R.string.comanda_voice_partial, texto, resumen, pendientes)
                    else -> ctx.getString(R.string.comanda_voice_added, texto, resumen)
                }
                clearFeedbackVoz()
            } catch (e: Exception) {
                _error.value = ctx.getString(R.string.error_add_product, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private fun clearFeedbackVoz() {
        viewModelScope.launch { kotlinx.coroutines.delay(FEEDBACK_VOZ_TIMEOUT_MS); _feedbackVoz.value = null }
    }

    fun addProducto(producto: Producto, cantidad: Int = 1) {
        if (cerrada) return
        viewModelScope.launch {
            mutex.withLock {
                try {
                    val p = db.pedidoDao().getActivo(mesaId)
                    val pedidoActivo = if (p == null) {
                        if (cerrada) return@launch
                        val nuevoId = db.pedidoDao().insert(Pedido(mesaId = mesaId, creadoEn = System.currentTimeMillis()))
                        db.mesaDao().updateEstado(mesaId, MesaEstado.OCUPADA, nuevoId, null)
                        Pedido(id = nuevoId, mesaId = mesaId)
                    } else p

                    val lineas = db.lineaPedidoDao().getForPedido(pedidoActivo.id)
                    val existente = lineas.firstOrNull { it.productoId == producto.id }
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

    fun aumentarLinea(linea: LineaPedido) {
        viewModelScope.launch {
            mutex.withLock {
                try { db.lineaPedidoDao().update(linea.copy(cantidad = linea.cantidad + 1)) }
                catch (e: Exception) { _error.value = ctx.getString(R.string.error_increase_quantity, e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    fun disminuirLinea(linea: LineaPedido) {
        viewModelScope.launch {
            mutex.withLock {
                try {
                    if (linea.cantidad > 1) db.lineaPedidoDao().update(linea.copy(cantidad = linea.cantidad - 1))
                    else db.lineaPedidoDao().delete(linea)
                } catch (e: Exception) { _error.value = ctx.getString(R.string.error_decrease_quantity, e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    fun enviarACocina() {
        viewModelScope.launch {
            mutex.withLock {
                try {
                    val p = db.pedidoDao().getActivo(mesaId) ?: return@launch
                    db.pedidoDao().update(p.copy(estado = PedidoEstado.ENVIADA))
                    db.mesaDao().updateEstado(mesaId, MesaEstado.EN_COCINA, p.id, null)
                } catch (e: Exception) { _error.value = ctx.getString(R.string.error_send_to_kitchen, e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    fun cerrarMesa() {
        cerrada = true
        viewModelScope.launch {
            mutex.withLock {
                try {
                    val p = db.pedidoDao().getActivo(mesaId) ?: return@launch
                    db.pedidoDao().update(p.copy(estado = PedidoEstado.CERRADA, cerradoEn = System.currentTimeMillis()))
                    db.mesaDao().updateEstado(mesaId, MesaEstado.LIBRE, null, null)
                } catch (e: Exception) { _error.value = ctx.getString(R.string.error_close_table, e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    class Factory(private val app: Application, private val mesaId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ComandaViewModel(app, mesaId) as T
    }
}
