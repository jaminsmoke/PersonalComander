package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaForma
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MesasViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as PersonalComanderApp).db
    private val ctx = getApplication<Application>()
    val mesas: Flow<List<Mesa>> = db.mesaDao().observeAll()

    /** True while Room has not emitted the first list yet; false afterwards (even if empty) */
    val cargando: StateFlow<Boolean> = db.mesaDao().observeAll()
        .map { false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

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
                _mensaje.value = ctx.getString(R.string.error_update_table, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun deleteMesa(mesa: Mesa) {
        if (mesa.comandaActivaId != null) {
            _mensaje.value = ctx.getString(R.string.error_delete_table_active)
            return
        }
        viewModelScope.launch {
            try {
                db.mesaDao().deleteById(mesa.id)
                db.mesaDao().renumberAfter(mesa.numero)
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_delete_table, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun updatePosicion(mesa: Mesa, posX: Float, posY: Float) {
        viewModelScope.launch {
            try {
                db.mesaDao().updatePosicion(mesa.id, posX, posY)
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_move_table, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun toggleGiro(mesa: Mesa) {
        viewModelScope.launch {
            try {
                db.mesaDao().updateGiro(mesa.id, !mesa.girada)
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_rotate_table, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun createMesa(zona: String, forma: MesaForma, capacidad: Int, alias: String?) {
        viewModelScope.launch {
            try {
                val maxNum = db.mesaDao().getMaxNumero()
                val a = alias?.trim()?.ifBlank { null }
                // Place new mesa at the bottom-right of existing mesas
                db.mesaDao().insertMesa(
                    Mesa(
                        numero = maxNum + 1, alias = a, forma = forma,
                        zona = zona, capacidad = capacidad,
                        posX = (maxNum % 4) * 140f,
                        posY = (maxNum / 4) * 160f + 50f
                    )
                )
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_create_table, e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
