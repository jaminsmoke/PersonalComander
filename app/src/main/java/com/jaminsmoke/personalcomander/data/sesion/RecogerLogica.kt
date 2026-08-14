package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.jaminsmoke.personalcomander.data.LineaEstado
import com.jaminsmoke.personalcomander.data.LineaPedido

/** Ticket del body de `POST /v1/rondas` y de `GET /v1/estado`. */
data class TicketLan(
    val id: String = "",
    val rondaId: String = "",
    val destino: String? = null,
    val estado: String? = null,
    val preparadoPor: String? = null,
    val numeroCola: Int = 0,
    val lineas: List<LineaTicketLan> = emptyList(),
)

data class LineaTicketLan(
    val productoId: String = "",
    val nombreProducto: String = "",
    val cantidad: Int = 0,
)

/** Payload SSE `/v1/eventos`. Bar v1 anida destino/cola en [ticket]; los campos planos quedan de respaldo. */
data class SalaEventLan(
    val tipo: String = "",
    val ticketId: String = "",
    val rondaId: String? = null,
    val mesaId: String? = null,
    val destino: String? = null,
    val numeroCola: Int? = null,
    val preparadoPor: String? = null,
    val ticket: TicketLan? = null,
)

data class EstadoLan(
    val bebida: List<TicketLan> = emptyList(),
    val comida: List<TicketLan> = emptyList(),
    val servidos: List<TicketLan> = emptyList(),
)

data class BloquesComanda(
    val estaRonda: List<LineaPedido>,
    val paraRecoger: List<LineaPedido>,
    val servido: List<LineaPedido>,
)

data class AvisoRecoger(
    val texto: String,
    val mesaId: String?,
    val ticketId: String,
)

enum class PlantillaAviso { COMPLETO, SOLO_MESA, SIN_MESA }

/**
 * Lógica pura del circuito recoger: delta de líneas, parseo Gson y bloques de la comanda.
 */
object RecogerLogica {
    const val TIPO_PREPARADO = "ticket.preparado"
    const val TIPO_RECOGIDO = "ticket.recogido"

    private val gson = Gson()

    fun lineasAEnviar(lineas: List<LineaPedido>): List<LineaPedido> =
        lineas.filter { it.estado == LineaEstado.PENDIENTE }

    fun lineaPendienteDelProducto(lineas: List<LineaPedido>, productoId: Long): LineaPedido? =
        lineas.firstOrNull { it.productoId == productoId && it.estado == LineaEstado.PENDIENTE }

    fun bloques(lineas: List<LineaPedido>): BloquesComanda = BloquesComanda(
        estaRonda = lineas.filter {
            it.estado == LineaEstado.PENDIENTE || it.estado == LineaEstado.ENVIADA
        },
        paraRecoger = lineas.filter { it.estado == LineaEstado.LISTA },
        servido = lineas.filter { it.estado == LineaEstado.SERVIDA },
    )

    fun puedeMarcarServida(linea: LineaPedido, ligadoAlBar: Boolean): Boolean =
        linea.estado == LineaEstado.LISTA ||
            (!ligadoAlBar && linea.estado == LineaEstado.ENVIADA)

    /**
     * Cruza las líneas que acabamos de mandar con los tickets que Bar devolvió.
     * Si el parseo falla, las marca [LineaEstado.ENVIADA] sin `ticketId`.
     */
    fun asignarTickets(lineasEnviadas: List<LineaPedido>, tickets: List<TicketLan>): List<LineaPedido> {
        val cupos = tickets.flatMap { t ->
            t.lineas.map { Triple(t.id, it.productoId, it.cantidad) }
        }.toMutableList()
        return lineasEnviadas.map { linea ->
            val idx = cupos.indexOfFirst { it.second == linea.productoId.toString() }
            if (idx >= 0) {
                val ticketId = cupos.removeAt(idx).first
                linea.copy(estado = LineaEstado.ENVIADA, ticketId = ticketId)
            } else {
                linea.copy(estado = LineaEstado.ENVIADA)
            }
        }
    }

    fun parseSalaEvent(json: String, eventType: String? = null): SalaEventLan? = try {
        val e = gson.fromJson(json, SalaEventLan::class.java) ?: return null
        if (e.ticketId.isBlank()) return null
        val tipo = e.tipo.ifBlank { eventType.orEmpty() }
        if (tipo.isBlank()) return null
        hidratarDesdeTicket(e.copy(tipo = tipo))
    } catch (_: Exception) {
        null
    }

    /**
     * Bar publica `destino` / `numeroCola` / `rondaId` dentro de `ticket`, no en la raíz.
     * Si ya vienen planos (contrato antiguo del plan), se respetan.
     */
    fun hidratarDesdeTicket(evento: SalaEventLan): SalaEventLan {
        val t = evento.ticket ?: return evento
        return evento.copy(
            rondaId = evento.rondaId?.takeIf { it.isNotBlank() } ?: t.rondaId.takeIf { it.isNotBlank() },
            destino = evento.destino ?: t.destino,
            numeroCola = evento.numeroCola?.takeIf { it > 0 } ?: t.numeroCola.takeIf { it > 0 },
        )
    }

    /** Qué plantilla de aviso usar: completo (mesa+cola+destino), solo mesa, o genérico. */
    fun plantillaAviso(evento: SalaEventLan): PlantillaAviso {
        val mesa = evento.mesaId?.takeIf { it.isNotBlank() }
        val dest = destinoClave(evento.destino)
        val cola = evento.numeroCola?.takeIf { it > 0 }
        return when {
            mesa != null && cola != null && dest != null -> PlantillaAviso.COMPLETO
            mesa != null -> PlantillaAviso.SOLO_MESA
            else -> PlantillaAviso.SIN_MESA
        }
    }

    fun parseTickets(json: String): List<TicketLan> = try {
        val arr = JsonParser.parseString(json).asJsonArray
        arr.mapNotNull { el ->
            val t = gson.fromJson(el, TicketLan::class.java)
            t?.takeIf { it.id.isNotBlank() }
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun parseEstado(json: String): EstadoLan? = try {
        gson.fromJson(json, EstadoLan::class.java)
    } catch (_: Exception) {
        null
    }

    fun ticketsDeColas(estado: EstadoLan): List<TicketLan> = estado.bebida + estado.comida

    /** `BARRA` → bebida, `COCINA` → comida; null si Bar no mandó destino. */
    fun destinoClave(destino: String?): String? = when (destino?.uppercase()) {
        "BARRA" -> "bebida"
        "COCINA" -> "comida"
        else -> null
    }

    /**
     * Acumula un frame SSE (`event:` / `data:` / línea vacía).
     * Devuelve el evento cerrado o null si el frame aún no termina.
     */
    fun alimentarSse(
        eventType: String?,
        data: StringBuilder,
        line: String,
    ): Pair<String?, SalaEventLan?> {
        when {
            line.startsWith("event:") -> return line.substring(6).trim() to null
            line.startsWith("data:") -> {
                if (data.isNotEmpty()) data.append('\n')
                data.append(line.substring(5).trimStart())
                return eventType to null
            }
            line.isEmpty() -> {
                val json = data.toString()
                data.clear()
                val evento = if (json.isNotEmpty()) parseSalaEvent(json, eventType) else null
                return null to evento
            }
            else -> return eventType to null
        }
    }
}
