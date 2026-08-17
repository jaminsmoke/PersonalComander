package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Parseo puro del contrato Identity `/v1`. Sin red. */
object IdentityJson {

    const val CODE_CREDENTIAL_REVOKED = "identity.credential_revoked"
    const val CODE_PASSWORD_INCORRECTA = "identity.password_incorrecta"
    const val CODE_FOTO_INVALIDA = "identity.foto_invalida"
    const val CODE_FOTO_INEXISTENTE = "identity.foto_inexistente"
    const val CODE_TOKEN_INVALIDO = "identity.token_invalido"

    data class SesionIdentity(
        val token: String?,
        val perfil: PerfilCamarero,
        val qr: String,
        val fichaUrl: String? = null,
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
        )
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
        )
    }

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

    fun contrastarHealth(
        nombreHealth: String?,
        membresias: List<MembresiaEstablecimiento>,
    ): ContrasteMembresia {
        if (membresias.isEmpty() || nombreHealth.isNullOrBlank()) return ContrasteMembresia.SinDatos
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
