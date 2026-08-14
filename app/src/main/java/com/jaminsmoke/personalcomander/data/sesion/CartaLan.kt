package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.Gson
import com.jaminsmoke.personalcomander.data.Producto

/** Producto del contrato Bar `GET /v1/carta`. [id] es el slug de red (`cana`). */
data class ProductoLan(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val precio: Double = 0.0,
    val disponible: Boolean = true,
)

data class CartaLan(
    val productos: List<ProductoLan> = emptyList(),
)

data class PlanCarta(
    val insertar: List<Producto>,
    val actualizar: List<Producto>,
)

/**
 * Espejo de la carta de Bar: parseo y plan de upsert por [Producto.codigoBar].
 * No borra productos locales sin código.
 */
object CartaSync {
    private val gson = Gson()

    fun parse(json: String): CartaLan? = try {
        val carta = gson.fromJson(json, CartaLan::class.java) ?: return null
        CartaLan(
            productos = carta.productos.filter { it.id.isNotBlank() && it.nombre.isNotBlank() },
        )
    } catch (_: Exception) {
        null
    }

    fun plan(existentes: List<Producto>, remotos: List<ProductoLan>): PlanCarta {
        val porCodigo = existentes.mapNotNull { p -> p.codigoBar?.let { it to p } }.toMap()
        val insertar = mutableListOf<Producto>()
        val actualizar = mutableListOf<Producto>()
        for (remoto in remotos) {
            val local = porCodigo[remoto.id]
            if (local == null) {
                insertar.add(
                    Producto(
                        nombre = remoto.nombre,
                        categoria = remoto.categoria,
                        precio = remoto.precio,
                        disponible = remoto.disponible,
                        codigoBar = remoto.id,
                    ),
                )
            } else {
                actualizar.add(
                    local.copy(
                        nombre = remoto.nombre,
                        categoria = remoto.categoria,
                        precio = remoto.precio,
                        disponible = remoto.disponible,
                    ),
                )
            }
        }
        return PlanCarta(insertar, actualizar)
    }
}
