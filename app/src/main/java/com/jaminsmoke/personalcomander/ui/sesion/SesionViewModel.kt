package com.jaminsmoke.personalcomander.ui.sesion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.ServidorDescubierto
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.data.sesion.SesionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SesionViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as PersonalComanderApp).sesion
    private val ctx = getApplication<Application>()

    val modo: StateFlow<ModoSesion> = repo.modo

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _identityUrl = MutableStateFlow(repo.identityBaseUrl)
    val identityUrl: StateFlow<String> = _identityUrl.asStateFlow()

    private val _bares = MutableStateFlow<List<ServidorDescubierto>>(emptyList())
    val bares: StateFlow<List<ServidorDescubierto>> = _bares.asStateFlow()

    private val _escaneando = MutableStateFlow(false)
    val escaneando: StateFlow<Boolean> = _escaneando.asStateFlow()

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    fun setIdentityUrl(url: String) {
        _identityUrl.value = url
        repo.identityBaseUrl = url.ifBlank { SesionStore.DEFAULT_IDENTITY_URL }
    }

    fun registrar(nombre: String, apellidos: String, email: String, password: String) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.registrar(nombre, apellidos, email, password)
            _busy.value = false
            if (!r.ok) {
                _mensaje.value = r.error ?: ctx.getString(R.string.sesion_error_generico)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.login(email, password)
            _busy.value = false
            if (!r.ok) {
                _mensaje.value = r.error ?: ctx.getString(R.string.sesion_error_generico)
            }
        }
    }

    fun cerrarSesion() {
        repo.cerrarSesion()
    }

    fun buscarBares() {
        viewModelScope.launch {
            _escaneando.value = true
            _bares.value = repo.buscarBares()
            _escaneando.value = false
            if (_bares.value.isEmpty()) {
                _mensaje.value = ctx.getString(R.string.sesion_bar_ninguno)
            }
        }
    }

    fun conectarBar(host: String, puerto: Int) {
        viewModelScope.launch {
            _busy.value = true
            val ok = repo.conectarBar(host, puerto)
            _busy.value = false
            _mensaje.value = ctx.getString(
                if (ok) R.string.sesion_bar_conectado_pendiente else R.string.sesion_bar_no_health,
            )
        }
    }

    fun desconectarBar() {
        repo.desconectarBar()
    }
}
