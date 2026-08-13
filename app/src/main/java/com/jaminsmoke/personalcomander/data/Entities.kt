package com.jaminsmoke.personalcomander.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Estado operativo de mesa: reflejo del ciclo de comanda/pedido. */
enum class MesaEstado { LIBRE, OCUPADA, EN_COCINA }

/**
 * Estado visual de sala (board/lista): combina operativo + hold comercial.
 * No sustituye [MesaEstado]; se deriva con [mesaVisualStatus].
 */
enum class MesaVisualStatus { LIBRE, OCUPADA, EN_COCINA, RESERVADA, BLOQUEADA }

enum class MesaForma(val capacidadDefecto: Int) { REDONDA(2), CUADRADA(4), RECTANGULAR(8), RECTANGULAR_XL(12) }

enum class PedidoEstado { ABIERTA, ENVIADA, CERRADA }

enum class LineaEstado {
    PENDIENTE,
    /** Reservado para feature futura: marcar líneas como servidas en el panel de comanda */
    SERVIDA
}

@Entity(tableName = "salas")
data class Sala(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val orden: Int = 0,
)

@Entity(
    tableName = "mesas",
    foreignKeys = [
        ForeignKey(
            entity = Pedido::class,
            parentColumns = ["id"],
            childColumns = ["comandaActivaId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Sala::class,
            parentColumns = ["id"],
            childColumns = ["salaId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    // reservaActivaId sin FK Room (ALTER no añade FK; integridad en transacciones + Reserva.mesaId CASCADE)
    indices = [
        Index(value = ["comandaActivaId"]),
        Index(value = ["reservaActivaId"]),
        Index(value = ["salaId"]),
    ]
)
data class Mesa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numero: Int,
    val alias: String? = null,
    val forma: MesaForma = MesaForma.CUADRADA,
    val salaId: Long = 0,
    val capacidad: Int = 4,
    val estado: MesaEstado = MesaEstado.LIBRE,
    val comandaActivaId: Long? = null,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val girada: Boolean = false,
    /** Índice secuencial dentro de su sala (1,2,3…). Con [zonaPrefijo] forma el ID visible (B1, T2…). */
    val indiceZona: Int = 0,
    /** Hold de sala: mesa no disponible (rojo). Independiente del ciclo de comanda. */
    val bloqueada: Boolean = false,
    /** Puntero a [Reserva] activa; null = sin reserva. */
    val reservaActivaId: Long? = null
)

/** ID dentro de la sala, p.ej. "B1" para Barra 1. */
fun Mesa.idZona(nombreSala: String): String = "${zonaPrefijo(nombreSala)}$indiceZona"

/** Nombre visible: alias del usuario si existe; si no, el ID de sala (B1, T2…). */
fun Mesa.nombreVisible(nombreSala: String): String = alias ?: idZona(nombreSala)

fun Mesa.idZona(salas: Map<Long, Sala>): String = idZona(salas[salaId]?.nombre.orEmpty())

fun Mesa.nombreVisible(salas: Map<Long, Sala>): String = nombreVisible(salas[salaId]?.nombre.orEmpty())

/**
 * Prioridad: comanda activa (OCUPADA/EN_COCINA) > bloqueo > reserva > libre.
 */
fun mesaVisualStatus(mesa: Mesa): MesaVisualStatus = when (mesa.estado) {
    MesaEstado.OCUPADA -> MesaVisualStatus.OCUPADA
    MesaEstado.EN_COCINA -> MesaVisualStatus.EN_COCINA
    MesaEstado.LIBRE -> when {
        mesa.bloqueada -> MesaVisualStatus.BLOQUEADA
        mesa.reservaActivaId != null -> MesaVisualStatus.RESERVADA
        else -> MesaVisualStatus.LIBRE
    }
}

@Entity(
    tableName = "reservas",
    foreignKeys = [
        ForeignKey(
            entity = Mesa::class,
            parentColumns = ["id"],
            childColumns = ["mesaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mesaId"])]
)
data class Reserva(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mesaId: Long,
    /** Nombre del cliente o nota corta. */
    val nombre: String,
    /** Momento previsto (epoch ms); null si no se indicó. */
    val paraEpoch: Long? = null,
    val creadaEn: Long = 0L,
    val canceladaEn: Long? = null,
    /** Cuando la mesa pasó a ocupada por llegada del cliente. */
    val convertidaEn: Long? = null
)

/** Prefijo corto del nombre de sala para IDs tipo B1, T1, I1… */
fun zonaPrefijo(zona: String): String = when {
    zona.contains("Bar", ignoreCase = true) -> "B"
    zona.contains("Terraza", ignoreCase = true) -> "T"
    zona.contains("Interior", ignoreCase = true) || zona.contains("Salon", ignoreCase = true) || zona.contains("Salón", ignoreCase = true) -> "I"
    zona.contains("VIP", ignoreCase = true) || zona.contains("Reservado", ignoreCase = true) -> "V"
    zona.isBlank() -> "M"
    else -> zona.trim().firstOrNull()?.uppercase() ?: "M"
}

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val categoria: String,
    val precio: Double,
    val disponible: Boolean = true
)

@Entity(
    tableName = "pedidos",
    foreignKeys = [
        ForeignKey(
            entity = Mesa::class,
            parentColumns = ["id"],
            childColumns = ["mesaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mesaId"]), Index(value = ["creadoEn"])]
)
data class Pedido(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mesaId: Long,
    val estado: PedidoEstado = PedidoEstado.ABIERTA,
    val creadoEn: Long = 0L,
    val cerradoEn: Long? = null
)

@Entity(
    tableName = "lineas_pedido",
    foreignKeys = [
        ForeignKey(
            entity = Pedido::class,
            parentColumns = ["id"],
            childColumns = ["pedidoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["pedidoId"]), Index(value = ["productoId"])]
)
data class LineaPedido(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pedidoId: Long,
    val productoId: Long,
    val nombreProducto: String,
    val precioUnitario: Double,
    val cantidad: Int,
    val creadoEn: Long = 0L,
    val estado: LineaEstado = LineaEstado.PENDIENTE
)
