package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.data.Mesa
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MesasViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as PersonalComanderApp).db
    val mesas: Flow<List<Mesa>> = db.mesaDao().observeAll()

    private val _zona = MutableStateFlow<String?>(null)
    val zona: StateFlow<String?> = _zona.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun setZona(z: String?) { _zona.value = z }
    fun limpiarMensaje() { _mensaje.value = null }

    fun setAlias(mesa: Mesa, alias: String?) {
        viewModelScope.launch {
            try {
                val a = alias?.trim()?.ifBlank { null }
                db.mesaDao().setAlias(mesa.id, a)
            } catch (e: Exception) {
                _mensaje.value = "Error al cambiar alias: ${e.message}"
            }
        }
    }
}
