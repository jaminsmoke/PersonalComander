package com.jaminsmoke.personalcomander.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MesaEstado { LIBRE, OCUPADA, EN_COCINA }

enum class MesaForma(val capacidadDefecto: Int) { REDONDA(2), CUADRADA(4), RECTANGULAR(8), RECTANGULAR_XL(12) }

enum class PedidoEstado { ABIERTA, ENVIADA, CERRADA }

enum class LineaEstado {
    PENDIENTE,
    /** Reservado para feature futura: marcar líneas como servidas en el panel de comanda */
    SERVIDA
}

@Entity(
    tableName = "mesas",
    foreignKeys = [
        ForeignKey(
            entity = Pedido::class,
            parentColumns = ["id"],
            childColumns = ["comandaActivaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["comandaActivaId"])]
)
data class Mesa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numero: Int,
    val alias: String? = null,
    val forma: MesaForma = MesaForma.CUADRADA,
    val zona: String = "",
    val capacidad: Int = 4,
    val estado: MesaEstado = MesaEstado.LIBRE,
    val comandaActivaId: Long? = null,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val girada: Boolean = false,
    /** Índice secuencial dentro de su zona (1,2,3…). Con [zonaPrefijo] forma el ID visible (B1, T2…). */
    val indiceZona: Int = 0
) {
    /** ID dentro de la zona, p.ej. "B1" para Barra 1. */
    val idZona: String get() = "${zonaPrefijo(zona)}$indiceZona"

    /** Nombre visible: alias del usuario si existe; si no, el ID de zona (B1, T2…) */
    val nombreVisible: String get() = alias ?: idZona
}

/** Prefijo corto de zona para IDs tipo B1, T1, I1… */
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
