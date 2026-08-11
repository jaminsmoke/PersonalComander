package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val mesasTotales: Int = 0,
    val mesasOcupadas: Int = 0,
    val pedidosAbiertos: Int = 0,
    val totalHoy: Double = 0.0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as PersonalComanderApp).db

    val uiState: StateFlow<HomeUiState> = run {
        val inicioDelDia = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        combine(
            db.mesaDao().observeCount(),
            db.mesaDao().observeOcupadas(),
            db.pedidoDao().observeAbiertos(),
            db.pedidoDao().observeTotalHoy(inicioDelDia)
        ) { totales, ocupadas, abiertos, total ->
            HomeUiState(
                mesasTotales = totales,
                mesasOcupadas = ocupadas,
                pedidosAbiertos = abiertos,
                totalHoy = total
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
    }
}
