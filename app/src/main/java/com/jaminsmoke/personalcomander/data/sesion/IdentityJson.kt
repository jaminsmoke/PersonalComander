package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonElement
import com.google.gson.JsonParser

/** Parseo puro del contrato Identity `/v1`. Sin red. */
object IdentityJson {

    data class SesionIdentity(
        val token: String?,
        val perfil: PerfilCamarero,
        val qr: String,
    )

    fun parseErrorDetail(body: String): String {
        return try {
            val root = JsonParser.parseString(body).asJsonObject
            val detail = root.get("detail") ?: return body.take(200)
            when {
                detail.isJsonPrimitive -> detail.asString
                detail.isJsonArray -> detail.asJsonArray.joinToString(" ") { el ->
                    if (el.isJsonPrimitive) el.asString else el.toString()
                }
                else -> detail.toString()
            }
        } catch (_: Exception) {
            body.take(200)
        }
    }

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
}
