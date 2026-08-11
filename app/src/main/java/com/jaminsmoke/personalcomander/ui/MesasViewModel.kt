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

    fun updateConfig(mesa: Mesa, alias: String?, capacidad: Int, forma: MesaForma = mesa.forma) {
        viewModelScope.launch {
            try {
                val a = alias?.trim()?.ifBlank { null }
                val cap = capacidad.coerceIn(1, 99)
                db.mesaDao().updateConfig(mesa.id, a, cap, forma)
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

    fun swapMesas(mesa1: Mesa, mesa2: Mesa) {
        if (mesa1.id == mesa2.id) return
        viewModelScope.launch {
            try {
                db.mesaDao().swapNumeros(mesa1.numero, mesa2.numero)
            } catch (e: Exception) {
                _mensaje.value = "Error al reordenar: ${e.message}"
            }
        }
    }

    fun updatePosicion(mesa: Mesa, posX: Float, posY: Float) {
        viewModelScope.launch {
            try {
                db.mesaDao().updatePosicion(mesa.id, posX, posY)
            } catch (e: Exception) {
                _mensaje.value = "Error al mover mesa: ${e.message}"
            }
        }
    }

    fun toggleGiro(mesa: Mesa) {
        viewModelScope.launch {
            try {
                db.mesaDao().updateGiro(mesa.id, !mesa.girada)
            } catch (e: Exception) {
                _mensaje.value = "Error al girar mesa: ${e.message}"
            }
        }
    }

    fun createMesa(zona: String, forma: MesaForma, capacidad: Int, alias: String?) {
        viewModelScope.launch {
            try {
                val maxNum = db.mesaDao().getMaxNumero()
                val a = alias?.trim()?.ifBlank { null }
                // Place new mesa at the bottom-right of existing mesas
                val mesas = db.mesaDao().observeAll()
                // Use a simple default position
                db.mesaDao().insertMesa(
                    Mesa(
                        numero = maxNum + 1, alias = a, forma = forma,
                        zona = zona, capacidad = capacidad,
                        posX = (maxNum % 4) * 140f,
                        posY = (maxNum / 4) * 160f + 50f
                    )
                )
            } catch (e: Exception) {
                _mensaje.value = "Error al crear mesa: ${e.message}"
            }
        }
    }
}
