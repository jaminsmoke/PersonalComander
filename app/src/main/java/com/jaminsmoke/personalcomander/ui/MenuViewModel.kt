package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.data.Producto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as PersonalComanderApp).db
    val productos: Flow<List<Producto>> = db.productoDao().observeAllIncluyendoOcultos()

    fun addProducto(nombre: String, categoria: String, precio: Double) {
        viewModelScope.launch {
            db.productoDao().insert(
                Producto(nombre = nombre.trim(), categoria = categoria.trim(), precio = precio, disponible = true)
            )
        }
    }

    fun updateProducto(producto: Producto, nombre: String, categoria: String, precio: Double) {
        viewModelScope.launch {
            db.productoDao().update(producto.copy(nombre = nombre.trim(), categoria = categoria.trim(), precio = precio))
        }
    }

    fun toggleDisponible(producto: Producto) {
        viewModelScope.launch { db.productoDao().updateDisponible(producto.id, !producto.disponible) }
    }

    fun deleteProducto(producto: Producto) {
        viewModelScope.launch { db.productoDao().delete(producto.id) }
    }
}
