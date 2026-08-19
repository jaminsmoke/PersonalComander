package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.GrupoModificador
import com.jaminsmoke.personalcomander.data.OpcionModificador
import com.jaminsmoke.personalcomander.data.Producto
import com.jaminsmoke.personalcomander.data.ProductoGrupo
import com.jaminsmoke.personalcomander.data.sesion.cartaEditable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OpcionBorrador(
    val id: Long = 0,
    val nombre: String = "",
    val deltaPrecio: Double = 0.0,
    val alias: String = "",
)

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PersonalComanderApp
    private val db = app.db
    private val ctx = getApplication<Application>()
    val productos: Flow<List<Producto>> = db.productoDao().observeAllIncluyendoOcultos()
    val grupos: StateFlow<List<GrupoModificador>> = db.grupoModificadorDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val opciones: StateFlow<List<OpcionModificador>> = db.opcionModificadorDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val asignaciones: StateFlow<List<ProductoGrupo>> = db.productoGrupoDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cartaEditable: StateFlow<Boolean> = app.sesion.modo
        .map { it.cartaEditable }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val cargando: StateFlow<Boolean> = db.productoDao().observeAllIncluyendoOcultos()
        .map { false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun limpiarMensaje() { _mensaje.value = null }

    fun gruposDe(productoId: Long): List<Long> =
        asignaciones.value.filter { it.productoId == productoId }.map { it.grupoId }

    fun opcionesDe(grupoId: Long): List<OpcionModificador> =
        opciones.value.filter { it.grupoId == grupoId }

    fun addProducto(
        nombre: String,
        categoria: String,
        precio: Double,
        subfamilia: String?,
        permiteNota: Boolean,
        grupoIds: List<Long>,
    ) {
        if (!cartaEditable.value) {
            _mensaje.value = ctx.getString(R.string.sesion_carta_solo_lectura)
            return
        }
        val n = nombre.trim()
        val c = categoria.trim()
        if (n.isBlank()) { _mensaje.value = ctx.getString(R.string.menu_validation_name_required); return }
        if (c.isBlank()) { _mensaje.value = ctx.getString(R.string.menu_validation_category_required); return }
        if (precio < 0) { _mensaje.value = ctx.getString(R.string.menu_validation_negative_price); return }
        viewModelScope.launch {
            try {
                db.withTransaction {
                    val id = db.productoDao().insert(
                        Producto(
                            nombre = n,
                            categoria = c,
                            precio = precio,
                            disponible = true,
                            subfamilia = subfamilia?.trim()?.takeIf { it.isNotEmpty() },
                            permiteNota = permiteNota,
                        ),
                    )
                    if (grupoIds.isNotEmpty()) {
                        db.productoGrupoDao().insertAll(grupoIds.map { ProductoGrupo(id, it) })
                    }
                }
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_add_product, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun updateProducto(
        producto: Producto,
        nombre: String,
        categoria: String,
        precio: Double,
        subfamilia: String?,
        permiteNota: Boolean,
        grupoIds: List<Long>,
    ) {
        if (!cartaEditable.value) {
            _mensaje.value = ctx.getString(R.string.sesion_carta_solo_lectura)
            return
        }
        val n = nombre.trim()
        val c = categoria.trim()
        if (n.isBlank()) { _mensaje.value = ctx.getString(R.string.menu_validation_name_required); return }
        if (c.isBlank()) { _mensaje.value = ctx.getString(R.string.menu_validation_category_required); return }
        if (precio < 0) { _mensaje.value = ctx.getString(R.string.menu_validation_negative_price); return }
        viewModelScope.launch {
            try {
                db.withTransaction {
                    db.productoDao().update(
                        producto.copy(
                            nombre = n,
                            categoria = c,
                            precio = precio,
                            subfamilia = subfamilia?.trim()?.takeIf { it.isNotEmpty() },
                            permiteNota = permiteNota,
                        ),
                    )
                    db.productoGrupoDao().deleteByProducto(producto.id)
                    if (grupoIds.isNotEmpty()) {
                        db.productoGrupoDao().insertAll(grupoIds.map { ProductoGrupo(producto.id, it) })
                    }
                }
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_update_product, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun toggleDisponible(producto: Producto) {
        if (!cartaEditable.value) {
            _mensaje.value = ctx.getString(R.string.sesion_carta_solo_lectura)
            return
        }
        viewModelScope.launch {
            try {
                db.productoDao().updateDisponible(producto.id, !producto.disponible)
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_toggle_availability, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun deleteProducto(producto: Producto) {
        if (!cartaEditable.value) {
            _mensaje.value = ctx.getString(R.string.sesion_carta_solo_lectura)
            return
        }
        viewModelScope.launch {
            try {
                val activas = db.lineaPedidoDao().countActiveLinesForProduct(producto.id)
                if (activas > 0) {
                    _mensaje.value = ctx.getString(R.string.error_delete_product_active, producto.nombre, activas)
                    return@launch
                }
                db.productoDao().delete(producto.id)
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_delete_product, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun guardarGrupo(
        existente: GrupoModificador?,
        nombre: String,
        multiple: Boolean,
        obligatorio: Boolean,
        opciones: List<OpcionBorrador>,
    ) {
        if (!cartaEditable.value) {
            _mensaje.value = ctx.getString(R.string.sesion_carta_solo_lectura)
            return
        }
        val n = nombre.trim()
        if (n.isBlank()) {
            _mensaje.value = ctx.getString(R.string.menu_validation_group_name)
            return
        }
        val limpias = opciones.map { it.copy(nombre = it.nombre.trim()) }.filter { it.nombre.isNotEmpty() }
        if (limpias.isEmpty()) {
            _mensaje.value = ctx.getString(R.string.menu_validation_group_options)
            return
        }
        viewModelScope.launch {
            try {
                db.withTransaction {
                    val gid = if (existente == null) {
                        db.grupoModificadorDao().insert(
                            GrupoModificador(nombre = n, multiple = multiple, obligatorio = obligatorio),
                        )
                    } else {
                        db.grupoModificadorDao().update(
                            existente.copy(nombre = n, multiple = multiple, obligatorio = obligatorio),
                        )
                        existente.id
                    }
                    db.opcionModificadorDao().deleteByGrupo(gid)
                    db.opcionModificadorDao().insertAll(
                        limpias.map {
                            OpcionModificador(
                                grupoId = gid,
                                nombre = it.nombre,
                                deltaPrecio = it.deltaPrecio,
                                alias = it.alias.trim(),
                            )
                        },
                    )
                }
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_add_product, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun deleteGrupo(grupo: GrupoModificador) {
        if (!cartaEditable.value) {
            _mensaje.value = ctx.getString(R.string.sesion_carta_solo_lectura)
            return
        }
        viewModelScope.launch {
            try {
                db.grupoModificadorDao().delete(grupo.id)
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_delete_product, e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
