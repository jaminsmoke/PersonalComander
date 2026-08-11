package com.jaminsmoke.personalcomander.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.data.AppDatabase
import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaEstado
import com.jaminsmoke.personalcomander.data.Pedido
import com.jaminsmoke.personalcomander.data.PedidoEstado
import com.jaminsmoke.personalcomander.data.Producto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    val categoria: String? = null
) {
    val total: Double get() = lineas.sumOf { it.precioUnitario * it.cantidad }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ComandaViewModel(
    private val db: AppDatabase,
    private val mesaId: Long
) : ViewModel() {

    private val pedido: StateFlow<Pedido?> = db.pedidoDao().observeActivo(mesaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val lineas = pedido.flatMapLatest { p ->
        if (p == null) flowOf(emptyList())
        else db.lineaPedidoDao().observeForPedido(p.id)
    }

    private val _busqueda = MutableStateFlow("")
    private val _categoria = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ComandaUiState> = run {
        val datos = combine(
            db.mesaDao().observeById(mesaId),
            pedido,
            lineas,
            db.productoDao().observeAll()
        ) { mesa, p, ls, prods ->
            ComandaData(mesa, p, ls, prods)
        }

        combine(datos, _busqueda, _categoria) { d, busqueda, categoria ->
            val cats = d.productos.map { it.categoria }.distinct()
            val filtrados = d.productos.filter { prod ->
                (categoria == null || prod.categoria == categoria) &&
                    prod.nombre.contains(busqueda.trim(), ignoreCase = true)
            }
            ComandaUiState(
                mesa = d.mesa,
                pedido = d.pedido,
                lineas = d.lineas,
                categorias = cats,
                productos = filtrados,
                busqueda = busqueda,
                categoria = categoria
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ComandaUiState())
    }

    private data class ComandaData(
        val mesa: Mesa?,
        val pedido: Pedido?,
        val lineas: List<LineaPedido>,
        val productos: List<Producto>
    )

    fun setBusqueda(valor: String) {
        _busqueda.value = valor
    }

    fun setCategoria(categoria: String?) {
        _categoria.value = categoria
    }

    fun addProducto(producto: Producto) {
        viewModelScope.launch {
            val p = db.pedidoDao().getActivo(mesaId)
            val pedidoActivo = if (p == null) {
                val nuevoId = db.pedidoDao().insert(Pedido(mesaId = mesaId))
                db.mesaDao().updateEstado(mesaId, MesaEstado.OCUPADA, nuevoId)
                Pedido(id = nuevoId, mesaId = mesaId)
            } else {
                p
            }

            val lineas = db.lineaPedidoDao().getForPedido(pedidoActivo.id)
            val existente = lineas.firstOrNull { it.productoId == producto.id }
            if (existente != null) {
                db.lineaPedidoDao().update(existente.copy(cantidad = existente.cantidad + 1))
            } else {
                db.lineaPedidoDao().insert(
                    LineaPedido(
                        pedidoId = pedidoActivo.id,
                        productoId = producto.id,
                        nombreProducto = producto.nombre,
                        precioUnitario = producto.precio,
                        cantidad = 1
                    )
                )
            }
        }
    }

    fun aumentarLinea(linea: LineaPedido) {
        viewModelScope.launch {
            db.lineaPedidoDao().update(linea.copy(cantidad = linea.cantidad + 1))
        }
    }

    fun disminuirLinea(linea: LineaPedido) {
        viewModelScope.launch {
            if (linea.cantidad > 1) {
                db.lineaPedidoDao().update(linea.copy(cantidad = linea.cantidad - 1))
            } else {
                db.lineaPedidoDao().delete(linea)
            }
        }
    }

    fun enviarACocina() {
        viewModelScope.launch {
            val p = db.pedidoDao().getActivo(mesaId) ?: return@launch
            db.pedidoDao().update(p.copy(estado = PedidoEstado.ENVIADA))
            db.mesaDao().updateEstado(mesaId, MesaEstado.EN_COCINA, p.id)
        }
    }

    fun cerrarMesa() {
        viewModelScope.launch {
            val p = db.pedidoDao().getActivo(mesaId) ?: return@launch
            db.pedidoDao().update(
                p.copy(
                    estado = PedidoEstado.CERRADA,
                    cerradoEn = System.currentTimeMillis()
                )
            )
            db.mesaDao().updateEstado(mesaId, MesaEstado.LIBRE, null)
        }
    }

    companion object {
        fun factory(context: Context, mesaId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ComandaViewModel(AppDatabase.get(context), mesaId) as T
                }
            }
    }
}
