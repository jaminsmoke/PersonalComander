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
import java.time.Instant

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

    private val _invitaciones = MutableStateFlow<List<InvitacionCamarero>>(emptyList())
    val invitaciones: StateFlow<List<InvitacionCamarero>> = _invitaciones.asStateFlow()

    private val _visibilidad = MutableStateFlow(store.cargarVisibilidad())
    val visibilidad: StateFlow<VisibilidadCamarero> = _visibilidad.asStateFlow()

    var identityBaseUrl: String
        get() = store.identityBaseUrl
        set(value) {
            store.identityBaseUrl = value
        }

    init {
        soltarSiNoAdmitido()
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
            soltarSiNoAdmitido()
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
        _invitaciones.value = emptyList()
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
            val establecimientoId = health?.establecimientoId
            val contraste = IdentityJson.contrastarHealth(nombre, _membresias.value, establecimientoId)
            if (sesion?.admitido != true) {
                return@withContext ConectarBarResult(
                    ok = true,
                    contraste = contraste,
                    nombreBar = nombre,
                    admitido = false,
                )
            }
            val establecimiento = ModoSesion.Establecimiento(
                perfil = perfil,
                qr = qr,
                token = token,
                barHost = host,
                barPuerto = puerto,
                admitido = true,
                nombreEstablecimiento = nombre,
                establecimientoId = establecimientoId,
                sesionTrabajo = false,
                fichaUrl = actual.fichaUrl,
            )
            store.guardarEstablecimiento(establecimiento)
            _modo.value = establecimiento
            espejarCarta(host, puerto)
            espejarMapa(host, puerto)
            ConectarBarResult(
                ok = true,
                contraste = contraste,
                nombreBar = nombre,
                admitido = true,
            )
        }

    fun desconectarBar() {
        val actual = _modo.value
        if (actual !is ModoSesion.Establecimiento) return
        val qr = actual.qr
        val host = actual.barHost
        val puerto = actual.barPuerto
        val token = actual.token
        val cortarLan = actual.sesionTrabajo && qr != null
        scope.launch(Dispatchers.IO) {
            if (cortarLan) BarLanCliente.postCortar(host, puerto, qr)
            cortarJornadaServer(token)
        }
        store.limpiarBar()
        val identidad = ModoSesion.Identidad(
            actual.perfil,
            actual.qr,
            actual.token,
            normalizarFichaUrl(actual.fichaUrl),
        )
        store.guardarIdentidad(identidad.perfil, identidad.qr, identidad.token, identidad.fichaUrl)
        _modo.value = identidad
    }

    suspend fun pedirJornada(host: String, puerto: Int): BarLanCliente.JornadaLanResult {
        val actual = _modo.value
        if (actual is ModoSesion.Establecimiento &&
            actual.sesionTrabajo &&
            !mismosNodo(host, puerto, actual)
        ) {
            cortarJornada()
        }
        val ligado = _modo.value as? ModoSesion.Establecimiento
        if (ligado == null || !mismosNodo(host, puerto, ligado) || !ligado.admitido) {
            val r = conectarBar(host, puerto)
            if (!r.ok) return BarLanCliente.JornadaLanResult(ok = false, codigo = 0)
            if (!r.admitido) return BarLanCliente.JornadaLanResult(ok = false, codigo = 403)
        }
        return iniciarJornada()
    }

    suspend fun iniciarJornada(): BarLanCliente.JornadaLanResult {
        val actual = _modo.value as? ModoSesion.Establecimiento
            ?: return BarLanCliente.JornadaLanResult(ok = false, codigo = 0)
        val qr = actual.qr ?: return BarLanCliente.JornadaLanResult(ok = false, codigo = 0)
        return withContext(Dispatchers.IO) {
            val r = BarLanCliente.postIniciar(actual.barHost, actual.barPuerto, qr)
            if (r.ok && r.sesionActiva) {
                persistirJornada(true)
                registrarJornadaServer(actual.token, actual.nombreEstablecimiento, actual.establecimientoId)
            }
            r
        }
    }

    suspend fun cortarJornada(): BarLanCliente.JornadaLanResult {
        val actual = _modo.value as? ModoSesion.Establecimiento
            ?: return BarLanCliente.JornadaLanResult(ok = false, codigo = 0)
        val qr = actual.qr
        val token = actual.token
        return withContext(Dispatchers.IO) {
            val r = if (qr != null) {
                BarLanCliente.postCortar(actual.barHost, actual.barPuerto, qr)
            } else {
                BarLanCliente.JornadaLanResult(ok = true, codigo = 0)
            }
            persistirJornada(false)
            cortarJornadaServer(token)
            r
        }
    }

    suspend fun resumenOficio(desde: Instant, hasta: Instant): IdentityRespuesta<ResumenOficio> =
        withContext(Dispatchers.IO) {
            val token = _modo.value.token
                ?: return@withContext IdentityRespuesta(false, error = "Sin sesión")
            cliente().meResumen(token, desde, hasta)
        }

    suspend fun jornadasOficio(desde: Instant, hasta: Instant): IdentityRespuesta<List<JornadaOficio>> =
        withContext(Dispatchers.IO) {
            val token = _modo.value.token
                ?: return@withContext IdentityRespuesta(false, error = "Sin sesión")
            cliente().meJornadas(token, desde, hasta)
        }

    /** Dual-write al libro canónico. 409 (ya abierta) cuenta como éxito. Sin UUID no se inventa. */
    private fun registrarJornadaServer(
        token: String,
        nombreHealth: String?,
        healthId: String? = null,
    ) {
        val establecimientoId = IdentityJson.establecimientoIdPorHealth(
            nombreHealth,
            _membresias.value,
            healthId,
        ) ?: return
        val r = cliente().iniciarJornada(token, establecimientoId)
        if (r.ok) return
        if (r.codigo == 409 || r.code == IdentityJson.CODE_JORNADA_YA_ABIERTA) return
    }

    /** Cierra el intervalo canónico. 404 (no abierta) se ignora. */
    private fun cortarJornadaServer(token: String) {
        val r = cliente().cortarJornada(token)
        if (r.ok) return
        if (r.codigo == 404 || r.code == IdentityJson.CODE_JORNADA_NO_ABIERTA) return
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
            if (sesion != null && !sesion.admitido) {
                if (actual.sesionTrabajo) {
                    BarLanCliente.postCortar(actual.barHost, actual.barPuerto, qr)
                }
                withContext(Dispatchers.Main.immediate) { desconectarBar() }
                return@withContext
            }
            val health = BarLanCliente.health(actual.barHost, actual.barPuerto)
            val admitido = sesion?.admitido ?: actual.admitido
            val nombre = health?.establecimiento?.trim()?.takeIf { it.isNotEmpty() }
                ?: actual.nombreEstablecimiento
            val establecimientoId = health?.establecimientoId ?: actual.establecimientoId
            val jornada = if (admitido) actual.sesionTrabajo else false
            if (
                admitido == actual.admitido &&
                nombre == actual.nombreEstablecimiento &&
                establecimientoId == actual.establecimientoId &&
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
                establecimientoId = establecimientoId,
                sesionTrabajo = jornada,
            )
            store.guardarEstablecimiento(nuevo)
            _modo.value = nuevo
            if (admitido && !actual.admitido) espejarMapa(actual.barHost, actual.barPuerto)
        }
    }

    /**
     * Best-effort: 404 o red caída no deshacen el ligue. No borra productos locales.
     * Si el esquema de carta cambió (p. ej. slug→UUID), reconstruye el espejo
     * re-apuntando `codigoBar` por nombre — sin borrar, para no romper líneas históricas.
     */
    private suspend fun espejarCarta(host: String, puerto: Int) {
        val carta = BarLanCliente.carta(host, puerto) ?: return
        val existentes = db.productoDao().getAllIncluyendoOcultos()
        val reconstruir = CartaSync.debeReconstruir(
            schemaRemoto = carta.schema,
            schemaGuardado = store.cartaSchema,
            existentes = existentes,
            remotos = carta.productos,
        )
        val plan = if (reconstruir) {
            CartaSync.planReconstruccion(existentes, carta.productos)
        } else {
            CartaSync.plan(existentes, carta.productos)
        }
        if (plan.insertar.isEmpty() && plan.actualizar.isEmpty()) {
            if (reconstruir) store.cartaSchema = carta.schema
            return
        }
        db.withTransaction {
            if (plan.insertar.isNotEmpty()) db.productoDao().insertAll(plan.insertar)
            if (plan.actualizar.isNotEmpty()) db.productoDao().updateAll(plan.actualizar)
        }
        if (reconstruir) store.cartaSchema = carta.schema
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

    /** Nodos que ya fueron Bar en esta visita a Resumen (para rojo si caen). */
    private val nombresLan = java.util.concurrent.ConcurrentHashMap<String, String>()
    @Volatile
    private var nodosLan: List<ServidorDescubierto> = emptyList()

    fun limpiarScanLan() {
        nodosLan = emptyList()
        nombresLan.clear()
    }

    /**
     * Radar de la Wi‑Fi actual. No liga si Bar no admite.
     * [extras] son hosts de beacon UDP; `10.0.2.2` se añade siempre (emulador).
     * Un puerto 8787 abierto que no responde health de Bar no se pinta (evita el «Local» fantasma).
     */
    suspend fun sondearLan(
        extras: List<ServidorDescubierto> = emptyList(),
        escanearSubred: Boolean = true,
    ): List<LanLocalUi> = withContext(Dispatchers.IO) {
        val actual = _modo.value
        val qr = actual.qr
        if (actual is ModoSesion.Local || qr == null) return@withContext emptyList()
        val scan = if (escanearSubred) {
            EscaneadorRed.escanear(listOf(BarLanCliente.PUERTO))
        } else {
            emptyList()
        }
        val extrasTodos = extras + ServidorDescubierto(EMULADOR_BAR_HOST, BarLanCliente.PUERTO) + nodosLan
        val candidatos = candidatosLan(scan, extrasTodos)
        val vigente = actual as? ModoSesion.Establecimiento
        val locales = candidatos.mapNotNull { s ->
            val clave = "${s.ip}:${s.puerto}"
            val health = BarLanCliente.health(s.ip, s.puerto)
            val nombre = nombreLanVisible(health?.establecimiento)
            if (!BarLanCliente.esBar(health)) {
                val conocido = nombresLan.containsKey(clave)
                val aspecto = aspectoSondeo(conocido, errorConexion = true, admitido = false, jornada = false)
                    ?: return@mapNotNull null
                return@mapNotNull LanLocalUi(s.ip, s.puerto, nombresLan[clave].orEmpty(), aspecto)
            }
            val visible = nombre.ifEmpty { nombresLan[clave].orEmpty() }
            nombresLan[clave] = visible
            val jornada = vigente != null &&
                vigente.sesionTrabajo &&
                mismosNodo(s.ip, s.puerto, vigente)
            if (jornada) {
                return@mapNotNull LanLocalUi(
                    s.ip,
                    s.puerto,
                    visible.ifEmpty { vigente.etiquetaLocal() },
                    LanLocalAspecto.VERDE,
                )
            }
            val sesion = BarLanCliente.postSesion(s.ip, s.puerto, qr)
            val admitido = sesion?.admitido == true
            LanLocalUi(s.ip, s.puerto, visible, aspectoLan(false, admitido, false))
        }
        nodosLan = locales.map { ServidorDescubierto(it.host, it.puerto) }
        locales
    }

    /** Prefs antiguas: ligado sin lista blanca no debe candar carta ni mapa. */
    private fun soltarSiNoAdmitido() {
        val vigente = _modo.value as? ModoSesion.Establecimiento ?: return
        if (!vigente.admitido && !vigente.sesionTrabajo) desconectarBar()
    }

    private fun persistir(
        perfil: PerfilCamarero,
        qr: String?,
        token: String,
        fichaUrl: String? = _modo.value.fichaUrl,
    ) {
        val canonica = normalizarFichaUrl(fichaUrl)
        val actual = _modo.value
        if (actual is ModoSesion.Establecimiento) {
            val establecimiento = actual.copy(perfil = perfil, qr = qr, token = token, fichaUrl = canonica)
            store.guardarEstablecimiento(establecimiento)
            _modo.value = establecimiento
        } else {
            store.guardarIdentidad(perfil, qr, token, canonica)
            _modo.value = ModoSesion.Identidad(perfil, qr, token, canonica)
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

    suspend fun actualizarVisibilidadEstablecimientos(
        visible: VisibleOtrosEstablecimientos,
    ): IdentityRespuesta<PerfilCamarero> {
        val modo = _modo.value
        val token = modo.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        val perfilActual = modo.perfil ?: return IdentityRespuesta(false, error = "Sin sesión")
        if (perfilActual.visibleDirectorio == visible) {
            return IdentityRespuesta(true, perfilActual)
        }
        val qr = modo.qr
        val ficha = modo.fichaUrl
        return withContext(Dispatchers.IO) {
            persistir(perfilActual.copy(visibleOtrosEstablecimientos = visible), qr, token, ficha)
            val r = cliente().actualizarVisibilidadEstablecimientos(token, visible)
            if (r.ok && r.valor != null) {
                persistir(r.valor, qr, token, ficha)
            } else {
                persistir(perfilActual, qr, token, ficha)
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

    /**
     * Bandeja del Server (todas las invitaciones del email).
     * Si falla la red se conserva lo último en memoria.
     */
    suspend fun cargarInvitaciones(): IdentityRespuesta<List<InvitacionCamarero>> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            val r = cliente().meInvitaciones(token)
            if (r.ok && r.valor != null) {
                _invitaciones.value = r.valor
            }
            r
        }
    }

    suspend fun aceptarInvitacion(id: String): IdentityRespuesta<Unit> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            val r = cliente().aceptarInvitacion(token, id)
            if (r.ok) {
                refrescarMembresias(token)
                val lista = cliente().meInvitaciones(token)
                if (lista.ok && lista.valor != null) {
                    _invitaciones.value = lista.valor
                }
            }
            r
        }
    }

    suspend fun rechazarInvitacion(id: String): IdentityRespuesta<Unit> {
        val token = _modo.value.token ?: return IdentityRespuesta(false, error = "Sin sesión")
        return withContext(Dispatchers.IO) {
            val r = cliente().rechazarInvitacion(token, id)
            if (r.ok) {
                val lista = cliente().meInvitaciones(token)
                if (lista.ok && lista.valor != null) {
                    _invitaciones.value = lista.valor
                }
            }
            r
        }
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
