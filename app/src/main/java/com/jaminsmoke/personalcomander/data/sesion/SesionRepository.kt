package com.jaminsmoke.personalcomander.data.sesion

import android.content.Context
import androidx.room.withTransaction
import com.jaminsmoke.personalcomander.data.AppDatabase
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
    private val scope: CoroutineScope,
    private val db: AppDatabase,
) {
    private val store = SesionStore(context)

    private val _modo = MutableStateFlow(store.cargar())
    val modo: StateFlow<ModoSesion> = _modo.asStateFlow()

    private val _foto = MutableStateFlow<ByteArray?>(null)
    val foto: StateFlow<ByteArray?> = _foto.asStateFlow()

    private val _membresias = MutableStateFlow(store.cargarMembresias())
    val membresias: StateFlow<List<MembresiaEstablecimiento>> = _membresias.asStateFlow()

    private val _visibilidad = MutableStateFlow(store.cargarVisibilidad())
    val visibilidad: StateFlow<VisibilidadCamarero> = _visibilidad.asStateFlow()

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
        origin: DataOrigin = DataOrigin.Real,
    ): IdentityRespuesta<IdentityJson.SesionIdentity> = withContext(Dispatchers.IO) {
        val r = cliente().registrar(nombre, apellidos, email, password, telefono, nick, origin)
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
            val qr: String?
            val fichaUrl: String?
            when {
                qrResp.codigo == 409 || qrResp.code == IdentityJson.CODE_CREDENTIAL_REVOKED -> {
                    qr = null
                    fichaUrl = null
                }
                qrResp.ok && qrResp.valor != null -> {
                    qr = qrResp.valor.qr
                    fichaUrl = qrResp.valor.fichaUrl
                }
                else -> {
                    qr = _modo.value.qr
                    fichaUrl = _modo.value.fichaUrl
                }
            }
            if (qr == null && _modo.value is ModoSesion.Establecimiento) desconectarBar()
            persistir(perfil, qr, token, fichaUrl)
            cargarFoto(token, perfil.fotoUrl)
            refrescarMembresias(token)
            refrescarVisibilidad(token)
            revalidarTurno()
        }
    }

    suspend fun actualizarFicha(
        nick: String,
        direccion: String?,
        ciudad: String?,
    ): IdentityRespuesta<PerfilCamarero> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        val nickLimpio = nick.trim()
        if (nickLimpio.isEmpty()) return IdentityRespuesta(false, error = "Nick vacío")
        val dir = direccion?.trim()?.ifEmpty { null }
        val ciu = ciudad?.trim()?.ifEmpty { null }
        return withContext(Dispatchers.IO) {
            val r = cliente().actualizarPerfil(token, nickLimpio, dir, ciu)
            if (r.ok && r.valor != null) {
                persistir(r.valor, _modo.value.qr, token)
            }
            r
        }
    }

    suspend fun cambiarPassword(
        passwordActual: String,
        passwordNueva: String,
    ): IdentityRespuesta<Unit> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            cliente().cambiarPassword(token, passwordActual, passwordNueva)
        }
    }

    suspend fun renovar(): IdentityRespuesta<String> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            val r = cliente().renovar(token)
            if (r.ok && r.valor != null) {
                desconectarBar()
                val perfil = _modo.value.perfil ?: return@withContext IdentityRespuesta(
                    true,
                    r.valor.qr,
                    codigo = r.codigo,
                )
                persistir(perfil, r.valor.qr, token, r.valor.fichaUrl)
            }
            IdentityRespuesta(r.ok, r.valor?.qr, error = r.error, codigo = r.codigo, code = r.code)
        }
    }

    suspend fun revocar(): IdentityRespuesta<Unit> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            val r = cliente().revocar(token)
            if (r.ok) {
                desconectarBar()
                val perfil = _modo.value.perfil ?: return@withContext r
                persistir(perfil, qr = null, token = token, fichaUrl = null)
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
        _membresias.value = emptyList()
        _visibilidad.value = VisibilidadCamarero.DEFAULT
        _modo.value = ModoSesion.Local
    }

    suspend fun conectarBar(host: String, puerto: Int = BarLanCliente.PUERTO): ConectarBarResult =
        withContext(Dispatchers.IO) {
            val actual = _modo.value
            val perfil = actual.perfil ?: return@withContext ConectarBarResult(ok = false)
            val qr = actual.qr ?: return@withContext ConectarBarResult(ok = false)
            val token = actual.token ?: return@withContext ConectarBarResult(ok = false)
            val health = BarLanCliente.health(host, puerto)
            if (!BarLanCliente.esBar(health)) return@withContext ConectarBarResult(ok = false)
            val sesion = BarLanCliente.postSesion(host, puerto, qr)
            val nombre = health?.establecimiento?.trim()?.takeIf { it.isNotEmpty() }
            val establecimiento = ModoSesion.Establecimiento(
                perfil = perfil,
                qr = qr,
                token = token,
                barHost = host,
                barPuerto = puerto,
                admitido = sesion?.admitido == true,
                nombreEstablecimiento = nombre,
                sesionTrabajo = false,
                fichaUrl = actual.fichaUrl,
            )
            store.guardarEstablecimiento(establecimiento)
            _modo.value = establecimiento
            espejarCarta(host, puerto)
            if (establecimiento.admitido) espejarMapa(host, puerto)
            ConectarBarResult(
                ok = true,
                contraste = IdentityJson.contrastarHealth(nombre, _membresias.value),
                nombreBar = nombre,
                admitido = establecimiento.admitido,
            )
        }

    fun desconectarBar() {
        val actual = _modo.value
        if (actual !is ModoSesion.Establecimiento) return
        val qr = actual.qr
        val host = actual.barHost
        val puerto = actual.barPuerto
        if (actual.sesionTrabajo && qr != null) {
            scope.launch(Dispatchers.IO) { BarLanCliente.postCortar(host, puerto, qr) }
        }
        store.limpiarBar()
        val identidad = ModoSesion.Identidad(actual.perfil, actual.qr, actual.token, actual.fichaUrl)
        store.guardarIdentidad(identidad.perfil, identidad.qr, identidad.token, identidad.fichaUrl)
        _modo.value = identidad
    }

    suspend fun iniciarJornada(): BarLanCliente.JornadaLanResult {
        val actual = _modo.value as? ModoSesion.Establecimiento
            ?: return BarLanCliente.JornadaLanResult(ok = false, codigo = 0)
        val qr = actual.qr ?: return BarLanCliente.JornadaLanResult(ok = false, codigo = 0)
        return withContext(Dispatchers.IO) {
            val r = BarLanCliente.postIniciar(actual.barHost, actual.barPuerto, qr)
            if (r.ok && r.sesionActiva) persistirJornada(true)
            r
        }
    }

    suspend fun cortarJornada(): BarLanCliente.JornadaLanResult {
        val actual = _modo.value as? ModoSesion.Establecimiento
            ?: return BarLanCliente.JornadaLanResult(ok = false, codigo = 0)
        val qr = actual.qr
        return withContext(Dispatchers.IO) {
            val r = if (qr != null) {
                BarLanCliente.postCortar(actual.barHost, actual.barPuerto, qr)
            } else {
                BarLanCliente.JornadaLanResult(ok = true, codigo = 0)
            }
            persistirJornada(false)
            r
        }
    }

    fun marcarJornadaCortada() {
        persistirJornada(false)
    }

    private fun persistirJornada(activa: Boolean) {
        val vigente = _modo.value as? ModoSesion.Establecimiento ?: return
        if (vigente.sesionTrabajo == activa) return
        val nuevo = vigente.copy(sesionTrabajo = activa)
        store.guardarEstablecimiento(nuevo)
        _modo.value = nuevo
    }

    /**
     * Vuelve a consultar lista blanca y el nombre de health sin desactivar el turno.
     * Si pasa a admitido, espeja el mapa. Best-effort: red caída no desliga.
     */
    suspend fun revalidarTurno() {
        val actual = _modo.value as? ModoSesion.Establecimiento ?: return
        val qr = actual.qr ?: return
        withContext(Dispatchers.IO) {
            val sesion = BarLanCliente.postSesion(actual.barHost, actual.barPuerto, qr)
            val health = BarLanCliente.health(actual.barHost, actual.barPuerto)
            val admitido = sesion?.admitido ?: actual.admitido
            val nombre = health?.establecimiento?.trim()?.takeIf { it.isNotEmpty() }
                ?: actual.nombreEstablecimiento
            val jornada = if (admitido) actual.sesionTrabajo else false
            if (
                admitido == actual.admitido &&
                nombre == actual.nombreEstablecimiento &&
                jornada == actual.sesionTrabajo
            ) {
                return@withContext
            }
            if (!jornada && actual.sesionTrabajo) {
                BarLanCliente.postCortar(actual.barHost, actual.barPuerto, qr)
            }
            val nuevo = actual.copy(
                admitido = admitido,
                nombreEstablecimiento = nombre,
                sesionTrabajo = jornada,
            )
            store.guardarEstablecimiento(nuevo)
            _modo.value = nuevo
            if (admitido && !actual.admitido) espejarMapa(actual.barHost, actual.barPuerto)
        }
    }

    /** Best-effort: 404 o red caída no deshacen el ligue. No borra productos locales. */
    private suspend fun espejarCarta(host: String, puerto: Int) {
        val carta = BarLanCliente.carta(host, puerto) ?: return
        val existentes = db.productoDao().getAllIncluyendoOcultos()
        val plan = CartaSync.plan(existentes, carta.productos)
        if (plan.insertar.isEmpty() && plan.actualizar.isEmpty()) return
        db.withTransaction {
            if (plan.insertar.isNotEmpty()) db.productoDao().insertAll(plan.insertar)
            if (plan.actualizar.isNotEmpty()) db.productoDao().updateAll(plan.actualizar)
        }
    }

    /**
     * Réplica de layout al ligar admitido. 404 o `/estado` sin salas no toca el mapa.
     * No corre en el bucle SSE: solo aquí.
     */
    private suspend fun espejarMapa(host: String, puerto: Int) {
        val estado = BarLanCliente.estado(host, puerto) ?: return
        if (estado.salas.isEmpty() && estado.mesas.isEmpty()) return
        db.withTransaction {
            val planSalas = MapaSync.planSalas(db.salaDao().getAll(), estado.salas)
            if (planSalas.actualizar.isNotEmpty()) db.salaDao().updateAll(planSalas.actualizar)
            for (sala in planSalas.insertar) {
                db.salaDao().insert(sala)
            }
            val salasPorCodigo = db.salaDao().getAll()
                .mapNotNull { s -> s.codigoBar?.let { it to s.id } }
                .toMap()
            val planMesas = MapaSync.planMesas(db.mesaDao().getAll(), estado.mesas, salasPorCodigo)
            if (planMesas.insertar.isNotEmpty()) db.mesaDao().insertAll(planMesas.insertar)
            if (planMesas.actualizar.isNotEmpty()) db.mesaDao().updateAll(planMesas.actualizar)
        }
    }

    suspend fun buscarBares(): List<ServidorDescubierto> = withContext(Dispatchers.IO) {
        EscaneadorRed.escanear(listOf(BarLanCliente.PUERTO))
    }

    private fun persistir(
        perfil: PerfilCamarero,
        qr: String?,
        token: String,
        fichaUrl: String? = _modo.value.fichaUrl,
    ) {
        val actual = _modo.value
        if (actual is ModoSesion.Establecimiento) {
            val establecimiento = actual.copy(perfil = perfil, qr = qr, token = token, fichaUrl = fichaUrl)
            store.guardarEstablecimiento(establecimiento)
            _modo.value = establecimiento
        } else {
            store.guardarIdentidad(perfil, qr, token, fichaUrl)
            _modo.value = ModoSesion.Identidad(perfil, qr, token, fichaUrl)
        }
    }

    private fun aplicarSesion(r: IdentityRespuesta<IdentityJson.SesionIdentity>) {
        val sesion = r.valor ?: return
        val token = sesion.token ?: return
        if (!r.ok) return
        persistir(sesion.perfil, sesion.qr, token, sesion.fichaUrl)
        cargarFoto(token, sesion.perfil.fotoUrl)
        refrescarMembresias(token)
        refrescarVisibilidad(token)
    }

    suspend fun actualizarVisibilidad(
        campo: CampoVisibilidad,
        valor: Boolean,
    ): IdentityRespuesta<VisibilidadCamarero> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            val anterior = _visibilidad.value
            _visibilidad.value = anterior.con(campo, valor)
            val r = cliente().actualizarVisibilidad(token, campo, valor)
            if (r.ok && r.valor != null) {
                store.guardarVisibilidad(r.valor)
                _visibilidad.value = r.valor
            } else {
                _visibilidad.value = anterior
            }
            r
        }
    }

    /** Identity gana si responde; red caída o cuerpo inválido conservan la cache. */
    private fun refrescarMembresias(token: String) {
        val r = cliente().meEstablecimientos(token)
        if (!r.ok || r.valor == null) return
        store.guardarMembresias(r.valor)
        _membresias.value = r.valor
    }

    /** Identity gana si responde; red caída o cuerpo inválido conservan la cache. */
    private fun refrescarVisibilidad(token: String) {
        val r = cliente().meVisibilidad(token)
        if (!r.ok || r.valor == null) return
        store.guardarVisibilidad(r.valor)
        _visibilidad.value = r.valor
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
