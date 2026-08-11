package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.data.Mesa
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MesasViewModel(application: Application) : AndroidViewModel(application) {
    val mesas: Flow<List<Mesa>> =
        (application as PersonalComanderApp).db.mesaDao().observeAll()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun mostrarError(mensaje: String) { _mensaje.value = mensaje }
    fun limpiarMensaje() { _mensaje.value = null }
}
