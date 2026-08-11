package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val cargando: Boolean = true,
    val mesasTotales: Int = 0,
    val mesasOcupadas: Int = 0,
    val pedidosAbiertos: Int = 0,
    val totalHoy: Double = 0.0,
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as PersonalComanderApp).db

    /** Emits a new value at start and after midnight, so totalHoy recalculates daily */
    private val _inicioDelDia = MutableStateFlow(todayStart())

    init {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val tomorrow = LocalDate.now().plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val msUntilMidnight = tomorrow - now + 60_000 // 1 min past midnight
                delay(msUntilMidnight.coerceAtLeast(60_000))
                _inicioDelDia.value = todayStart()
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = _inicioDelDia.flatMapLatest { inicio ->
        combine(
            db.mesaDao().observeCount(),
            db.mesaDao().observeOcupadas(),
            db.pedidoDao().observeAbiertos(),
            db.pedidoDao().observeTotalHoy(inicio)
        ) { totales, ocupadas, abiertos, total ->
            HomeUiState(
                cargando = false,
                mesasTotales = totales,
                mesasOcupadas = ocupadas,
                pedidosAbiertos = abiertos,
                totalHoy = total
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    companion object {
        fun todayStart(): Long =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
