package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object BarLanCliente {
    const val PUERTO: Int = 8787

    data class Health(
        val ok: Boolean,
        val role: String,
        val sala: String? = null,
        val version: String? = null,
    )

    fun health(host: String, puerto: Int = PUERTO): Health? {
        val conexion = URL("http://$host:$puerto/health").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 2500
            conexion.readTimeout = 4000
            conexion.requestMethod = "GET"
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) return null
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            parseHealth(texto)
        } catch (_: IOException) {
            null
        } finally {
            conexion.disconnect()
        }
    }

    fun parseHealth(json: String): Health? = try {
        val o = JsonParser.parseString(json).asJsonObject
        val role = o.get("role")?.asString ?: return null
        Health(
            ok = o.get("ok")?.asBoolean == true,
            role = role,
            sala = o.get("sala")?.takeUnless { it.isJsonNull }?.asString,
            version = o.get("version")?.takeUnless { it.isJsonNull }?.asString,
        )
    } catch (_: Exception) {
        null
    }

    fun esBar(health: Health?): Boolean =
        health != null && health.ok && health.role.equals("bar", ignoreCase = true)
}
