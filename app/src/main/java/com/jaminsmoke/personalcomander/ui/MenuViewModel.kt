package com.jaminsmoke.personalcomander.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.data.AppDatabase
import com.jaminsmoke.personalcomander.data.Producto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MenuViewModel(private val db: AppDatabase) : ViewModel() {
    val productos: Flow<List<Producto>> = db.productoDao().observeAllIncluyendoOcultos()

    fun addProducto(nombre: String, categoria: String, precio: Double) {
        viewModelScope.launch {
            db.productoDao().insert(
                Producto(
                    nombre = nombre.trim(),
                    categoria = categoria.trim(),
                    precio = precio,
                    disponible = true
                )
            )
        }
    }

    fun updateProducto(producto: Producto, nombre: String, categoria: String, precio: Double) {
        viewModelScope.launch {
            db.productoDao().update(
                producto.copy(
                    nombre = nombre.trim(),
                    categoria = categoria.trim(),
                    precio = precio
                )
            )
        }
    }

    fun toggleDisponible(producto: Producto) {
        viewModelScope.launch {
            db.productoDao().updateDisponible(producto.id, !producto.disponible)
        }
    }

    fun deleteProducto(producto: Producto) {
        viewModelScope.launch {
            db.productoDao().delete(producto.id)
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MenuViewModel(AppDatabase.get(context)) as T
                }
            }
    }
}
