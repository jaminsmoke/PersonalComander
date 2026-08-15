package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonElement
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
    )

    data class IdentityError(
        val detail: String,
        val code: String? = null,
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

    fun parseRegistro(body: String): Pair<String, String> {
        val o = JsonParser.parseString(body).asJsonObject
        return o.get("id").asString to o.get("qr").asString
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
        )
    }

    fun parseLogin(body: String): SesionIdentity {
        val o = JsonParser.parseString(body).asJsonObject
        return SesionIdentity(
            token = o.get("token").asString,
            perfil = parsePerfil(o.get("camarero")),
            qr = o.get("qr").asString,
        )
    }

    fun parseQr(body: String): String =
        JsonParser.parseString(body).asJsonObject.get("qr").asString

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
