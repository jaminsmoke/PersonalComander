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
 * Cliente HTTP del **servicio camareros** de PersonalHostel Identity
 * (`:8080` en local / emulador). Misma pila que [com.jaminsmoke.personalcomander.data.TpvCliente].
 *
 * No apunta al servicio negocio (`:8082`). Las membresías llegan por
 * [Rutas.ME_ESTABLECIMIENTOS], que Identity consulta internamente a la BD de negocio.
 */
class IdentityCliente(
    private val baseUrl: String,
) {
    object Rutas {
        const val REGISTRO = "/v1/camareros/registro"
        const val LOGIN = "/v1/auth/login"
        const val ME = "/v1/camareros/me"
        const val ME_QR = "/v1/camareros/me/qr"
        const val ME_RENOVAR = "/v1/camareros/me/renovar"
        const val ME_REVOCAR = "/v1/camareros/me/revocar"
        const val ME_FOTO = "/v1/camareros/me/foto"
        const val ME_ESTABLECIMIENTOS = "/v1/camareros/me/establecimientos"
        const val ME_VISIBILIDAD = "/v1/camareros/me/visibilidad"

        fun todas(): List<String> = listOf(
            REGISTRO,
            LOGIN,
            ME,
            ME_QR,
            ME_RENOVAR,
            ME_REVOCAR,
            ME_FOTO,
            ME_ESTABLECIMIENTOS,
            ME_VISIBILIDAD,
        )
    }

    fun registrar(
        nombre: String,
        apellidos: String,
        email: String,
        password: String,
        telefono: String? = null,
        nick: String,
        origin: DataOrigin = DataOrigin.Real,
    ): IdentityRespuesta<IdentityJson.SesionIdentity> {
        val http = post(
            Rutas.REGISTRO,
            IdentityJson.cuerpoRegistro(nombre, apellidos, email, password, nick, telefono, origin),
        )
        if (http.codigo !in 200..299) return errorDe(http)
        val registro = IdentityJson.parseRegistro(http.cuerpo)
        val login = login(email, password)
        if (login.ok && login.valor != null) return login
        return IdentityRespuesta(
            true,
            IdentityJson.SesionIdentity(
                token = null,
                perfil = PerfilCamarero(
                    registro.id,
                    nombre,
                    apellidos,
                    email,
                    telefono,
                    nick = nick,
                    dataOrigin = registro.dataOrigin,
                ),
                qr = registro.qr,
                fichaUrl = registro.fichaUrl,
            ),
            codigo = http.codigo,
        )
    }

    fun login(email: String, password: String): IdentityRespuesta<IdentityJson.SesionIdentity> {
        val payload = JsonObject().apply {
            addProperty("email", email)
            addProperty("password", password)
        }
        val http = post(Rutas.LOGIN, payload.toString())
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, IdentityJson.parseLogin(http.cuerpo), codigo = http.codigo)
    }

    fun actualizarPerfil(token: String, nick: String): IdentityRespuesta<PerfilCamarero> {
        val payload = JsonObject().apply { addProperty("nick", nick) }
        val http = patch(Rutas.ME, payload.toString(), token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(
            true,
            IdentityJson.parsePerfil(com.google.gson.JsonParser.parseString(http.cuerpo)),
            codigo = http.codigo,
        )
    }

    fun me(token: String): IdentityRespuesta<PerfilCamarero> {
        val http = get(Rutas.ME, token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(
            true,
            IdentityJson.parsePerfil(com.google.gson.JsonParser.parseString(http.cuerpo)),
            codigo = http.codigo,
        )
    }

    fun meQr(token: String): IdentityRespuesta<IdentityJson.QrIdentity> {
        val http = get(Rutas.ME_QR, token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, IdentityJson.parseQr(http.cuerpo), codigo = http.codigo)
    }

    fun meVisibilidad(token: String): IdentityRespuesta<VisibilidadCamarero> {
        val http = get(Rutas.ME_VISIBILIDAD, token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, IdentityJson.parseVisibilidad(http.cuerpo), codigo = http.codigo)
    }

    fun actualizarVisibilidad(
        token: String,
        campo: CampoVisibilidad,
        valor: Boolean,
    ): IdentityRespuesta<VisibilidadCamarero> {
        val http = put(Rutas.ME_VISIBILIDAD, IdentityJson.cuerpoVisibilidad(campo, valor), token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, IdentityJson.parseVisibilidad(http.cuerpo), codigo = http.codigo)
    }

    fun meEstablecimientos(token: String): IdentityRespuesta<List<MembresiaEstablecimiento>> {
        val http = get(Rutas.ME_ESTABLECIMIENTOS, token)
        if (http.codigo !in 200..299) return errorDe(http)
        val lista = IdentityJson.parseEstablecimientos(http.cuerpo)
            ?: return IdentityRespuesta(false, error = "Respuesta inválida", codigo = http.codigo)
        return IdentityRespuesta(true, lista, codigo = http.codigo)
    }

    fun renovar(token: String): IdentityRespuesta<IdentityJson.QrIdentity> {
        val http = post(Rutas.ME_RENOVAR, "{}", token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, IdentityJson.parseQr(http.cuerpo), codigo = http.codigo)
    }

    fun revocar(token: String, motivo: String? = null): IdentityRespuesta<Unit> {
        val payload = JsonObject().apply {
            if (!motivo.isNullOrBlank()) addProperty("motivo", motivo)
        }
        val http = post(Rutas.ME_REVOCAR, payload.toString(), token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, Unit, codigo = http.codigo)
    }

    fun subirFoto(token: String, bytes: ByteArray, mime: String): IdentityRespuesta<String?> {
        val boundary = "----PcFoto${System.currentTimeMillis()}"
        val conexion = open(Rutas.ME_FOTO)
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
        val conexion = open(Rutas.ME_FOTO)
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
        val http = delete(Rutas.ME_FOTO, token)
        if (http.codigo !in 200..299) return errorDe(http)
        return IdentityRespuesta(true, Unit, codigo = http.codigo)
    }

    fun suprimirCuenta(token: String, password: String): IdentityRespuesta<Unit> {
        val payload = JsonObject().apply { addProperty("password", password) }
        val http = delete(Rutas.ME, token, payload.toString())
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

    private fun put(path: String, json: String, token: String): HttpCuerpo {
        val conexion = open(path)
        return try {
            conexion.requestMethod = "PUT"
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conexion.setRequestProperty("Authorization", "Bearer $token")
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

    private fun patch(path: String, json: String, token: String): HttpCuerpo {
        val conexion = open(path)
        return try {
            setHttpMethod(conexion, "PATCH")
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conexion.setRequestProperty("Authorization", "Bearer $token")
            conexion.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            leer(conexion)
        } catch (e: IOException) {
            HttpCuerpo(0, e.message ?: "Sin conexión")
        } finally {
            conexion.disconnect()
        }
    }

    /** PATCH no está en HttpURLConnection de API 24; se fuerza el método. */
    private fun setHttpMethod(conexion: HttpURLConnection, method: String) {
        try {
            conexion.requestMethod = method
        } catch (_: java.net.ProtocolException) {
            val campo = HttpURLConnection::class.java.getDeclaredField("method")
            campo.isAccessible = true
            campo.set(conexion, method)
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
