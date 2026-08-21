package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.R
import androidx.room.withTransaction
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaEstado
import com.jaminsmoke.personalcomander.data.MesaForma
import com.jaminsmoke.personalcomander.data.Reserva
import com.jaminsmoke.personalcomander.data.Sala
import com.jaminsmoke.personalcomander.data.ZonaTerritorio
import com.jaminsmoke.personalcomander.data.sesion.mapaEditable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class MesasViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PersonalComanderApp
    private val db = app.db
    private val ctx = getApplication<Application>()
    val mesas: Flow<List<Mesa>> = db.mesaDao().observeAll()
    val salas: Flow<List<Sala>> = db.salaDao().observeAll()
    val zonas: Flow<List<ZonaTerritorio>> = db.zonaTerritorioDao().observeAll()

    val mapaEditable: StateFlow<Boolean> = app.sesion.modo
        .map { it.mapaEditable }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** True while Room has not emitted the first list yet; false afterwards (even if empty) */
    val cargando: StateFlow<Boolean> = db.mesaDao().observeAll()
        .map { false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _salaId = MutableStateFlow<Long?>(null)
    val salaId: StateFlow<Long?> = _salaId.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    init {
        // Normalizar posiciones de mesas legacy una sola vez por zona.
        // distinctUntilChanged por zona evita re-ejecuciones al añadir/mover mesas.
        viewModelScope.launch {
            combine(mesas, salaId) { mesas, salaActual -> mesas to salaActual }
                .distinctUntilChanged { old, new -> old.second == new.second }
                .collect { (todasLasMesas, salaActual) ->
                    if (salaActual != null) {
                        val mesasSala = todasLasMesas.filter { it.salaId == salaActual }
                        if (mesasSala.isNotEmpty()) {
                            normalizarPosiciones(mesasSala)
                        }
                    }
                }
        }
    }

    fun setSala(id: Long?) { _salaId.value = id }
    fun limpiarMensaje() { _mensaje.value = null }

    private fun exigirMapaEditable(): Boolean {
        if (mapaEditable.value) return true
        _mensaje.value = ctx.getString(R.string.sesion_mapa_solo_lectura)
        return false
    }

    fun updateConfig(mesa: Mesa, alias: String?, capacidad: Int, forma: MesaForma = mesa.forma) {
        if (!exigirMapaEditable()) return
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
        if (!exigirMapaEditable()) return
        if (mesa.comandaActivaId != null) {
            _mensaje.value = ctx.getString(R.string.error_delete_table_active)
            return
        }
        viewModelScope.launch {
            try {
                db.withTransaction {
                    db.mesaDao().deleteById(mesa.id)
                    db.mesaDao().renumberAfter(mesa.numero)
                    // Correr índices posteriores de la misma zona (B3→B2) para mantener coherencia
                    if (mesa.indiceZona > 0) {
                        db.mesaDao().decrementarIndicesSala(mesa.salaId, mesa.indiceZona)
                    }
                }
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_delete_table, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun updatePosicion(mesa: Mesa, posX: Float, posY: Float) {
        if (!exigirMapaEditable()) return
        viewModelScope.launch {
            try {
                db.mesaDao().updatePosicion(mesa.id, posX, posY)
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_move_table, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun toggleGiro(mesa: Mesa) {
        if (!exigirMapaEditable()) return
        viewModelScope.launch {
            try {
                db.withTransaction {
                    val nuevoGiro = !mesa.girada
                    val (w, h) = mesaDims(mesa.forma, nuevoGiro)
                    val ocupadas = db.mesaDao().getPorSala(mesa.salaId)
                        .filter { it.id != mesa.id }
                        .map {
                            val (ow, oh) = mesaDims(it.forma, it.girada)
                            listOf(it.posX, it.posY, ow, oh)
                        }
                    val (x, y) = findNearestFreeCell(mesa.posX, mesa.posY, w, h, ocupadas)
                    db.mesaDao().updateGiro(mesa.id, nuevoGiro)
                    if (x != mesa.posX || y != mesa.posY) {
                        db.mesaDao().updatePosicion(mesa.id, x, y)
                    }
                }
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_rotate_table, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /** Corrige una sola vez posiciones antiguas que no respetaban el grid fijo. */
    fun normalizarPosiciones(mesasZona: List<Mesa>) {
        if (!mapaEditable.value) return
        val normalizadas = normalizarMesasEnGrid(mesasZona)
        val cambios = mesasZona.filter { mesa ->
            val destino = normalizadas[mesa.id]
            destino != null && (abs(destino.x - mesa.posX) >= 0.01f || abs(destino.y - mesa.posY) >= 0.01f)
        }
        if (cambios.isEmpty()) return

        viewModelScope.launch {
            try {
                db.withTransaction {
                    cambios.forEach { mesa ->
                        val destino = normalizadas.getValue(mesa.id)
                        db.mesaDao().updatePosicion(mesa.id, destino.x, destino.y)
                    }
                }
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_move_table, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun createMesa(salaId: Long, forma: MesaForma, capacidad: Int, alias: String?) {
        if (!exigirMapaEditable()) return
        viewModelScope.launch {
            try {
                db.withTransaction {
                    val maxNum = db.mesaDao().getMaxNumero()
                    val a = alias?.trim()?.ifBlank { null }
                    val siguienteIndice = db.mesaDao().getMaxIndiceSala(salaId) + 1
                    val mesasSala = db.mesaDao().getPorSala(salaId)
                    val candidata = mesasSala.maxByOrNull { it.posX }
                        ?.let {
                            val (lastW, _) = mesaDims(it.forma, it.girada)
                            it.posX + lastW + CELL_F to it.posY
                        }
                        ?: ((maxNum % 4) * 160f to (maxNum / 4) * 160f + CELL_F)
                    val ocupadas = mesasSala.map {
                        val (ow, oh) = mesaDims(it.forma, it.girada)
                        listOf(it.posX, it.posY, ow, oh)
                    }
                    val (newW, newH) = mesaDims(forma, girada = false)
                    val (px, py) = findNearestFreeCell(
                        candidata.first, candidata.second, newW, newH, ocupadas
                    )
                    db.mesaDao().insertMesa(
                        Mesa(
                            numero = maxNum + 1, alias = a, forma = forma,
                            salaId = salaId, capacidad = capacidad, indiceZona = siguienteIndice,
                            posX = px, posY = py
                        )
                    )
                }
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_create_table, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun avisarMesaBloqueada() {
        _mensaje.value = ctx.getString(R.string.error_table_blocked)
    }

    fun reservar(mesa: Mesa, nombre: String, paraEpoch: Long? = null) {
        if (!exigirMapaEditable()) return
        val n = nombre.trim()
        if (n.isEmpty()) {
            _mensaje.value = ctx.getString(R.string.error_reserve_name)
            return
        }
        if (mesa.comandaActivaId != null || mesa.estado != MesaEstado.LIBRE) {
            _mensaje.value = ctx.getString(R.string.error_reserve_busy)
            return
        }
        if (mesa.bloqueada) {
            _mensaje.value = ctx.getString(R.string.error_reserve_blocked)
            return
        }
        if (mesa.reservaActivaId != null) {
            _mensaje.value = ctx.getString(R.string.error_reserve_already)
            return
        }
        viewModelScope.launch {
            try {
                db.withTransaction {
                    val id = db.reservaDao().insert(
                        Reserva(
                            mesaId = mesa.id,
                            nombre = n,
                            paraEpoch = paraEpoch,
                            creadaEn = System.currentTimeMillis()
                        )
                    )
                    db.mesaDao().setReservaActiva(mesa.id, id)
                }
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_reserve, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun cancelarReserva(mesa: Mesa) {
        if (!exigirMapaEditable()) return
        val rid = mesa.reservaActivaId
        if (rid == null) {
            _mensaje.value = ctx.getString(R.string.error_reserve_none)
            return
        }
        viewModelScope.launch {
            try {
                db.withTransaction {
                    db.reservaDao().marcarCancelada(rid, System.currentTimeMillis())
                    db.mesaDao().setReservaActiva(mesa.id, null)
                }
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_reserve_cancel, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun bloquear(mesa: Mesa) {
        if (!exigirMapaEditable()) return
        if (mesa.comandaActivaId != null) {
            _mensaje.value = ctx.getString(R.string.error_block_active)
            return
        }
        viewModelScope.launch {
            try {
                db.withTransaction {
                    // Al bloquear, cancelar reserva activa si la hubiera
                    mesa.reservaActivaId?.let { rid ->
                        db.reservaDao().marcarCancelada(rid, System.currentTimeMillis())
                        db.mesaDao().setReservaActiva(mesa.id, null)
                    }
                    db.mesaDao().setBloqueada(mesa.id, true)
                }
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_block, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun desbloquear(mesa: Mesa) {
        if (!exigirMapaEditable()) return
        viewModelScope.launch {
            try {
                db.mesaDao().setBloqueada(mesa.id, false)
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_unblock, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun crearSala(nombre: String) {
        if (!exigirMapaEditable()) return
        val n = nombre.trim()
        if (n.isEmpty()) {
            _mensaje.value = ctx.getString(R.string.mesas_sala_nombre_vacio)
            return
        }
        viewModelScope.launch {
            try {
                val id = db.salaDao().insert(
                    Sala(nombre = n, orden = db.salaDao().getMaxOrden() + 1)
                )
                _salaId.value = id
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_create_sala, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun renombrarSala(sala: Sala, nombre: String) {
        if (!exigirMapaEditable()) return
        val n = nombre.trim()
        if (n.isEmpty()) {
            _mensaje.value = ctx.getString(R.string.mesas_sala_nombre_vacio)
            return
        }
        viewModelScope.launch {
            try {
                db.salaDao().update(sala.copy(nombre = n))
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_rename_sala, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun eliminarSala(sala: Sala) {
        if (!exigirMapaEditable()) return
        viewModelScope.launch {
            try {
                if (db.mesaDao().countPorSala(sala.id) > 0) {
                    _mensaje.value = ctx.getString(R.string.mesas_sala_no_vacia)
                    return@launch
                }
                db.salaDao().deleteById(sala.id)
                if (_salaId.value == sala.id) _salaId.value = null
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_delete_sala, e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
