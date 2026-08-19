package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.Gson
import com.jaminsmoke.personalcomander.data.Producto

/** Producto del contrato Bar `GET /v1/carta`. [id] es el id de red: slug (`cana`) o UUID. */
data class ProductoLan(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val precio: Double = 0.0,
    val disponible: Boolean = true,
    val subfamilia: String? = null,
    val permiteNota: Boolean = false,
    /** Ids de red de [CartaLan.gruposModificador] asignados a este SKU. */
    val grupos: List<String> = emptyList(),
)

data class OpcionModificadorLan(
    val id: String = "",
    val nombre: String = "",
    val deltaPrecio: Double = 0.0,
    val alias: String = "",
)

data class GrupoModificadorLan(
    val id: String = "",
    val nombre: String = "",
    val multiple: Boolean = false,
    val obligatorio: Boolean = false,
    val opciones: List<OpcionModificadorLan> = emptyList(),
)

data class CartaLan(
    /** Esquema del contrato de carta (`CartaResponse.schema`). 0 = Bar sin campo (legacy / slugs). */
    val schema: Int = 0,
    val productos: List<ProductoLan> = emptyList(),
    val gruposModificador: List<GrupoModificadorLan> = emptyList(),
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
    private val idUuid = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )

    fun parse(json: String): CartaLan? = try {
        val carta = gson.fromJson(json, CartaLan::class.java) ?: return null
        CartaLan(
            schema = carta.schema,
            productos = carta.productos.orEmpty().mapNotNull { p ->
                if (p.id.isBlank() || p.nombre.isBlank()) null
                else p.copy(
                    grupos = p.grupos.orEmpty(),
                    subfamilia = p.subfamilia?.trim()?.takeIf { it.isNotEmpty() },
                )
            },
            gruposModificador = carta.gruposModificador.orEmpty()
                .filter { it.id.isNotBlank() && it.nombre.isNotBlank() }
                .map { g ->
                    g.copy(opciones = g.opciones.orEmpty().filter {
                        it.id.isNotBlank() && it.nombre.isNotBlank()
                    })
                },
        )
    } catch (_: Exception) {
        null
    }

    private fun Producto.conRemoto(remoto: ProductoLan, codigoBar: String = remoto.id) = copy(
        nombre = remoto.nombre,
        categoria = remoto.categoria,
        precio = remoto.precio,
        disponible = remoto.disponible,
        codigoBar = codigoBar,
        subfamilia = remoto.subfamilia?.trim()?.takeIf { it.isNotEmpty() },
        permiteNota = remoto.permiteNota,
    )

    private fun desdeRemoto(remoto: ProductoLan) = Producto(
        nombre = remoto.nombre,
        categoria = remoto.categoria,
        precio = remoto.precio,
        disponible = remoto.disponible,
        codigoBar = remoto.id,
        subfamilia = remoto.subfamilia?.trim()?.takeIf { it.isNotEmpty() },
        permiteNota = remoto.permiteNota,
    )

    fun plan(existentes: List<Producto>, remotos: List<ProductoLan>): PlanCarta {
        val porCodigo = existentes.mapNotNull { p -> p.codigoBar?.let { it to p } }.toMap()
        val insertar = mutableListOf<Producto>()
        val actualizar = mutableListOf<Producto>()
        for (remoto in remotos) {
            val local = porCodigo[remoto.id]
            if (local == null) {
                insertar.add(desdeRemoto(remoto))
            } else {
                actualizar.add(local.conRemoto(remoto))
            }
        }
        return PlanCarta(insertar, actualizar)
    }

    fun pareceIdUuid(id: String): Boolean = idUuid.matches(id.trim())

    /**
     * Hay que re-apuntar por nombre si Bar bumpó [schema] o si los ids de red
     * pasaron de slug a UUID aunque el nodo aún no mande `schema`.
     */
    fun debeReconstruir(
        schemaRemoto: Int,
        schemaGuardado: Int,
        existentes: List<Producto>,
        remotos: List<ProductoLan>,
    ): Boolean {
        if (schemaRemoto != schemaGuardado) return true
        val locales = existentes.mapNotNull { it.codigoBar?.takeIf { codigo -> codigo.isNotBlank() } }
        if (locales.isEmpty() || remotos.isEmpty()) return false
        val remotosUuid = remotos.all { pareceIdUuid(it.id) }
        val localesLegacy = locales.any { !pareceIdUuid(it) }
        return remotosUuid && localesLegacy
    }

    /**
     * Reconstrucción del espejo cuando cambia el esquema de ids de Bar (p. ej. slug→UUID).
     * Re-apunta los productos espejados por nombre —conservando su id Long local para no
     * romper las líneas históricas (`LineaPedido.productoId` FK NO ACTION)— en lugar de
     * borrar e insertar. Los productos solo locales (codigoBar null) no se tocan.
     */
    fun planReconstruccion(existentes: List<Producto>, remotos: List<ProductoLan>): PlanCarta {
        val insertar = mutableListOf<Producto>()
        val actualizar = mutableListOf<Producto>()
        val espejados = existentes.filter { it.codigoBar != null }.toMutableList()
        for (remoto in remotos) {
            val idx = espejados.indexOfFirst {
                it.nombre.trim().equals(remoto.nombre.trim(), ignoreCase = true)
            }
            if (idx >= 0) {
                val local = espejados.removeAt(idx)
                actualizar.add(local.conRemoto(remoto))
            } else {
                insertar.add(desdeRemoto(remoto))
            }
        }
        return PlanCarta(insertar, actualizar)
    }
}
