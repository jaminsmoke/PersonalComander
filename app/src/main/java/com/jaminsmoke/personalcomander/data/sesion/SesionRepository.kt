package com.jaminsmoke.personalcomander.data.sesion

import android.content.Context
import com.jaminsmoke.personalcomander.data.EscaneadorRed
import com.jaminsmoke.personalcomander.data.ServidorDescubierto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SesionRepository(
    context: Context,
    scope: CoroutineScope,
) {
    private val store = SesionStore(context)

    private val _modo = MutableStateFlow(store.cargar())
    val modo: StateFlow<ModoSesion> = _modo.asStateFlow()

    private val _foto = MutableStateFlow<ByteArray?>(null)
    val foto: StateFlow<ByteArray?> = _foto.asStateFlow()

    var identityBaseUrl: String
        get() = store.identityBaseUrl
        set(value) {
            store.identityBaseUrl = value
        }

    init {
        scope.launch { hidratar() }
    }

    private fun cliente() = IdentityCliente(store.identityBaseUrl)

    suspend fun registrar(
        nombre: String,
        apellidos: String,
        email: String,
        password: String,
        telefono: String? = null,
        nick: String,
    ): IdentityRespuesta<IdentityJson.SesionIdentity> = withContext(Dispatchers.IO) {
        val r = cliente().registrar(nombre, apellidos, email, password, telefono, nick)
        aplicarSesion(r)
        r
    }

    suspend fun login(email: String, password: String): IdentityRespuesta<IdentityJson.SesionIdentity> =
        withContext(Dispatchers.IO) {
            val r = cliente().login(email, password)
            aplicarSesion(r)
            r
        }

    suspend fun hidratar() {
        val token = _modo.value.token ?: return
        withContext(Dispatchers.IO) {
            val me = cliente().me(token)
            if (me.codigo == 401) {
                withContext(Dispatchers.Main.immediate) { cerrarSesion() }
                return@withContext
            }
            val perfil = me.valor ?: return@withContext
            val qrResp = cliente().meQr(token)
            val qr = when {
                qrResp.codigo == 409 || qrResp.code == IdentityJson.CODE_CREDENTIAL_REVOKED -> null
                qrResp.ok -> qrResp.valor
                else -> _modo.value.qr
            }
            if (qr == null && _modo.value is ModoSesion.Establecimiento) desconectarBar()
            persistir(perfil, qr, token)
            cargarFoto(token, perfil.fotoUrl)
        }
    }

    suspend fun actualizarNick(nick: String): IdentityRespuesta<PerfilCamarero> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        val limpio = nick.trim()
        if (limpio.isEmpty()) return IdentityRespuesta(false, error = "Nick vacío")
        return withContext(Dispatchers.IO) {
            val r = cliente().actualizarPerfil(token, limpio)
            if (r.ok && r.valor != null) {
                persistir(r.valor, _modo.value.qr, token)
            }
            r
        }
    }

    suspend fun renovar(): IdentityRespuesta<String> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            val r = cliente().renovar(token)
            if (r.ok && r.valor != null) {
                desconectarBar()
                val perfil = _modo.value.perfil ?: return@withContext r
                persistir(perfil, r.valor, token)
            }
            r
        }
    }

    suspend fun revocar(): IdentityRespuesta<Unit> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            val r = cliente().revocar(token)
            if (r.ok) {
                desconectarBar()
                val perfil = _modo.value.perfil ?: return@withContext r
                persistir(perfil, qr = null, token)
            }
            r
        }
    }

    suspend fun subirFoto(bytes: ByteArray, mime: String): IdentityRespuesta<String?> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            val r = cliente().subirFoto(token, bytes, mime)
            if (r.ok) {
                val perfil = _modo.value.perfil?.copy(fotoUrl = r.valor ?: "/v1/camareros/me/foto")
                    ?: return@withContext r
                persistir(perfil, _modo.value.qr, token)
                cargarFoto(token, perfil.fotoUrl)
            }
            r
        }
    }

    suspend fun borrarFoto(): IdentityRespuesta<Unit> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            val r = cliente().borrarFoto(token)
            if (r.ok) {
                val perfil = _modo.value.perfil?.copy(fotoUrl = null) ?: return@withContext r
                persistir(perfil, _modo.value.qr, token)
                _foto.value = null
            }
            r
        }
    }

    suspend fun borrarCuenta(password: String): IdentityRespuesta<Unit> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            val r = cliente().suprimirCuenta(token, password)
            if (r.ok) {
                withContext(Dispatchers.Main.immediate) { cerrarSesion() }
            }
            r
        }
    }

    fun cerrarSesion() {
        store.limpiarTodo()
        _foto.value = null
        _modo.value = ModoSesion.Local
    }

    suspend fun conectarBar(host: String, puerto: Int = BarLanCliente.PUERTO): Boolean =
        withContext(Dispatchers.IO) {
            val actual = _modo.value
            val perfil = actual.perfil ?: return@withContext false
            val qr = actual.qr ?: return@withContext false
            val token = actual.token ?: return@withContext false
            val health = BarLanCliente.health(host, puerto)
            if (!BarLanCliente.esBar(health)) return@withContext false
            val establecimiento = ModoSesion.Establecimiento(
                perfil = perfil,
                qr = qr,
                token = token,
                barHost = host,
                barPuerto = puerto,
                admitido = false,
            )
            store.guardarEstablecimiento(establecimiento)
            _modo.value = establecimiento
            true
        }

    fun desconectarBar() {
        val actual = _modo.value
        if (actual !is ModoSesion.Establecimiento) return
        store.limpiarBar()
        val identidad = ModoSesion.Identidad(actual.perfil, actual.qr, actual.token)
        store.guardarIdentidad(identidad.perfil, identidad.qr, identidad.token)
        _modo.value = identidad
    }

    suspend fun buscarBares(): List<ServidorDescubierto> = withContext(Dispatchers.IO) {
        EscaneadorRed.escanear(listOf(BarLanCliente.PUERTO))
    }

    private fun persistir(perfil: PerfilCamarero, qr: String?, token: String) {
        val actual = _modo.value
        if (actual is ModoSesion.Establecimiento) {
            val establecimiento = actual.copy(perfil = perfil, qr = qr, token = token)
            store.guardarEstablecimiento(establecimiento)
            _modo.value = establecimiento
        } else {
            store.guardarIdentidad(perfil, qr, token)
            _modo.value = ModoSesion.Identidad(perfil, qr, token)
        }
    }

    private fun aplicarSesion(r: IdentityRespuesta<IdentityJson.SesionIdentity>) {
        val sesion = r.valor ?: return
        val token = sesion.token ?: return
        if (!r.ok) return
        persistir(sesion.perfil, sesion.qr, token)
        cargarFoto(token, sesion.perfil.fotoUrl)
    }

    private fun cargarFoto(token: String, fotoUrl: String?) {
        if (fotoUrl.isNullOrBlank()) {
            _foto.value = null
            return
        }
        val r = cliente().foto(token)
        _foto.value = if (r.ok) r.valor else null
    }
}
