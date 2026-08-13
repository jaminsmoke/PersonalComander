package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class IdentityRespuesta<T>(
    val ok: Boolean,
    val valor: T? = null,
    val error: String? = null,
    val codigo: Int = 0,
    val code: String? = null,
)

/**
 * Cliente HTTP de PersonalHostel Identity. Misma pila que [com.jaminsmoke.personalcomander.data.TpvCliente].
 */
class IdentityCliente(
    private val baseUrl: String,
) {
    fun registrar(
        nombre: String,
        apellidos: String,
        email: String,
        password: String,
        telefono: String? = null,
    ): IdentityRespuesta<IdentityJson.SesionIdentity> {
        val payload = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("apellidos", apellidos)
            addProperty("email", email)
            addProperty("password", password)
            if (!telefono.isNullOrBlank()) addProperty("telefono", telefono)
        }
        val http = post("/v1/camareros/registro", payload.toString())
        if (http.codigo !in 200..299) return errorDe(http)
        val (id, qr) = IdentityJson.parseRegistro(http.cuerpo)
        val login = login(email, password)
        if (login.ok && login.valor != null) return login
        return IdentityRespuesta(
            true,
            IdentityJson.SesionIdentity(
                token = null,
                perfil = PerfilCamarero(id, nombre, apellidos, email, telefono),
                qr = qr,
            ),
            codigo = http.codigo,
        )
    }

    fun login(email: String, password: String): IdentityRespuesta<IdentityJson.SesionIdentity> {
        val payload = JsonObject().apply {
            addProperty("email", email)
            addProperty("password", password)
        }
        val http = post("/v1/auth/login", payload.toString())
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, IdentityJson.parseLogin(http.cuerpo), codigo = http.codigo)
    }

    fun me(token: String): IdentityRespuesta<PerfilCamarero> {
        val http = get("/v1/camareros/me", token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(
            true,
            IdentityJson.parsePerfil(com.google.gson.JsonParser.parseString(http.cuerpo)),
            codigo = http.codigo,
        )
    }

    fun meQr(token: String): IdentityRespuesta<String> {
        val http = get("/v1/camareros/me/qr", token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, IdentityJson.parseQr(http.cuerpo), codigo = http.codigo)
    }

    fun renovar(token: String): IdentityRespuesta<String> {
        val http = post("/v1/camareros/me/renovar", "{}", token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, IdentityJson.parseQr(http.cuerpo), codigo = http.codigo)
    }

    fun revocar(token: String, motivo: String? = null): IdentityRespuesta<Unit> {
        val payload = JsonObject().apply {
            if (!motivo.isNullOrBlank()) addProperty("motivo", motivo)
        }
        val http = post("/v1/camareros/me/revocar", payload.toString(), token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, Unit, codigo = http.codigo)
    }

    fun subirFoto(token: String, bytes: ByteArray, mime: String): IdentityRespuesta<String?> {
        val boundary = "----PcFoto${System.currentTimeMillis()}"
        val conexion = open("/v1/camareros/me/foto")
        return try {
            conexion.requestMethod = "POST"
            conexion.doOutput = true
            conexion.setRequestProperty("Authorization", "Bearer $token")
            conexion.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            val filename = when {
                mime.contains("png") -> "foto.png"
                mime.contains("webp") -> "foto.webp"
                else -> "foto.jpg"
            }
            val preamble = buildString {
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"foto\"; filename=\"$filename\"\r\n")
                append("Content-Type: $mime\r\n\r\n")
            }.toByteArray(Charsets.UTF_8)
            val closing = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
            conexion.setFixedLengthStreamingMode(preamble.size + bytes.size + closing.size)
            conexion.outputStream.use { out ->
                out.write(preamble)
                out.write(bytes)
                out.write(closing)
            }
            val http = leer(conexion)
            if (http.codigo !in 200..299) return errorDe(http)
            IdentityRespuesta(true, IdentityJson.parseFotoUrl(http.cuerpo), codigo = http.codigo)
        } catch (e: IOException) {
            IdentityRespuesta(false, error = e.message ?: "Sin conexión")
        } finally {
            conexion.disconnect()
        }
    }

    fun foto(token: String): IdentityRespuesta<ByteArray> {
        val conexion = open("/v1/camareros/me/foto")
        return try {
            conexion.requestMethod = "GET"
            conexion.setRequestProperty("Authorization", "Bearer $token")
            val codigo = conexion.responseCode
            val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val bytes = stream?.readBytes() ?: ByteArray(0)
            if (codigo !in 200..299) {
                val err = IdentityJson.parseError(bytes.toString(Charsets.UTF_8))
                return IdentityRespuesta(false, error = err.detail, codigo = codigo, code = err.code)
            }
            IdentityRespuesta(true, bytes, codigo = codigo)
        } catch (e: IOException) {
            IdentityRespuesta(false, error = e.message ?: "Sin conexión")
        } finally {
            conexion.disconnect()
        }
    }

    fun borrarFoto(token: String): IdentityRespuesta<Unit> {
        val http = delete("/v1/camareros/me/foto", token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, Unit, codigo = http.codigo)
    }

    fun suprimirCuenta(token: String, password: String): IdentityRespuesta<Unit> {
        val payload = JsonObject().apply { addProperty("password", password) }
        val http = delete("/v1/camareros/me", token, payload.toString())
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, Unit, codigo = http.codigo)
    }

    private fun <T> errorDe(http: HttpCuerpo): IdentityRespuesta<T> {
        val err = IdentityJson.parseError(http.cuerpo)
        return IdentityRespuesta(false, error = err.detail, codigo = http.codigo, code = err.code)
    }

    private fun post(path: String, json: String, token: String? = null): HttpCuerpo {
        val conexion = open(path)
        return try {
            conexion.requestMethod = "POST"
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (token != null) conexion.setRequestProperty("Authorization", "Bearer $token")
            conexion.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            leer(conexion)
        } catch (e: IOException) {
            HttpCuerpo(0, e.message ?: "Sin conexión")
        } finally {
            conexion.disconnect()
        }
    }

    private fun get(path: String, token: String): HttpCuerpo {
        val conexion = open(path)
        return try {
            conexion.requestMethod = "GET"
            conexion.setRequestProperty("Authorization", "Bearer $token")
            leer(conexion)
        } catch (e: IOException) {
            HttpCuerpo(0, e.message ?: "Sin conexión")
        } finally {
            conexion.disconnect()
        }
    }

    private fun delete(path: String, token: String, json: String? = null): HttpCuerpo {
        val conexion = open(path)
        return try {
            conexion.requestMethod = "DELETE"
            conexion.setRequestProperty("Authorization", "Bearer $token")
            if (json != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexion.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }
            leer(conexion)
        } catch (e: IOException) {
            HttpCuerpo(0, e.message ?: "Sin conexión")
        } finally {
            conexion.disconnect()
        }
    }

    private fun open(path: String): HttpURLConnection {
        val raiz = baseUrl.trimEnd('/')
        val conexion = URL("$raiz$path").openConnection() as HttpURLConnection
        conexion.connectTimeout = 8000
        conexion.readTimeout = 15000
        return conexion
    }

    private fun leer(conexion: HttpURLConnection): HttpCuerpo {
        val codigo = try {
            conexion.responseCode
        } catch (e: IOException) {
            return HttpCuerpo(0, e.message ?: "Sin conexión")
        }
        val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
        val cuerpo = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        return HttpCuerpo(codigo, cuerpo)
    }

    private data class HttpCuerpo(val codigo: Int, val cuerpo: String)
}
