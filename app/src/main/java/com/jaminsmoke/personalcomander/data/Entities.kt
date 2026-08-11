package com.jaminsmoke.personalcomander.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class MesaEstado { LIBRE, OCUPADA, EN_COCINA }

enum class PedidoEstado { ABIERTA, ENVIADA, CERRADA }

enum class LineaEstado { PENDIENTE, SERVIDA }

@Entity(
    tableName = "mesas",
    foreignKeys = [
        ForeignKey(
            entity = Pedido::class,
            parentColumns = ["id"],
            childColumns = ["comandaActivaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Mesa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numero: Int,
    val estado: MesaEstado = MesaEstado.LIBRE,
    val comandaActivaId: Long? = null
)

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val categoria: String,
    val precio: Double,
    val disponible: Boolean = true
)

@Entity(tableName = "pedidos")
data class Pedido(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mesaId: Long,
    val estado: PedidoEstado = PedidoEstado.ABIERTA,
    val creadoEn: Long = 0L,
    val cerradoEn: Long? = null
)

@Entity(tableName = "lineas_pedido")
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
