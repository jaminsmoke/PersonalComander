package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaForma
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

    fun updateConfig(mesa: Mesa, alias: String?, capacidad: Int) {
        viewModelScope.launch {
            try {
                val a = alias?.trim()?.ifBlank { null }
                val cap = capacidad.coerceIn(1, 99)
                db.mesaDao().updateConfig(mesa.id, a, cap)
            } catch (e: Exception) {
                _mensaje.value = "Error al actualizar mesa: ${e.message}"
            }
        }
    }

    fun deleteMesa(mesa: Mesa) {
        viewModelScope.launch {
            try {
                db.mesaDao().deleteById(mesa.id)
                db.mesaDao().renumberAfter(mesa.numero)
            } catch (e: Exception) {
                _mensaje.value = "Error al eliminar mesa: ${e.message}"
            }
        }
    }

    fun createMesa(zona: String, forma: MesaForma, capacidad: Int, alias: String?) {
        viewModelScope.launch {
            try {
                val maxNum = db.mesaDao().getMaxNumero()
                val a = alias?.trim()?.ifBlank { null }
                db.mesaDao().insertMesa(
                    Mesa(numero = maxNum + 1, alias = a, forma = forma, zona = zona, capacidad = capacidad)
                )
            } catch (e: Exception) {
                _mensaje.value = "Error al crear mesa: ${e.message}"
            }
        }
    }
}
