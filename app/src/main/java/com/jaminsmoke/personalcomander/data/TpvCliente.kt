package com.jaminsmoke.personalcomander.data

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL

/** Proveedor de filas genérico para poder testear el mapeo en la JVM. */
interface FilasProvider {
    fun tablas(): List<String>
    fun filasDe(tabla: String, filtro: String? = null): List<Map<String, Any?>>
}

/** Lee un archivo SQLite local usando la API de Android. */
class SqliteFilasProvider(private val archivo: File) : FilasProvider {

    private val db: SQLiteDatabase =
        SQLiteDatabase.openDatabase(archivo.path, null, SQLiteDatabase.OPEN_READONLY)

    override fun tablas(): List<String> =
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type IN ('table','view') AND name NOT LIKE 'sqlite_%'",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    override fun filasDe(tabla: String, filtro: String?): List<Map<String, Any?>> {
        // Validate filtro: only allow alphanumeric, comparison operators, spaces, quotes, dots, underscores
        val safeFiltro = filtro?.takeIf { it.isNotBlank() }?.also {
            require(it.matches(Regex("^[a-zA-Z0-9_\\s=<>!'.()]*$"))) {
                "Invalid filter: contains characters not allowed in SQL WHERE clause"
            }
        }
        val seleccion = buildString {
            append("SELECT * FROM \"$tabla\"")
            if (safeFiltro != null) append(" WHERE $safeFiltro")
        }
        val columnas = db.rawQuery("$seleccion LIMIT 0", null).use { c ->
            (0 until c.columnCount).map { c.getColumnName(it) }
        }
        return db.rawQuery(seleccion, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val fila = LinkedHashMap<String, Any?>()
                    for (col in columnas) {
                        val idx = cursor.getColumnIndex(col)
                        fila[col] = when (cursor.getType(idx)) {
                            android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(idx)
                            android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(idx)
                            android.database.Cursor.FIELD_TYPE_STRING -> cursor.getString(idx)
                            android.database.Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(idx)
                            else -> null
                        }
                    }
                    add(fila)
                }
            }
        }
    }

    fun cerrar() {
        db.close()
    }
}

data class ServidorDescubierto(
    val ip: String,
    val puerto: Int,
    val etiqueta: String = ""
)

object TpvCliente {

    fun descargarArchivo(url: String, destino: File): Boolean {
        val conexion = URL(url).openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 5000
            conexion.readTimeout = 15000
            conexion.requestMethod = "GET"
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) return false
            conexion.inputStream.use { input ->
                destino.outputStream().use { output -> input.copyTo(output) }
            }
            true
        } finally {
            conexion.disconnect()
        }
    }

    fun descargarProductosJson(url: String): List<Producto>? {
        val conexion = URL(url).openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 5000
            conexion.readTimeout = 15000
            conexion.requestMethod = "GET"
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) return null
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            BackupJson.deserializar(texto)
        } finally {
            conexion.disconnect()
        }
    }
}

object EscaneadorRed {

    @Volatile
    private var cancelado = false

    fun cancelar() { cancelado = true }

    fun ipLocal(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        for (i in interfaces) {
            if (!i.isUp || i.isLoopback) continue
            for (addr in i.inetAddresses) {
                if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                    return addr.hostAddress ?: continue
                }
            }
        }
        return null
    }

    suspend fun escanear(puertos: List<Int>, timeoutMs: Int = 400): List<ServidorDescubierto> = coroutineScope {
        val ip = ipLocal() ?: return@coroutineScope emptyList()
        val partes = ip.split(".")
        if (partes.size != 4) return@coroutineScope emptyList()
        val red = "${partes[0]}.${partes[1]}.${partes[2]}"

        cancelado = false
        val resultados = (1..254).map { host ->
            async(Dispatchers.IO) {
                if (cancelado || !isActive) return@async null
                val destino = "$red.$host"
                if (destino == ip) return@async null
                for (puerto in puertos) {
                    if (cancelado || !isActive) break
                    if (puertoDisponible(destino, puerto, timeoutMs)) {
                        return@async ServidorDescubierto(destino, puerto)
                    }
                }
                null
            }
        }.awaitAll().filterNotNull()

        resultados.sortedBy { it.ip }.distinctBy { "${it.ip}:${it.puerto}" }
    }

    private fun puertoDisponible(host: String, puerto: Int, timeoutMs: Int): Boolean = try {
        val socket = Socket()
        socket.connect(InetSocketAddress(InetAddress.getByName(host), puerto), timeoutMs)
        socket.close()
        true
    } catch (e: Exception) {
        false
    }
}
