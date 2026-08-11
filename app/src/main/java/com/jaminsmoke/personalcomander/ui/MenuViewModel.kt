package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.Producto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as PersonalComanderApp).db
    private val ctx = getApplication<Application>()
    val productos: Flow<List<Producto>> = db.productoDao().observeAllIncluyendoOcultos()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun limpiarMensaje() { _mensaje.value = null }

    fun addProducto(nombre: String, categoria: String, precio: Double) {
        val n = nombre.trim()
        val c = categoria.trim()
        if (n.isBlank()) { _mensaje.value = ctx.getString(R.string.menu_validation_name_required); return }
        if (c.isBlank()) { _mensaje.value = ctx.getString(R.string.menu_validation_category_required); return }
        if (precio < 0) { _mensaje.value = ctx.getString(R.string.menu_validation_negative_price); return }
        viewModelScope.launch {
            try {
                db.productoDao().insert(Producto(nombre = n, categoria = c, precio = precio, disponible = true))
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_add_product, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun updateProducto(producto: Producto, nombre: String, categoria: String, precio: Double) {
        val n = nombre.trim()
        val c = categoria.trim()
        if (n.isBlank()) { _mensaje.value = ctx.getString(R.string.menu_validation_name_required); return }
        if (c.isBlank()) { _mensaje.value = ctx.getString(R.string.menu_validation_category_required); return }
        if (precio < 0) { _mensaje.value = ctx.getString(R.string.menu_validation_negative_price); return }
        viewModelScope.launch {
            try {
                db.productoDao().update(producto.copy(nombre = n, categoria = c, precio = precio))
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_update_product, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun toggleDisponible(producto: Producto) {
        viewModelScope.launch {
            try {
                db.productoDao().updateDisponible(producto.id, !producto.disponible)
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_toggle_availability, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun deleteProducto(producto: Producto) {
        viewModelScope.launch {
            try {
                db.productoDao().delete(producto.id)
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_delete_product, e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
