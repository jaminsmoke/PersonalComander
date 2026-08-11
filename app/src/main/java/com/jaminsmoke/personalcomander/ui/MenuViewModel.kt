package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.data.Producto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as PersonalComanderApp).db
    val productos: Flow<List<Producto>> = db.productoDao().observeAllIncluyendoOcultos()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun limpiarMensaje() { _mensaje.value = null }

    fun addProducto(nombre: String, categoria: String, precio: Double) {
        viewModelScope.launch {
            try {
                db.productoDao().insert(
                    Producto(nombre = nombre.trim(), categoria = categoria.trim(), precio = precio, disponible = true)
                )
            } catch (e: Exception) {
                _mensaje.value = "Error al añadir producto: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun updateProducto(producto: Producto, nombre: String, categoria: String, precio: Double) {
        viewModelScope.launch {
            try {
                db.productoDao().update(producto.copy(nombre = nombre.trim(), categoria = categoria.trim(), precio = precio))
            } catch (e: Exception) {
                _mensaje.value = "Error al actualizar producto: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun toggleDisponible(producto: Producto) {
        viewModelScope.launch {
            try {
                db.productoDao().updateDisponible(producto.id, !producto.disponible)
            } catch (e: Exception) {
                _mensaje.value = "Error al cambiar disponibilidad: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun deleteProducto(producto: Producto) {
        viewModelScope.launch {
            try {
                db.productoDao().delete(producto.id)
            } catch (e: Exception) {
                _mensaje.value = "Error al eliminar producto: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }
}
