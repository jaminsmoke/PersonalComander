package com.jaminsmoke.personalcomander.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.data.BackupJson
import com.jaminsmoke.personalcomander.data.EscaneadorRed
import com.jaminsmoke.personalcomander.data.ServidorDescubierto
import com.jaminsmoke.personalcomander.data.SqliteFilasProvider
import com.jaminsmoke.personalcomander.data.TpvCliente
import com.jaminsmoke.personalcomander.data.TpvPrograma
import com.jaminsmoke.personalcomander.data.fusionarProductos
import com.jaminsmoke.personalcomander.data.mapFilaProducto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AjustesSyncState(
    val programa: TpvPrograma = TpvPrograma.AGORA,
    val host: String = "",
    val puerto: String = TpvPrograma.AGORA.puertoPorDefecto.toString(),
    val ruta: String = TpvPrograma.AGORA.rutaPorDefecto,
    val escaneando: Boolean = false,
    val servidores: List<ServidorDescubierto> = emptyList(),
    val sincronizando: Boolean = false,
    val mensaje: String? = null
)

data class ImportPreview(
    val nuevos: Int,
    val actualizados: Int,
    val ignorados: Int
)

class AjustesViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private val db = (application as PersonalComanderApp).db

    private val _sync = MutableStateFlow(AjustesSyncState())
    val sync: StateFlow<AjustesSyncState> = _sync.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun setPrograma(programa: TpvPrograma) {
        _sync.update { it.copy(programa = programa, puerto = programa.puertoPorDefecto.toString(), ruta = programa.rutaPorDefecto) }
    }

    fun setHost(host: String) { _sync.update { it.copy(host = host) } }
    fun setPuerto(puerto: String) { _sync.update { it.copy(puerto = puerto) } }
    fun setRuta(ruta: String) { _sync.update { it.copy(ruta = ruta) } }

    fun elegirServidor(servidor: ServidorDescubierto) {
        _sync.update { it.copy(host = servidor.ip, puerto = servidor.puerto.toString()) }
    }

    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()

    fun limpiarMensaje() { _mensaje.value = null }

    fun exportar(uri: Uri) {
        viewModelScope.launch {
            try {
                val productos = db.productoDao().getAllIncluyendoOcultos()
                val json = BackupJson.serializar(productos)
                context.contentResolver.openOutputStream(uri)?.use { out -> out.write(json.toByteArray(Charsets.UTF_8)) }
                _mensaje.value = "Exportados ${productos.size} productos"
            } catch (e: Exception) {
                _mensaje.value = "Error al exportar: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun importar(uri: Uri) {
        viewModelScope.launch {
            try {
                val texto = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                val importados = BackupJson.deserializar(texto ?: "")
                if (importados == null) { _mensaje.value = "El archivo no es un JSON válido de Personal Comander"; return@launch }
                val existentes = db.productoDao().getAllIncluyendoOcultos()
                val fusion = fusionarProductos(existentes, importados)
                _importPreview.value = ImportPreview(fusion.insertados, fusion.actualizados, fusion.ignorados)
            } catch (e: Exception) {
                _mensaje.value = "Error al leer archivo: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun confirmarImportacion(uri: Uri) {
        viewModelScope.launch {
            try {
                _importPreview.value = null
                val texto = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                val importados = BackupJson.deserializar(texto ?: "")
                if (importados == null) { _mensaje.value = "El archivo no es un JSON válido"; return@launch }
                val existentes = db.productoDao().getAllIncluyendoOcultos()
                val fusion = fusionarProductos(existentes, importados)
                if (fusion.insertar.isNotEmpty()) db.productoDao().insertAll(fusion.insertar)
                fusion.actualizar.forEach { db.productoDao().update(it) }
                _mensaje.value = "Importados: ${fusion.insertados} nuevos, ${fusion.actualizados} actualizados, ${fusion.ignorados} ignorados"
            } catch (e: Exception) {
                _mensaje.value = "Error al importar: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun cancelarImportacion() { _importPreview.value = null }

    fun buscarServidores() {
        viewModelScope.launch {
            _sync.update { it.copy(escaneando = true, servidores = emptyList(), mensaje = null) }
            val puertoActual = _sync.value.puerto.toIntOrNull() ?: _sync.value.programa.puertoPorDefecto
            val puertos = listOf(puertoActual) + listOf(80, 8080, 8000).filterNot { it == puertoActual }
            val resultados = withContext(Dispatchers.IO) { EscaneadorRed.escanear(puertos) }
            _sync.update { it.copy(escaneando = false, servidores = resultados, mensaje = if (resultados.isEmpty()) "No se encontraron servidores en la red local" else "Se encontraron ${resultados.size} servidores") }
        }
    }

    fun sincronizar() {
        val estado = _sync.value
        if (estado.host.isBlank()) { _mensaje.value = "Indica la IP del servidor TPV"; return }
        viewModelScope.launch {
            _sync.update { it.copy(sincronizando = true, mensaje = null) }
            val resultado = withContext(Dispatchers.IO) { ejecutarSincronizacion(estado) }
            _sync.update { it.copy(sincronizando = false, mensaje = resultado) }
        }
    }

    private suspend fun ejecutarSincronizacion(estado: AjustesSyncState): String {
        val puerto = estado.puerto.toIntOrNull() ?: estado.programa.puertoPorDefecto
        val url = "http://${estado.host}:$puerto/${estado.ruta.trimStart('/')}"
        return try {
            val productos = when (estado.programa.tipo) {
                com.jaminsmoke.personalcomander.data.TipoFuenteTpv.JSON ->
                    TpvCliente.descargarProductosJson(url) ?: return "No se pudo leer el JSON en $url"
                com.jaminsmoke.personalcomander.data.TipoFuenteTpv.SQLITE -> {
                    val mapeo = estado.programa.mapeo ?: return "El programa seleccionado no tiene mapeo"
                    val archivo = File(context.cacheDir, "tpv_${System.currentTimeMillis()}.db")
                    if (!TpvCliente.descargarArchivo(url, archivo)) return "No se pudo descargar la base de datos en $url"
                    val provider = SqliteFilasProvider(archivo)
                    try {
                        val tablas = provider.tablas()
                        if (mapeo.tabla !in tablas) return "No existe la tabla '${mapeo.tabla}'. Disponibles: " + tablas.take(8).joinToString(", ")
                        provider.filasDe(mapeo.tabla, mapeo.filtro).mapNotNull { mapFilaProducto(it, mapeo) }
                    } finally { provider.cerrar(); archivo.delete() }
                }
            }
            aplicarProductos(productos)
        } catch (e: Exception) { "Error de sincronización: ${e.message ?: e.javaClass.simpleName}" }
    }

    private suspend fun aplicarProductos(importados: List<com.jaminsmoke.personalcomander.data.Producto>): String {
        val existentes = db.productoDao().getAllIncluyendoOcultos()
        val fusion = fusionarProductos(existentes, importados)
        if (fusion.insertar.isNotEmpty()) db.productoDao().insertAll(fusion.insertar)
        fusion.actualizar.forEach { db.productoDao().update(it) }
        return "Sincronizados ${fusion.insertados} nuevos, ${fusion.actualizados} actualizados, ${fusion.ignorados} ignorados"
    }
}
