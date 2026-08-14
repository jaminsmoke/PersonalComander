package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object BarLanCliente {
    const val PUERTO: Int = 8787

    object Rutas {
        const val HEALTH = "/health"
        const val RONDAS = "/v1/rondas"
        const val ESTADO = "/v1/estado"
        const val EVENTOS = "/v1/eventos"
        const val CARTA = "/v1/carta"

        fun todas(): List<String> = listOf(HEALTH, RONDAS, ESTADO, EVENTOS, CARTA)
    }

    data class Health(
        val ok: Boolean,
        val role: String,
        val establecimiento: String? = null,
        val version: String? = null,
    )

    fun health(host: String, puerto: Int = PUERTO): Health? {
        val conexion = URL("http://$host:$puerto${Rutas.HEALTH}").openConnection() as HttpURLConnection
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

    data class PostRondaResult(
        val ok: Boolean,
        val codigo: Int,
        val tickets: List<TicketLan> = emptyList(),
    )

    /** `POST /v1/rondas`. 200 (idempotente) y 201 cuentan como ok. Sin auth en Bar 0.1. */
    fun postRonda(host: String, puerto: Int, ronda: RondaLan): PostRondaResult {
        val conexion = URL("http://$host:$puerto${Rutas.RONDAS}").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 2500
            conexion.readTimeout = 4000
            conexion.requestMethod = "POST"
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val cuerpo = RondaLanMapper.toJson(ronda).toByteArray(Charsets.UTF_8)
            conexion.outputStream.use { it.write(cuerpo) }
            val codigo = conexion.responseCode
            val ok = codigo in 200..299
            val texto = (if (ok) conexion.inputStream else conexion.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                .orEmpty()
            PostRondaResult(
                ok = ok,
                codigo = codigo,
                tickets = if (ok) RecogerLogica.parseTickets(texto) else emptyList(),
            )
        } catch (_: IOException) {
            PostRondaResult(ok = false, codigo = 0)
        } finally {
            conexion.disconnect()
        }
    }

    /** Snapshot de colas. Bar no persiste SSE: al reconectar hay que realinear con esto. */
    fun estado(host: String, puerto: Int = PUERTO): EstadoLan? {
        val conexion = URL("http://$host:$puerto${Rutas.ESTADO}").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 2500
            conexion.readTimeout = 4000
            conexion.requestMethod = "GET"
            if (conexion.responseCode !in 200..299) return null
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            RecogerLogica.parseEstado(texto)
        } catch (_: IOException) {
            null
        } finally {
            conexion.disconnect()
        }
    }

    /** Catálogo canónico del nodo. 404 o red caída → null (el ligue no debe fallar). */
    fun carta(host: String, puerto: Int = PUERTO): CartaLan? {
        val conexion = URL("http://$host:$puerto${Rutas.CARTA}").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 2500
            conexion.readTimeout = 4000
            conexion.requestMethod = "GET"
            if (conexion.responseCode !in 200..299) return null
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            CartaSync.parse(texto)
        } catch (_: IOException) {
            null
        } finally {
            conexion.disconnect()
        }
    }

    fun abrirSse(host: String, puerto: Int): HttpURLConnection {
        val conexion = URL("http://$host:$puerto${Rutas.EVENTOS}").openConnection() as HttpURLConnection
        conexion.connectTimeout = 4000
        conexion.readTimeout = 0
        conexion.requestMethod = "GET"
        conexion.setRequestProperty("Accept", "text/event-stream")
        conexion.setRequestProperty("Cache-Control", "no-cache")
        return conexion
    }

    fun leerSseAbierto(
        conexion: HttpURLConnection,
        debeParar: () -> Boolean,
        onEvento: (SalaEventLan) -> Unit,
    ) {
        try {
            if (conexion.responseCode !in 200..299) return
            val reader = conexion.inputStream.bufferedReader()
            var eventType: String? = null
            val data = StringBuilder()
            while (!debeParar() && !Thread.currentThread().isInterrupted) {
                val line = reader.readLine() ?: break
                val (nuevoTipo, evento) = RecogerLogica.alimentarSse(eventType, data, line)
                eventType = nuevoTipo
                if (evento != null) onEvento(evento)
            }
        } catch (_: IOException) {
            // reconexión la gestiona RecogerServicio
        }
    }
}
