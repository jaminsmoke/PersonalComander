package com.jaminsmoke.personalcomander.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Opción elegida en una línea (snapshot para historial y ronda). */
data class ModificadorElegido(
    val grupoId: Long = 0,
    val grupoNombre: String = "",
    val opcionId: Long = 0,
    val opcionNombre: String = "",
    val deltaPrecio: Double = 0.0,
)

data class GrupoConOpciones(
    val grupo: GrupoModificador,
    val opciones: List<OpcionModificador>,
)

object CartaModificadores {
    private val gson = Gson()
    private val tipoLista = object : TypeToken<List<ModificadorElegido>>() {}.type

    fun canonicalJson(elegidos: List<ModificadorElegido>): String {
        val ordenados = elegidos.sortedWith(compareBy({ it.grupoId }, { it.opcionId }))
        return gson.toJson(ordenados)
    }

    fun parseJson(json: String?): List<ModificadorElegido> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        return runCatching {
            gson.fromJson<List<ModificadorElegido>>(json, tipoLista).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun textoLinea(elegidos: List<ModificadorElegido>, nota: String?): String {
        val partes = elegidos.map { it.opcionNombre.trim() }.filter { it.isNotEmpty() }.toMutableList()
        val n = nota?.trim().orEmpty()
        if (n.isNotEmpty()) partes.add(n)
        return partes.joinToString(" · ")
    }

    fun precioUnitario(base: Double, elegidos: List<ModificadorElegido>): Double =
        base + elegidos.sumOf { it.deltaPrecio }

    fun gruposDeProducto(
        productoId: Long,
        grupos: List<GrupoModificador>,
        opciones: List<OpcionModificador>,
        asignaciones: List<ProductoGrupo>,
    ): List<GrupoConOpciones> {
        val ids = asignaciones.filter { it.productoId == productoId }.map { it.grupoId }.toSet()
        return grupos.filter { it.id in ids }.map { g ->
            GrupoConOpciones(g, opciones.filter { it.grupoId == g.id })
        }
    }

    fun faltanObligatorios(
        grupos: List<GrupoConOpciones>,
        elegidos: List<ModificadorElegido>,
    ): Boolean {
        val porGrupo = elegidos.groupBy { it.grupoId }
        return grupos.any { gc ->
            gc.grupo.obligatorio && porGrupo[gc.grupo.id].isNullOrEmpty()
        }
    }

    fun tokensOpcion(opcion: OpcionModificador): List<List<String>> {
        val nombres = buildList {
            add(opcion.nombre)
            opcion.alias.split('|').map { it.trim() }.filter { it.isNotEmpty() }.forEach { add(it) }
        }
        return nombres.map { normalizarNombre(it).split(" ").filter { t -> t.isNotEmpty() } }
            .filter { it.isNotEmpty() }
    }

    /** Agrupa consecutivos por subfamilia (el ORDER BY de la carta ya viene así). */
    fun agruparPorSubfamilia(productos: List<Producto>): List<Pair<String?, List<Producto>>> {
        if (productos.isEmpty()) return emptyList()
        val out = mutableListOf<Pair<String?, MutableList<Producto>>>()
        for (p in productos) {
            val clave = p.subfamiliaOrNull()
            val ultimo = out.lastOrNull()
            if (ultimo != null && ultimo.first == clave) ultimo.second.add(p)
            else out.add(clave to mutableListOf(p))
        }
        return out.map { it.first to it.second.toList() }
    }
}

fun LineaPedido.modificadores(): List<ModificadorElegido> =
    CartaModificadores.parseJson(modificadoresJson)

fun LineaPedido.detalleModificadores(): String =
    CartaModificadores.textoLinea(modificadores(), nota)

fun Producto.subfamiliaOrNull(): String? = subfamilia?.trim()?.takeIf { it.isNotEmpty() }
