package com.jaminsmoke.personalcomander.data.sesion

import android.content.Context
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.AppDatabase
import com.jaminsmoke.personalcomander.data.LineaEstado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection

/**
 * Escucha SSE del Bar mientras el modo es Establecimiento.
 * Alinea líneas locales (ENVIADA → LISTA) y emite avisos para snackbar/notificación.
 */
class RecogerServicio(
    private val context: Context,
    private val db: AppDatabase,
    private val sesion: SesionRepository,
    scope: CoroutineScope,
) {
    private val _avisos = MutableSharedFlow<AvisoRecoger>(extraBufferCapacity = 8)
    val avisos: SharedFlow<AvisoRecoger> = _avisos.asSharedFlow()

    init {
        RecogerNotificador.asegurarCanal(context)
        scope.launch {
            sesion.modo.collectLatest { modo ->
                if (modo is ModoSesion.Establecimiento) {
                    bucle(modo)
                }
            }
        }
    }

    private suspend fun bucle(modo: ModoSesion.Establecimiento) {
        coroutineScope {
            if (modo.sesionTrabajo) {
                launch { latido(modo) }
            }
            while (isActive) {
                val tokenLan = tokenVigente()
                alinearConEstado(modo.barHost, modo.barPuerto, tokenLan)
                escucharSse(modo.barHost, modo.barPuerto, tokenLan)
                delay(2_000)
            }
        }
    }

    /** Token LAN más fresco del StateFlow (se actualiza tras re-ligar). */
    private fun tokenVigente(): String? =
        (sesion.modo.value as? ModoSesion.Establecimiento)?.tokenLan

    private suspend fun latido(modo: ModoSesion.Establecimiento) {
        while (currentCoroutineContext().isActive) {
            var tokenLan = tokenVigente()
            val r = withContext(Dispatchers.IO) {
                BarLanCliente.postHeartbeat(modo.barHost, modo.barPuerto, modo.perfil.id, tokenLan)
            }
            if (r.codigo == 401) {
                tokenLan = sesion.reLigarSilencioso()
                if (tokenLan != null) {
                    val r2 = withContext(Dispatchers.IO) {
                        BarLanCliente.postHeartbeat(modo.barHost, modo.barPuerto, modo.perfil.id, tokenLan)
                    }
                    if (r2.codigo == 401 || r2.codigo == 403) {
                        avisarJornadaCortada()
                        return
                    }
                } else {
                    avisarJornadaCortada()
                    return
                }
            } else if (r.codigo == 403) {
                avisarJornadaCortada()
                return
            }
            delay(HEARTBEAT_MS)
        }
    }

    private suspend fun alinearConEstado(host: String, puerto: Int, tokenLan: String? = null) {
        val estado = withContext(Dispatchers.IO) { BarLanCliente.estado(host, puerto, tokenLan) } ?: return
        val dao = db.lineaPedidoDao()
        for (ticket in RecogerLogica.ticketsDeColas(estado)) {
            if (ticket.estado.equals("PREPARADO", ignoreCase = true)) {
                dao.updateEstadoSi(ticket.id, LineaEstado.ENVIADA, LineaEstado.LISTA)
            }
        }
        for (ticket in estado.servidos) {
            dao.updateEstadoSi(ticket.id, LineaEstado.ENVIADA, LineaEstado.LISTA)
        }
    }

    private suspend fun escucharSse(host: String, puerto: Int, tokenLan: String? = null) {
        var conexion: HttpURLConnection? = null
        try {
            withContext(Dispatchers.IO) {
                val conn = BarLanCliente.abrirSse(host, puerto, tokenLan)
                conexion = conn
                coroutineContext.job.invokeOnCompletion {
                    try {
                        conn.disconnect()
                    } catch (_: Exception) {
                    }
                }
                BarLanCliente.leerSseAbierto(conn, debeParar = { !isActive }) { evento ->
                    runBlocking { aplicarEvento(evento) }
                }
            }
        } catch (_: Exception) {
            try {
                conexion?.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun aplicarEvento(evento: SalaEventLan) {
        when (evento.tipo) {
            RecogerLogica.TIPO_PREPARADO -> {
                val n = db.lineaPedidoDao().updateEstadoSi(
                    evento.ticketId,
                    LineaEstado.ENVIADA,
                    LineaEstado.LISTA,
                )
                val mesa = evento.mesaId?.takeIf { it.isNotBlank() }
                if (n > 0 || mesa != null) {
                    val aviso = AvisoRecoger(
                        texto = textoPreparado(evento, mesa),
                        mesaId = mesa,
                        ticketId = evento.ticketId,
                    )
                    _avisos.emit(aviso)
                    RecogerNotificador.mostrar(context, aviso)
                }
            }
            RecogerLogica.TIPO_RECOGIDO -> {
                db.lineaPedidoDao().updateEstadoSi(
                    evento.ticketId,
                    LineaEstado.ENVIADA,
                    LineaEstado.LISTA,
                )
            }
            RecogerLogica.TIPO_SESION_CORTADA -> avisarJornadaCortada()
        }
    }

    private suspend fun avisarJornadaCortada() {
        sesion.marcarJornadaCortada()
        _avisos.emit(
            AvisoRecoger(
                texto = context.getString(R.string.sesion_jornada_cortada),
                mesaId = null,
                ticketId = RecogerLogica.TIPO_SESION_CORTADA,
            ),
        )
    }

    private fun textoPreparado(evento: SalaEventLan, mesa: String?): String {
        val dest = when (RecogerLogica.destinoClave(evento.destino)) {
            "bebida" -> context.getString(R.string.recoger_destino_bebida)
            "comida" -> context.getString(R.string.recoger_destino_comida)
            else -> null
        }
        val cola = evento.numeroCola?.takeIf { it > 0 }
        return when (RecogerLogica.plantillaAviso(evento)) {
            PlantillaAviso.COMPLETO ->
                context.getString(R.string.recoger_aviso_preparado, mesa, cola, dest)
            PlantillaAviso.SOLO_MESA ->
                context.getString(R.string.recoger_aviso_preparado_mesa, mesa)
            PlantillaAviso.SIN_MESA ->
                context.getString(R.string.recoger_aviso_preparado_sin_mesa)
        }
    }
}

private const val HEARTBEAT_MS = 10_000L
