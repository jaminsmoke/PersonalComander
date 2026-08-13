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
        val establecimiento: String? = null,
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
            establecimiento = o.get("establecimiento")?.takeUnless { it.isJsonNull }?.asString
                ?: o.get("sala")?.takeUnless { it.isJsonNull }?.asString,
            version = o.get("version")?.takeUnless { it.isJsonNull }?.asString,
        )
    } catch (_: Exception) {
        null
    }

    fun esBar(health: Health?): Boolean =
        health != null && health.ok && health.role.equals("bar", ignoreCase = true)

    data class PostRondaResult(val ok: Boolean, val codigo: Int)

    /** `POST /v1/rondas`. 200 (idempotente) y 201 cuentan como ok. Sin auth en Bar 0.1. */
    fun postRonda(host: String, puerto: Int, ronda: RondaLan): PostRondaResult {
        val conexion = URL("http://$host:$puerto/v1/rondas").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 2500
            conexion.readTimeout = 4000
            conexion.requestMethod = "POST"
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val cuerpo = RondaLanMapper.toJson(ronda).toByteArray(Charsets.UTF_8)
            conexion.outputStream.use { it.write(cuerpo) }
            val codigo = conexion.responseCode
            PostRondaResult(ok = codigo in 200..299, codigo = codigo)
        } catch (_: IOException) {
            PostRondaResult(ok = false, codigo = 0)
        } finally {
            conexion.disconnect()
        }
    }
}
