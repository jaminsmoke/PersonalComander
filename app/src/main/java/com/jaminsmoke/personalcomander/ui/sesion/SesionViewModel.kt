package com.jaminsmoke.personalcomander.ui.sesion

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.ServidorDescubierto
import com.jaminsmoke.personalcomander.data.sesion.ContrasteMembresia
import com.jaminsmoke.personalcomander.data.sesion.IdentityJson
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.data.sesion.SesionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SesionViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as PersonalComanderApp).sesion
    private val ctx = getApplication<Application>()

    val modo: StateFlow<ModoSesion> = repo.modo
    val foto: StateFlow<ByteArray?> = repo.foto
    val membresias = repo.membresias

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

    fun refrescarPerfil() {
        viewModelScope.launch { repo.hidratar() }
    }

    fun registrar(
        nombre: String,
        apellidos: String,
        email: String,
        password: String,
        telefono: String? = null,
        nick: String,
    ) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.registrar(nombre, apellidos, email, password, telefono, nick)
            _busy.value = false
            if (!r.ok) _mensaje.value = r.error ?: ctx.getString(R.string.sesion_error_generico)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.login(email, password)
            _busy.value = false
            if (!r.ok) {
                _mensaje.value = when (r.code) {
                    IdentityJson.CODE_CREDENTIAL_REVOKED -> ctx.getString(R.string.sesion_error_clave_revocada)
                    else -> r.error ?: ctx.getString(R.string.sesion_error_generico)
                }
            }
        }
    }

    fun cerrarSesion() {
        repo.cerrarSesion()
    }

    fun actualizarNick(nick: String) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.actualizarNick(nick)
            _busy.value = false
            _mensaje.value = if (r.ok) ctx.getString(R.string.sesion_nick_ok)
            else r.error ?: ctx.getString(R.string.sesion_error_generico)
        }
    }

    fun renovarQr() {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.renovar()
            _busy.value = false
            _mensaje.value = if (r.ok) ctx.getString(R.string.sesion_renovar_ok)
            else r.error ?: ctx.getString(R.string.sesion_error_generico)
        }
    }

    fun revocarQr() {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.revocar()
            _busy.value = false
            _mensaje.value = if (r.ok) ctx.getString(R.string.sesion_revocar_ok)
            else r.error ?: ctx.getString(R.string.sesion_error_generico)
        }
    }

    fun subirFoto(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            val bytes = withContext(Dispatchers.IO) {
                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null) {
                _busy.value = false
                _mensaje.value = ctx.getString(R.string.sesion_foto_error)
                return@launch
            }
            if (bytes.size > FOTO_MAX_BYTES) {
                _busy.value = false
                _mensaje.value = ctx.getString(R.string.sesion_foto_grande)
                return@launch
            }
            val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
            val r = repo.subirFoto(bytes, mime)
            _busy.value = false
            _mensaje.value = if (r.ok) ctx.getString(R.string.sesion_foto_ok)
            else r.error ?: ctx.getString(R.string.sesion_error_generico)
        }
    }

    fun borrarFoto() {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.borrarFoto()
            _busy.value = false
            _mensaje.value = if (r.ok) ctx.getString(R.string.sesion_foto_borrada)
            else r.error ?: ctx.getString(R.string.sesion_error_generico)
        }
    }

    fun borrarCuenta(password: String) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.borrarCuenta(password)
            _busy.value = false
            if (r.ok) {
                _mensaje.value = ctx.getString(R.string.sesion_cuenta_borrada)
            } else {
                _mensaje.value = when (r.code) {
                    IdentityJson.CODE_PASSWORD_INCORRECTA -> ctx.getString(R.string.sesion_password_incorrecta)
                    else -> r.error ?: ctx.getString(R.string.sesion_error_generico)
                }
            }
        }
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
            val r = repo.conectarBar(host, puerto)
            _busy.value = false
            _mensaje.value = when {
                !r.ok -> ctx.getString(R.string.sesion_bar_no_health)
                r.contraste == ContrasteMembresia.NoCoincide ->
                    ctx.getString(
                        R.string.sesion_bar_conectado_sin_membresia,
                        r.nombreBar ?: host,
                    )
                else -> ctx.getString(R.string.sesion_bar_conectado_pendiente)
            }
        }
    }

    fun desconectarBar() {
        repo.desconectarBar()
    }

    companion object {
        private const val FOTO_MAX_BYTES = 2 * 1024 * 1024
    }
}
