package com.jaminsmoke.personalcomander.data.sesion

import android.content.Context
import com.jaminsmoke.personalcomander.data.EscaneadorRed
import com.jaminsmoke.personalcomander.data.ServidorDescubierto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SesionRepository(context: Context) {
    private val store = SesionStore(context)

    private val _modo = MutableStateFlow(store.cargar())
    val modo: StateFlow<ModoSesion> = _modo.asStateFlow()

    var identityBaseUrl: String
        get() = store.identityBaseUrl
        set(value) {
            store.identityBaseUrl = value
        }

    private fun cliente() = IdentityCliente(store.identityBaseUrl)

    suspend fun registrar(
        nombre: String,
        apellidos: String,
        email: String,
        password: String,
        telefono: String? = null,
    ): IdentityRespuesta<IdentityJson.SesionIdentity> = withContext(Dispatchers.IO) {
        val r = cliente().registrar(nombre, apellidos, email, password, telefono)
        aplicarSesion(r)
        r
    }

    suspend fun login(email: String, password: String): IdentityRespuesta<IdentityJson.SesionIdentity> =
        withContext(Dispatchers.IO) {
            val r = cliente().login(email, password)
            aplicarSesion(r)
            r
        }

    fun cerrarSesion() {
        store.limpiarTodo()
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
            val sala = ModoSesion.Sala(
                perfil = perfil,
                qr = qr,
                token = token,
                barHost = host,
                barPuerto = puerto,
                admitido = false,
            )
            store.guardarSala(sala)
            _modo.value = sala
            true
        }

    fun desconectarBar() {
        val actual = _modo.value
        if (actual !is ModoSesion.Sala) return
        store.limpiarBar()
        val identidad = ModoSesion.Identidad(actual.perfil, actual.qr, actual.token)
        store.guardarIdentidad(identidad.perfil, identidad.qr, identidad.token)
        _modo.value = identidad
    }

    suspend fun buscarBares(): List<ServidorDescubierto> = withContext(Dispatchers.IO) {
        EscaneadorRed.escanear(listOf(BarLanCliente.PUERTO))
    }

    private fun aplicarSesion(r: IdentityRespuesta<IdentityJson.SesionIdentity>) {
        val sesion = r.valor ?: return
        val token = sesion.token ?: return
        if (!r.ok) return
        store.guardarIdentidad(sesion.perfil, sesion.qr, token)
        _modo.value = ModoSesion.Identidad(sesion.perfil, sesion.qr, token)
    }
}
