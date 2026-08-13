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
}
