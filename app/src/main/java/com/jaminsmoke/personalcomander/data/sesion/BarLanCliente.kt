package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object BarLanCliente {
    const val PUERTO: Int = 8787

    object Rutas {
        const val HEALTH = "/health"
        const val RONDAS = "/v1/rondas"
        const val SESION = "/v1/sesion"
        const val SESION_INICIAR = "/v1/sesion/iniciar"
        const val SESION_CORTAR = "/v1/sesion/cortar"
        const val HEARTBEAT = "/v1/heartbeat"
        const val ESTADO = "/v1/estado"
        const val EVENTOS = "/v1/eventos"
        const val CARTA = "/v1/carta"

        fun todas(): List<String> = listOf(
            HEALTH, RONDAS, SESION, SESION_INICIAR, SESION_CORTAR, HEARTBEAT, ESTADO, EVENTOS, CARTA,
        )
    }

    data class Health(
        val ok: Boolean,
        val role: String,
        val establecimiento: String? = null,
        /** UUID Identity; null si el nodo no está vinculado. */
        val establecimientoId: String? = null,
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
            establecimientoId = o.get("establecimiento_id")?.takeUnless { it.isJsonNull }?.asString
                ?.trim()?.takeIf { it.isNotEmpty() },
            version = o.get("version")?.takeUnless { it.isJsonNull }?.asString,
        )
    } catch (_: Exception) {
        null
    }

    fun esBar(health: Health?): Boolean =
        health != null && health.ok && health.role.equals("bar", ignoreCase = true)

    data class SesionLan(
        val admitido: Boolean = false,
        val camareroId: String? = null,
        val nombre: String? = null,
    )

    /**
     * `POST /v1/sesion` con el QR `phid1`. 404 o red → null (nodo viejo: ligue ok, no admitido).
     * 400 / no-2xx → null. No es un alta.
     */
    fun postSesion(host: String, puerto: Int, qr: String): SesionLan? {
        val conexion = URL("http://$host:$puerto${Rutas.SESION}").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 2500
            conexion.readTimeout = 4000
            conexion.requestMethod = "POST"
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val cuerpo = JsonObject().apply { addProperty("qr", qr) }.toString()
                .toByteArray(Charsets.UTF_8)
            conexion.outputStream.use { it.write(cuerpo) }
            val codigo = conexion.responseCode
            if (codigo !in 200..299) return null
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            parseSesion(texto)
        } catch (_: IOException) {
            null
        } finally {
            conexion.disconnect()
        }
    }

    fun parseSesion(json: String): SesionLan? = try {
        val o = JsonParser.parseString(json).asJsonObject
        if (!o.has("admitido") || o.get("admitido").isJsonNull) return null
        SesionLan(
            admitido = o.get("admitido").asBoolean,
            camareroId = o.get("camareroId")?.takeUnless { it.isJsonNull }?.asString,
            nombre = o.get("nombre")?.takeUnless { it.isJsonNull }?.asString,
        )
    } catch (_: Exception) {
        null
    }

    data class JornadaLanResult(
        val ok: Boolean,
        val codigo: Int,
        val sesionActiva: Boolean = false,
        val nodoViejo: Boolean = false,
        /** Token de sesión LAN emitido por Bar v0.2+. Null en Bar 0.1 o si el nodo no emite. */
        val token: String? = null,
    )

    fun postIniciar(host: String, puerto: Int, qr: String): JornadaLanResult {
        val (codigo, cuerpo) = postJson(host, puerto, Rutas.SESION_INICIAR, cuerpoQr(qr))
        return interpretarIniciar(codigo, cuerpo, token = parseToken(cuerpo))
    }

    /**
     * Bar v0.2 acepta QR o Bearer. Si hay token LAN se envía como auth;
     * el QR en el body sigue siendo necesario como identificador.
     */
    fun postCortar(host: String, puerto: Int, qr: String, tokenLan: String? = null): JornadaLanResult {
        val (codigo, _) = postJson(host, puerto, Rutas.SESION_CORTAR, cuerpoQr(qr), tokenLan)
        return JornadaLanResult(ok = codigo in 200..299 || codigo == 404, codigo = codigo)
    }

    /**
     * Bar v0.2 valida coherencia del token con el [camareroId] del body.
     * El body se mantiene durante la transición para compatibilidad con Bar 0.1.
     */
    fun postHeartbeat(host: String, puerto: Int, camareroId: String, tokenLan: String? = null): JornadaLanResult {
        val cuerpo = JsonObject().apply { addProperty("camareroId", camareroId) }.toString()
        val (codigo, _) = postJson(host, puerto, Rutas.HEARTBEAT, cuerpo, tokenLan)
        return JornadaLanResult(ok = codigo in 200..299, codigo = codigo)
    }

    fun cuerpoQr(qr: String): String =
        JsonObject().apply { addProperty("qr", qr) }.toString()

    fun parseSesionActiva(json: String): Boolean? = try {
        val o = JsonParser.parseString(json).asJsonObject
        val el = o.get("sesionActiva") ?: o.get("sesion_activa")
        if (el == null || el.isJsonNull || !el.isJsonPrimitive) null
        else if (el.asJsonPrimitive.isBoolean) el.asBoolean else null
    } catch (_: Exception) {
        null
    }

    /** Extrae el token de sesión LAN del JSON de respuesta de Bar v0.2. Null si ausente. */
    fun parseToken(json: String): String? = try {
        val o = JsonParser.parseString(json).asJsonObject
        o.get("token")?.takeUnless { it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }

    /** 404 = Bar 0.1 sin jornada: se trata como activa para no romper el envío. */
    fun interpretarIniciar(codigo: Int, cuerpo: String, token: String? = null): JornadaLanResult = when {
        codigo == 404 -> JornadaLanResult(ok = true, codigo = 404, sesionActiva = true, nodoViejo = true)
        codigo in 200..299 -> {
            val activa = parseSesionActiva(cuerpo) ?: true
            JornadaLanResult(ok = true, codigo = codigo, sesionActiva = activa, token = token)
        }
        else -> JornadaLanResult(ok = false, codigo = codigo)
    }

    data class PostRondaResult(
        val ok: Boolean,
        val codigo: Int,
        val tickets: List<TicketLan> = emptyList(),
    )

    /** `POST /v1/rondas`. 200 (idempotente) y 201 cuentan como ok. Bar v0.2 exige Bearer. */
    fun postRonda(host: String, puerto: Int, ronda: RondaLan, tokenLan: String? = null): PostRondaResult {
        val conexion = URL("http://$host:$puerto${Rutas.RONDAS}").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 2500
            conexion.readTimeout = 4000
            conexion.requestMethod = "POST"
            conexion.doOutput = true
            aplicarAuth(conexion, tokenLan)
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
    fun estado(host: String, puerto: Int = PUERTO, tokenLan: String? = null): EstadoLan? {
        val conexion = URL("http://$host:$puerto${Rutas.ESTADO}").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 2500
            conexion.readTimeout = 4000
            conexion.requestMethod = "GET"
            aplicarAuth(conexion, tokenLan)
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
    fun carta(host: String, puerto: Int = PUERTO, tokenLan: String? = null): CartaLan? {
        val conexion = URL("http://$host:$puerto${Rutas.CARTA}").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 2500
            conexion.readTimeout = 4000
            conexion.requestMethod = "GET"
            aplicarAuth(conexion, tokenLan)
            if (conexion.responseCode !in 200..299) return null
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            CartaSync.parse(texto)
        } catch (_: IOException) {
            null
        } finally {
            conexion.disconnect()
        }
    }

    /**
     * SSE no puede mandar headers → el token va por query string `?token=...`.
     * Sin token (Bar 0.1) la URL no se modifica.
     */
    fun abrirSse(host: String, puerto: Int, tokenLan: String? = null): HttpURLConnection {
        val tokenParam = if (tokenLan != null) "?token=${java.net.URLEncoder.encode(tokenLan, "UTF-8")}" else ""
        val conexion = URL("http://$host:$puerto${Rutas.EVENTOS}$tokenParam").openConnection() as HttpURLConnection
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

    private fun aplicarAuth(conexion: HttpURLConnection, tokenLan: String?) {
        if (tokenLan != null) {
            conexion.setRequestProperty("Authorization", "Bearer $tokenLan")
        }
    }

    private fun postJson(host: String, puerto: Int, path: String, json: String, tokenLan: String? = null): Pair<Int, String> {
        val conexion = URL("http://$host:$puerto$path").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 2500
            conexion.readTimeout = 4000
            conexion.requestMethod = "POST"
            conexion.doOutput = true
            aplicarAuth(conexion, tokenLan)
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conexion.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            val codigo = conexion.responseCode
            val texto = (if (codigo in 200..299) conexion.inputStream else conexion.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                .orEmpty()
            codigo to texto
        } catch (_: IOException) {
            0 to ""
        } finally {
            conexion.disconnect()
        }
    }
}
