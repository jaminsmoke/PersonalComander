package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.R
import androidx.room.withTransaction
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaForma
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

    init {
        // Normalizar posiciones de mesas legacy una sola vez por zona.
        // distinctUntilChanged por zona evita re-ejecuciones al añadir/mover mesas.
        viewModelScope.launch {
            combine(mesas, zona) { mesas, zona -> mesas to zona }
                .distinctUntilChanged { old, new -> old.second == new.second }
                .collect { (todasLasMesas, zonaActual) ->
                    if (zonaActual != null) {
                        val mesasZona = todasLasMesas.filter { it.zona == zonaActual }
                        if (mesasZona.isNotEmpty()) {
                            normalizarPosiciones(mesasZona)
                        }
                    }
                }
        }
    }

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
                db.withTransaction {
                    db.mesaDao().deleteById(mesa.id)
                    db.mesaDao().renumberAfter(mesa.numero)
                    // Correr índices posteriores de la misma zona (B3→B2) para mantener coherencia
                    if (mesa.indiceZona > 0) {
                        db.mesaDao().decrementarIndicesZona(mesa.zona, mesa.indiceZona)
                    }
                }
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
                db.withTransaction {
                    val nuevoGiro = !mesa.girada
                    val (w, h) = mesaDims(mesa.forma, nuevoGiro)
                    val ocupadas = db.mesaDao().getPorZona(mesa.zona)
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

    fun createMesa(zona: String, forma: MesaForma, capacidad: Int, alias: String?) {
        viewModelScope.launch {
            try {
                db.withTransaction {
                    val maxNum = db.mesaDao().getMaxNumero()
                    val a = alias?.trim()?.ifBlank { null }
                    // Siguiente índice secuencial dentro de la zona (B3 si ya hay B1, B2)
                    val siguienteIndice = db.mesaDao().getMaxIndiceZona(zona) + 1
                    // Colocar la mesa al lado de la última de SU zona (no por count
                    // global), pero siempre dentro de los límites del grid estándar.
                    val mesasZona = db.mesaDao().getPorZona(zona)
                    val candidata = mesasZona.maxByOrNull { it.posX }
                        ?.let {
                            val (lastW, _) = mesaDims(it.forma, it.girada)
                            it.posX + lastW + CELL_F to it.posY
                        }
                        ?: ((maxNum % 4) * 160f to (maxNum / 4) * 160f + CELL_F)
                    val ocupadas = mesasZona.map {
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
                            zona = zona, capacidad = capacidad, indiceZona = siguienteIndice,
                            posX = px, posY = py
                        )
                    )
                }
            } catch (e: Exception) {
                _mensaje.value = ctx.getString(R.string.error_create_table, e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
