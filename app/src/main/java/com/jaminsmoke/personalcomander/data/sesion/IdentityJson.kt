package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.Instant
import java.time.OffsetDateTime

/** Parseo puro del contrato Identity `/v1`. Sin red. */
object IdentityJson {

    const val CODE_CREDENTIAL_REVOKED = "identity.credential_revoked"
    const val CODE_CREDENCIALES_INVALIDAS = "identity.credenciales_invalidas"
    const val CODE_PASSWORD_INCORRECTA = "identity.password_incorrecta"
    const val CODE_FOTO_INVALIDA = "identity.foto_invalida"
    const val CODE_FOTO_INEXISTENTE = "identity.foto_inexistente"
    const val CODE_TOKEN_INVALIDO = "identity.token_invalido"
    const val CODE_JORNADA_YA_ABIERTA = "identity.jornada_ya_abierta"
    const val CODE_JORNADA_NO_ABIERTA = "identity.jornada_no_abierta"
    const val CODE_INVITACION_USADA = "identity.invitacion_ya_usada"
    const val CODE_INVITACION_EXPIRADA = "identity.invitacion_expirada"

    data class SesionIdentity(
        val token: String?,
        val perfil: PerfilCamarero,
        val qr: String,
        val fichaUrl: String? = null,
        /** Refresh opaco rotado de la sesión revocable. Null en JWT legacy. */
        val refreshToken: String? = null,
        /** UUID de sesión (`POST /v1/auth/login`). Null en JWT legacy. */
        val sesionId: String? = null,
        /** Vida del access en segundos. Null en JWT legacy. */
        val expiresInSegundos: Long? = null,
    )

    data class SesionRenovada(
        val token: String,
        val refreshToken: String,
        val sesionId: String? = null,
        val expiresInSegundos: Long? = null,
    )

    data class IdentityError(
        val detail: String,
        val code: String? = null,
    )

    data class RegistroIdentity(
        val id: String,
        val qr: String,
        val dataOrigin: DataOrigin,
        val fichaUrl: String? = null,
    )

    data class QrIdentity(
        val qr: String,
        val fichaUrl: String? = null,
    )

    fun parseError(body: String): IdentityError {
        return try {
            val root = JsonParser.parseString(body).asJsonObject
            val detailEl = root.get("detail")
            val detail = when {
                detailEl == null -> body.take(200)
                detailEl.isJsonPrimitive -> detailEl.asString
                detailEl.isJsonArray -> detailEl.asJsonArray.joinToString(" ") { el ->
                    if (el.isJsonPrimitive) el.asString else el.toString()
                }
                else -> detailEl.toString()
            }
            val code = root.get("code")?.takeUnless { it.isJsonNull }?.asString
            IdentityError(detail, code)
        } catch (_: Exception) {
            IdentityError(body.take(200))
        }
    }

    fun parseErrorDetail(body: String): String = parseError(body).detail

    fun parseRegistro(body: String): RegistroIdentity {
        val o = JsonParser.parseString(body).asJsonObject
        return RegistroIdentity(
            id = o.get("id").asString,
            qr = o.get("qr").asString,
            dataOrigin = parseDataOrigin(o),
            fichaUrl = textoOpcional(o, "ficha_url"),
        )
    }

    fun parsePerfil(el: JsonElement): PerfilCamarero {
        val o = el.asJsonObject
        return PerfilCamarero(
            id = o.get("id").asString,
            nombre = o.get("nombre").asString,
            apellidos = o.get("apellidos").asString,
            email = o.get("email").asString,
            telefono = o.get("telefono")?.takeUnless { it.isJsonNull }?.asString,
            fotoUrl = o.get("foto_url")?.takeUnless { it.isJsonNull }?.asString,
            nick = o.get("nick")?.takeUnless { it.isJsonNull }?.asString?.trim()?.ifBlank { null },
            direccion = textoOpcional(o, "direccion"),
            ciudad = textoOpcional(o, "ciudad"),
            dataOrigin = parseDataOrigin(o),
            visibleOtrosEstablecimientos = parseVisibleOtrosEstablecimientos(o),
        )
    }

    fun parseVisibleOtrosEstablecimientos(o: JsonObject): VisibleOtrosEstablecimientos {
        val el = o.get("visible_otros_establecimientos")
        val raw = el?.takeUnless { it.isJsonNull }?.takeIf { it.isJsonPrimitive }?.asString
        return VisibleOtrosEstablecimientos.fromWire(raw)
    }

    fun parseDataOrigin(o: JsonObject): DataOrigin {
        val el = o.get("data_origin")
        val raw = el?.takeUnless { it.isJsonNull }?.takeIf { it.isJsonPrimitive }?.asString
        return DataOrigin.fromWire(raw)
    }

    fun cuerpoRegistro(
        nombre: String,
        apellidos: String,
        email: String,
        password: String,
        nick: String,
        telefono: String? = null,
        origin: DataOrigin = DataOrigin.Real,
    ): String {
        val o = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("apellidos", apellidos)
            addProperty("email", email)
            addProperty("password", password)
            addProperty("nick", nick)
            addProperty("data_origin", origin.wire)
            if (!telefono.isNullOrBlank()) addProperty("telefono", telefono)
        }
        return o.toString()
    }

    fun parseLogin(body: String): SesionIdentity {
        val o = JsonParser.parseString(body).asJsonObject
        return SesionIdentity(
            token = o.get("token").asString,
            perfil = parsePerfil(o.get("camarero")),
            qr = o.get("qr").asString,
            fichaUrl = textoOpcional(o, "ficha_url"),
            refreshToken = textoOpcional(o, "refresh_token"),
            sesionId = textoOpcional(o, "sesion_id"),
            expiresInSegundos = longOpcional(o, "expires_in"),
        )
    }

    /** `POST /v1/auth/refresh`. `null` si el cuerpo no trae el par rotado completo. */
    fun parseRefreshOrNull(body: String): SesionRenovada? = try {
        val o = JsonParser.parseString(body).asJsonObject
        val token = o.get("token")?.takeUnless { it.isJsonNull }?.asString ?: return null
        val refresh = o.get("refresh_token")?.takeUnless { it.isJsonNull }?.asString ?: return null
        SesionRenovada(
            token = token,
            refreshToken = refresh,
            sesionId = textoOpcional(o, "sesion_id"),
            expiresInSegundos = longOpcional(o, "expires_in"),
        )
    } catch (_: Exception) {
        null
    }

    fun longOpcional(o: JsonObject, clave: String): Long? {
        val el = o.get(clave) ?: return null
        if (el.isJsonNull || !el.isJsonPrimitive) return null
        return try {
            el.asLong
        } catch (_: Exception) {
            null
        }
    }

    /** Momento de caducidad del access (`expires_in` segundos desde [ahora]). */
    fun expiraEnDe(expiresInSegundos: Long?, ahora: Long = System.currentTimeMillis()): Long? {
        val segundos = expiresInSegundos ?: return null
        if (segundos <= 0) return null
        return ahora + segundos * 1000L
    }

    /** Falta `margenMs` (60 s por defecto) o ya caducó. Sin expiración conocida → no renovar. */
    fun debeRenovar(expiraEn: Long?, ahora: Long = System.currentTimeMillis(), margenMs: Long = 60_000L): Boolean {
        val exp = expiraEn ?: return false
        return ahora + margenMs >= exp
    }

    fun parseLoginOrNull(body: String): SesionIdentity? = try {
        parseLogin(body)
    } catch (_: Exception) {
        null
    }

    /** Recorta correo y contraseña (pegado desde .env suele traer salto de línea). */
    fun normalizarCredenciales(email: String, password: String): Pair<String, String> =
        email.trim() to password.trim()

    fun parseQr(body: String): QrIdentity {
        val o = JsonParser.parseString(body).asJsonObject
        return QrIdentity(
            qr = o.get("qr").asString,
            fichaUrl = textoOpcional(o, "ficha_url"),
        )
    }

    fun textoOpcional(o: JsonObject, clave: String): String? {
        val el = o.get(clave) ?: return null
        if (el.isJsonNull || !el.isJsonPrimitive) return null
        return el.asString.trim().takeIf { it.isNotEmpty() }
    }

    fun booleanODefault(o: JsonObject, clave: String, defecto: Boolean): Boolean {
        val el = o.get(clave) ?: return defecto
        if (el.isJsonNull || !el.isJsonPrimitive || !el.asJsonPrimitive.isBoolean) return defecto
        return el.asBoolean
    }

    fun parseVisibilidad(body: String): VisibilidadCamarero {
        val o = JsonParser.parseString(body).asJsonObject
        return VisibilidadCamarero(
            nombre = booleanODefault(o, "nombre", true),
            apellidos = booleanODefault(o, "apellidos", true),
            nick = booleanODefault(o, "nick", true),
            email = booleanODefault(o, "email", false),
            telefono = booleanODefault(o, "telefono", false),
            direccion = booleanODefault(o, "direccion", false),
            ciudad = booleanODefault(o, "ciudad", false),
            foto = booleanODefault(o, "foto", false),
        )
    }

    fun cuerpoVisibilidad(campo: CampoVisibilidad, valor: Boolean): String =
        JsonObject().apply { addProperty(campo.wire, valor) }.toString()

    fun cuerpoVisibilidadEstablecimientos(visible: VisibleOtrosEstablecimientos): String =
        JsonObject().apply { addProperty("visible", visible.wire) }.toString()

    fun cuerpoVisibilidadCompleto(v: VisibilidadCamarero): String = JsonObject().apply {
        addProperty("nombre", v.nombre)
        addProperty("apellidos", v.apellidos)
        addProperty("nick", v.nick)
        addProperty("email", v.email)
        addProperty("telefono", v.telefono)
        addProperty("direccion", v.direccion)
        addProperty("ciudad", v.ciudad)
        addProperty("foto", v.foto)
    }.toString()

    /**
     * PATCH `/v1/camareros/me`. Identity exige al menos un campo.
     * [incluirDireccion]/[incluirCiudad] permiten mandar vacío para borrar.
     */
    fun cuerpoPerfilPatch(
        nick: String? = null,
        direccion: String? = null,
        ciudad: String? = null,
        incluirDireccion: Boolean = false,
        incluirCiudad: Boolean = false,
    ): String {
        val o = JsonObject()
        if (nick != null) o.addProperty("nick", nick)
        if (incluirDireccion) o.addProperty("direccion", direccion.orEmpty())
        if (incluirCiudad) o.addProperty("ciudad", ciudad.orEmpty())
        return o.toString()
    }

    fun cuerpoCambioPassword(passwordActual: String, passwordNueva: String): String =
        JsonObject().apply {
            addProperty("password_actual", passwordActual)
            addProperty("password_nueva", passwordNueva)
        }.toString()

    fun cuerpoIniciarJornada(establecimientoId: String): String =
        JsonObject().apply { addProperty("establecimiento_id", establecimientoId) }.toString()

    fun parseInstantIso(raw: String): Instant? {
        val texto = raw.trim().takeIf { it.isNotEmpty() } ?: return null
        return try {
            Instant.parse(texto)
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(texto).toInstant()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun parseJornada(body: String): JornadaOficio? = parseJornadaEl(
        try {
            JsonParser.parseString(body).takeIf { it.isJsonObject }?.asJsonObject
        } catch (_: Exception) {
            null
        },
    )

    fun parseJornadas(body: String): List<JornadaOficio>? {
        return try {
            val el = JsonParser.parseString(body)
            if (!el.isJsonArray) return null
            el.asJsonArray.mapNotNull { item ->
                parseJornadaEl(item.takeIf { it.isJsonObject }?.asJsonObject)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun parseResumenOficio(body: String): ResumenOficio? {
        return try {
            val o = JsonParser.parseString(body).asJsonObject
            val desde = parseInstantIso(o.get("desde")?.asString.orEmpty()) ?: return null
            val hasta = parseInstantIso(o.get("hasta")?.asString.orEmpty()) ?: return null
            val por = o.get("por_establecimiento")
                ?.takeIf { it.isJsonArray }
                ?.asJsonArray
                ?.mapNotNull { item ->
                    val est = item.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                    val id = est.get("establecimiento_id")?.takeUnless { it.isJsonNull }?.asString
                        ?: return@mapNotNull null
                    ResumenOficioEstablecimiento(
                        establecimientoId = id,
                        horasSegundos = est.get("horas_segundos")?.asInt ?: 0,
                        rondasServidas = est.get("mesas_servidas")?.asInt ?: 0,
                    )
                }
                .orEmpty()
            ResumenOficio(
                desde = desde,
                hasta = hasta,
                horasSegundos = o.get("horas_segundos")?.asInt ?: 0,
                rondasServidas = o.get("mesas_servidas")?.asInt ?: 0,
                porEstablecimiento = por,
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * UUID de membresía para el libro de oficio.
     * Si [healthId] viene, solo cuenta un `membresias.id` igual (sin caer al nombre).
     * Si falta, match único por nombre como [contrastarHealth].
     */
    fun establecimientoIdPorHealth(
        nombreHealth: String?,
        membresias: List<MembresiaEstablecimiento>,
        healthId: String? = null,
    ): String? {
        val id = healthId?.trim()?.takeIf { it.isNotEmpty() }
        if (id != null) {
            return membresias.firstOrNull { it.id.equals(id, ignoreCase = true) }?.id
        }
        if (nombreHealth.isNullOrBlank()) return null
        val needle = nombreHealth.trim()
        val coinciden = membresias.filter { it.nombre.trim().equals(needle, ignoreCase = true) }
        return coinciden.singleOrNull()?.id
    }

    private fun parseJornadaEl(o: JsonObject?): JornadaOficio? {
        if (o == null) return null
        val id = o.get("id")?.takeUnless { it.isJsonNull }?.asString ?: return null
        val camareroId = o.get("camarero_id")?.takeUnless { it.isJsonNull }?.asString ?: return null
        val establecimientoId = o.get("establecimiento_id")?.takeUnless { it.isJsonNull }?.asString
            ?: return null
        val inicio = parseInstantIso(o.get("inicio")?.asString.orEmpty()) ?: return null
        val finRaw = o.get("fin")?.takeUnless { it.isJsonNull }?.asString
        val fin = finRaw?.let { parseInstantIso(it) }
        if (finRaw != null && fin == null) return null
        return JornadaOficio(
            id = id,
            camareroId = camareroId,
            establecimientoId = establecimientoId,
            inicio = inicio,
            fin = fin,
        )
    }

    fun parseFotoUrl(body: String): String? {
        val el = JsonParser.parseString(body).asJsonObject.get("foto_url")
        return el?.takeUnless { it.isJsonNull }?.asString
    }

    /**
     * `GET /v1/camareros/me/establecimientos`. `null` si el cuerpo no es un array JSON
     * (no pisar la cache local). Lista vacía = sin membresías en Identity.
     */
    fun parseEstablecimientos(body: String): List<MembresiaEstablecimiento>? {
        return try {
            val el = JsonParser.parseString(body)
            if (!el.isJsonArray) return null
            el.asJsonArray.mapNotNull { item ->
                val o = item.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val id = o.get("id")?.takeUnless { it.isJsonNull }?.asString ?: return@mapNotNull null
                val nombre = o.get("nombre")?.takeUnless { it.isJsonNull }?.asString ?: return@mapNotNull null
                val cuenta = o.get("cuenta_negocio_id")?.takeUnless { it.isJsonNull }?.asString
                    ?: return@mapNotNull null
                val rol = o.get("rol")?.takeUnless { it.isJsonNull }?.asString ?: return@mapNotNull null
                MembresiaEstablecimiento(
                    id = id,
                    nombre = nombre,
                    cuentaNegocioId = cuenta,
                    rol = rol,
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * `GET /v1/camareros/me/invitaciones`. `null` si el cuerpo no es un array JSON
     * (no pisar la lista en memoria). Lista vacía = sin invitaciones.
     */
    fun parseInvitaciones(body: String): List<InvitacionCamarero>? {
        return try {
            val el = JsonParser.parseString(body)
            if (!el.isJsonArray) return null
            el.asJsonArray.mapNotNull { item ->
                val o = item.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val id = o.get("id")?.takeUnless { it.isJsonNull }?.asString ?: return@mapNotNull null
                val establecimientoId = o.get("establecimiento_id")?.takeUnless { it.isJsonNull }?.asString
                    ?: return@mapNotNull null
                val nombre = o.get("establecimiento_nombre")?.takeUnless { it.isJsonNull }?.asString
                    ?: return@mapNotNull null
                val rol = o.get("rol")?.takeUnless { it.isJsonNull }?.asString ?: return@mapNotNull null
                val estado = o.get("estado")?.takeUnless { it.isJsonNull }?.asString
                    ?: return@mapNotNull null
                val expiraEn = o.get("expira_en")?.takeUnless { it.isJsonNull }?.asString
                    ?: return@mapNotNull null
                val creadaEn = o.get("creada_en")?.takeUnless { it.isJsonNull }?.asString
                    ?: return@mapNotNull null
                InvitacionCamarero(
                    id = id,
                    establecimientoId = establecimientoId,
                    establecimientoNombre = nombre,
                    rol = rol,
                    estado = estado,
                    expiraEn = expiraEn,
                    creadaEn = creadaEn,
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun contrastarHealth(
        nombreHealth: String?,
        membresias: List<MembresiaEstablecimiento>,
        healthId: String? = null,
    ): ContrasteMembresia {
        if (membresias.isEmpty()) return ContrasteMembresia.SinDatos
        val id = healthId?.trim()?.takeIf { it.isNotEmpty() }
        if (id != null) {
            return if (membresias.any { it.id.equals(id, ignoreCase = true) }) {
                ContrasteMembresia.Coincide
            } else {
                ContrasteMembresia.NoCoincide
            }
        }
        if (nombreHealth.isNullOrBlank()) return ContrasteMembresia.SinDatos
        val needle = nombreHealth.trim()
        return if (membresias.any { it.nombre.trim().equals(needle, ignoreCase = true) }) {
            ContrasteMembresia.Coincide
        } else {
            ContrasteMembresia.NoCoincide
        }
    }
}

enum class CampoVisibilidad(val wire: String) {
    NOMBRE("nombre"),
    APELLIDOS("apellidos"),
    NICK("nick"),
    EMAIL("email"),
    TELEFONO("telefono"),
    DIRECCION("direccion"),
    CIUDAD("ciudad"),
    FOTO("foto"),
}

/** Preferencias públicas de ficha. Mirror de Identity; no se inventan campos. */
data class VisibilidadCamarero(
    val nombre: Boolean = true,
    val apellidos: Boolean = true,
    val nick: Boolean = true,
    val email: Boolean = false,
    val telefono: Boolean = false,
    val direccion: Boolean = false,
    val ciudad: Boolean = false,
    val foto: Boolean = false,
) {
    fun valor(campo: CampoVisibilidad): Boolean = when (campo) {
        CampoVisibilidad.NOMBRE -> nombre
        CampoVisibilidad.APELLIDOS -> apellidos
        CampoVisibilidad.NICK -> nick
        CampoVisibilidad.EMAIL -> email
        CampoVisibilidad.TELEFONO -> telefono
        CampoVisibilidad.DIRECCION -> direccion
        CampoVisibilidad.CIUDAD -> ciudad
        CampoVisibilidad.FOTO -> foto
    }

    fun con(campo: CampoVisibilidad, valor: Boolean): VisibilidadCamarero = when (campo) {
        CampoVisibilidad.NOMBRE -> copy(nombre = valor)
        CampoVisibilidad.APELLIDOS -> copy(apellidos = valor)
        CampoVisibilidad.NICK -> copy(nick = valor)
        CampoVisibilidad.EMAIL -> copy(email = valor)
        CampoVisibilidad.TELEFONO -> copy(telefono = valor)
        CampoVisibilidad.DIRECCION -> copy(direccion = valor)
        CampoVisibilidad.CIUDAD -> copy(ciudad = valor)
        CampoVisibilidad.FOTO -> copy(foto = valor)
    }

    companion object {
        val DEFAULT = VisibilidadCamarero()
    }
}

data class MembresiaEstablecimiento(
    val id: String,
    val nombre: String,
    val cuentaNegocioId: String,
    val rol: String,
)

data class InvitacionCamarero(
    val id: String,
    val establecimientoId: String,
    val establecimientoNombre: String,
    val rol: String,
    val estado: String,
    val expiraEn: String,
    val creadaEn: String,
) {
    val esPendiente: Boolean get() = estado.equals("pendiente", ignoreCase = true)
}

/** Estado laboral según membresías Identity. Misma regla que Bar: libre = ninguna activa. */
sealed class EstadoLaboral {
    data object Libre : EstadoLaboral()
    data class Trabajador(val nombres: List<String>) : EstadoLaboral()
}

fun estadoLaboral(membresias: List<MembresiaEstablecimiento>): EstadoLaboral {
    val nombres = membresias.map { it.nombre.trim() }.filter { it.isNotEmpty() }
    return if (nombres.isEmpty()) EstadoLaboral.Libre else EstadoLaboral.Trabajador(nombres)
}

enum class ContrasteMembresia {
    SinDatos,
    Coincide,
    NoCoincide,
}

data class ConectarBarResult(
    val ok: Boolean,
    val contraste: ContrasteMembresia = ContrasteMembresia.SinDatos,
    val nombreBar: String? = null,
    val admitido: Boolean = false,
)
