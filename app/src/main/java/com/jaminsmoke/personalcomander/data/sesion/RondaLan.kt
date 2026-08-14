package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.Gson
import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.idZona

/** Línea del contrato Bar `POST /v1/rondas`. `productoId` es el id Bar si hay `codigoBar`. */
data class LineaRondaLan(
    val productoId: String,
    val nombreProducto: String,
    val cantidad: Int,
    val estado: String = "PENDIENTE",
)

/**
 * Ronda hacia Personal Bar. [mesaId] es el idZona de la sala del mapa (T3),
 * no el id Room ni el alias, ni el establecimiento.
 */
data class RondaLan(
    val id: String,
    val mesaId: String,
    val numero: Int,
    val camarero: String?,
    val creadoEn: Long,
    val lineas: List<LineaRondaLan>,
)

object RondaLanMapper {
    private val gson = Gson()

    fun desdePedido(
        pedidoId: Long,
        mesa: Mesa,
        nombreSala: String,
        lineas: List<LineaPedido>,
        camarero: String?,
        creadoEn: Long,
        numero: Int = 1,
        codigoBarPorProductoId: Map<Long, String> = emptyMap(),
    ): RondaLan {
        require(lineas.isNotEmpty()) { "ronda sin líneas" }
        return RondaLan(
            id = "p$pedidoId-t$creadoEn",
            mesaId = mesa.idZona(nombreSala),
            numero = numero,
            camarero = camarero,
            creadoEn = creadoEn,
            lineas = lineas.map { linea ->
                LineaRondaLan(
                    productoId = codigoBarPorProductoId[linea.productoId]
                        ?.takeIf { it.isNotBlank() }
                        ?: linea.productoId.toString(),
                    nombreProducto = linea.nombreProducto,
                    cantidad = linea.cantidad,
                )
            },
        )
    }

    fun toJson(ronda: RondaLan): String = gson.toJson(ronda)
}
