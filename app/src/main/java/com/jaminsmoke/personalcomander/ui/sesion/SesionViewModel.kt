package com.jaminsmoke.personalcomander.ui.sesion

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.sesion.CampoVisibilidad
import com.jaminsmoke.personalcomander.data.sesion.IdentityJson
import com.jaminsmoke.personalcomander.data.sesion.IdentityRespuesta
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.data.sesion.VisibleOtrosEstablecimientos
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
    val invitaciones = repo.invitaciones
    val visibilidad = repo.visibilidad

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun limpiarMensaje() {
        _mensaje.value = null
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
            try {
                val (correo, clave) = IdentityJson.normalizarCredenciales(email, password)
                val r = repo.registrar(nombre.trim(), apellidos.trim(), correo, clave, telefono, nick)
                if (!r.ok) _mensaje.value = mensajeAuth(r)
            } catch (_: Exception) {
                _mensaje.value = ctx.getString(R.string.sesion_error_generico)
            } finally {
                _busy.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val (correo, clave) = IdentityJson.normalizarCredenciales(email, password)
                val r = repo.login(correo, clave)
                if (!r.ok) _mensaje.value = mensajeAuth(r)
            } catch (_: Exception) {
                _mensaje.value = ctx.getString(R.string.sesion_error_generico)
            } finally {
                _busy.value = false
            }
        }
    }

    private fun mensajeAuth(r: IdentityRespuesta<*>): String =
        when (r.code) {
            IdentityJson.CODE_CREDENTIAL_REVOKED -> ctx.getString(R.string.sesion_error_clave_revocada)
            IdentityJson.CODE_CREDENCIALES_INVALIDAS -> ctx.getString(R.string.sesion_error_credenciales)
            else -> r.error ?: ctx.getString(R.string.sesion_error_generico)
        }

    fun cerrarSesion() {
        repo.cerrarSesion()
    }

    fun actualizarFicha(nick: String, direccion: String, ciudad: String) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.actualizarFicha(nick, direccion, ciudad)
            _busy.value = false
            _mensaje.value = if (r.ok) ctx.getString(R.string.sesion_ficha_ok)
            else r.error ?: ctx.getString(R.string.sesion_error_generico)
        }
    }

    fun cambiarPassword(passwordActual: String, passwordNueva: String) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.cambiarPassword(passwordActual, passwordNueva)
            _busy.value = false
            _mensaje.value = if (r.ok) {
                ctx.getString(R.string.sesion_password_cambiada)
            } else {
                when (r.code) {
                    IdentityJson.CODE_PASSWORD_INCORRECTA ->
                        ctx.getString(R.string.sesion_password_actual_incorrecta)
                    else -> r.error ?: ctx.getString(R.string.sesion_error_generico)
                }
            }
        }
    }

    fun setVisibilidad(campo: CampoVisibilidad, valor: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.actualizarVisibilidad(campo, valor)
            _busy.value = false
            _mensaje.value = if (r.ok) ctx.getString(R.string.sesion_visibilidad_ok)
            else r.error ?: ctx.getString(R.string.sesion_error_generico)
        }
    }

    fun setVisibleOtrosEstablecimientos(visible: VisibleOtrosEstablecimientos) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.actualizarVisibilidadEstablecimientos(visible)
            _busy.value = false
            _mensaje.value = if (r.ok) ctx.getString(R.string.sesion_directorio_ok)
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

    fun desconectarBar() {
        repo.desconectarBar()
    }

    fun cargarInvitaciones() {
        if (modo.value is ModoSesion.Local) return
        viewModelScope.launch {
            _busy.value = true
            val r = repo.cargarInvitaciones()
            _busy.value = false
            if (!r.ok) {
                _mensaje.value = r.error ?: ctx.getString(R.string.gestion_invitaciones_error)
            }
        }
    }

    fun aceptarInvitacion(id: String) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.aceptarInvitacion(id)
            _busy.value = false
            _mensaje.value = when {
                r.ok -> ctx.getString(R.string.gestion_invitaciones_aceptada)
                r.code == IdentityJson.CODE_INVITACION_USADA ->
                    ctx.getString(R.string.gestion_invitaciones_ya_usada)
                r.code == IdentityJson.CODE_INVITACION_EXPIRADA ->
                    ctx.getString(R.string.gestion_invitaciones_expirada)
                else -> r.error ?: ctx.getString(R.string.sesion_error_generico)
            }
        }
    }

    fun rechazarInvitacion(id: String) {
        viewModelScope.launch {
            _busy.value = true
            val r = repo.rechazarInvitacion(id)
            _busy.value = false
            _mensaje.value = when {
                r.ok -> ctx.getString(R.string.gestion_invitaciones_rechazada)
                r.code == IdentityJson.CODE_INVITACION_USADA ->
                    ctx.getString(R.string.gestion_invitaciones_ya_usada)
                r.code == IdentityJson.CODE_INVITACION_EXPIRADA ->
                    ctx.getString(R.string.gestion_invitaciones_expirada)
                else -> r.error ?: ctx.getString(R.string.sesion_error_generico)
            }
        }
    }

    fun revalidarTurno() {
        viewModelScope.launch { repo.revalidarTurno() }
    }

    companion object {
        private const val FOTO_MAX_BYTES = 2 * 1024 * 1024
    }
}
