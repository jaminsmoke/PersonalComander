package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonParser

/**
 * Beacon UDP de Bar (`phbar1`). Puerto distinto de HTTP 8787.
 * Confirmar siempre con GET /health; no pintar el host.
 */
object PresenciaLan {
    const val PUERTO: Int = 8788
    const val MAGIC: String = "phbar1"
    const val HEARTBEAT_MS: Long = 2_000L
    const val TTL_MS: Long = 6_000L

    data class Anuncio(
        val establecimiento: String,
        val puertoHttp: Int = BarLanCliente.PUERTO,
        val activo: Boolean,
    )

    fun encode(anuncio: Anuncio): String {
        val nombre = anuncio.establecimiento.replace("\\", "\\\\").replace("\"", "\\\"")
        return "{" +
            "\"ph\":\"$MAGIC\"," +
            "\"role\":\"bar\"," +
            "\"establecimiento\":\"$nombre\"," +
            "\"puerto\":${anuncio.puertoHttp}," +
            "\"activo\":${anuncio.activo}" +
            "}"
    }

    fun decode(json: String): Anuncio? = try {
        val o = JsonParser.parseString(json).asJsonObject
        if (o.get("ph")?.asString != MAGIC) return null
        if (!o.get("role")?.asString.equals("bar", ignoreCase = true)) return null
        if (!o.has("activo") || o.get("activo").isJsonNull) return null
        val puerto = o.get("puerto")?.asInt ?: BarLanCliente.PUERTO
        if (puerto !in 1..65535) return null
        Anuncio(
            establecimiento = o.get("establecimiento")?.asString?.trim().orEmpty(),
            puertoHttp = puerto,
            activo = o.get("activo").asBoolean,
        )
    } catch (_: Exception) {
        null
    }
}
