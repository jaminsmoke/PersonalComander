package com.jaminsmoke.personalcomander.data

enum class TipoFuenteTpv { SQLITE, JSON }

/**
 * Programas de gestión conocidos. Los mapeos de esquema son orientativos:
 * el usuario puede ajustar tabla/columnas desde Ajustes. Se cargan por defecto
 * para que la importación sea de un clic cuando el esquema coincide.
 */
enum class TpvPrograma(
    val nombre: String,
    val tipo: TipoFuenteTpv,
    val puertoPorDefecto: Int,
    val rutaPorDefecto: String,
    val mapeo: TpvMapeo?
) {
    AGORA(
        nombre = "Ágora TPV",
        tipo = TipoFuenteTpv.SQLITE,
        puertoPorDefecto = 8080,
        rutaPorDefecto = "agora.db",
        mapeo = TpvMapeo(
            tabla = "PRODUCTOS",
            colNombre = "DESCRIPCION",
            colPrecio = "PRECIO",
            colCategoria = "FAMILIA",
            colDisponible = "ACTIVO",
            filtro = "ACTIVO = 1"
        )
    ),
    COMANDA_WIN(
        nombre = "ComandaWin",
        tipo = TipoFuenteTpv.SQLITE,
        puertoPorDefecto = 80,
        rutaPorDefecto = "comandawin.db",
        mapeo = TpvMapeo(
            tabla = "articulos",
            colNombre = "nombre",
            colPrecio = "pvp",
            colCategoria = "familia",
            colDisponible = null,
            filtro = null
        )
    ),
    HOSTEL_SOFT(
        nombre = "Rest Hostelería",
        tipo = TipoFuenteTpv.SQLITE,
        puertoPorDefecto = 80,
        rutaPorDefecto = "hostelsoft.db",
        mapeo = TpvMapeo(
            tabla = "ARTICULOS",
            colNombre = "DESCRIPCION",
            colPrecio = "PRECIO_VENTA",
            colCategoria = "GRUPO",
            colDisponible = "BAJA",
            filtro = "BAJA = 0"
        )
    ),
    SERVICIO_JSON(
        nombre = "Servidor JSON genérico",
        tipo = TipoFuenteTpv.JSON,
        puertoPorDefecto = 8000,
        rutaPorDefecto = "productos.json",
        mapeo = null
    );

    companion object {
        fun porNombre(nombre: String): TpvPrograma? = entries.firstOrNull { it.nombre == nombre }
    }
}

/** Define cómo leer las columnas de productos en la tabla del TPV. */
data class TpvMapeo(
    val tabla: String,
    val colNombre: String,
    val colPrecio: String,
    val colCategoria: String,
    val colDisponible: String? = null,
    val filtro: String? = null
)

internal fun normalizarNombre(s: String): String =
    s.lowercase().trim()
        .replace('á', 'a').replace('é', 'e').replace('í', 'i')
        .replace('ó', 'o').replace('ú', 'u')
        .replace('ü', 'u').replace('ñ', 'n')
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

/** Convierte una fila del TPV en un [Producto] según el [TpvMapeo]. null si no se puede mapear. */
fun mapFilaProducto(fila: Map<String, Any?>, mapeo: TpvMapeo): Producto? {
    val nombre = fila[mapeo.colNombre].texto() ?: return null
    if (nombre.isBlank()) return null

    val precio = fila[mapeo.colPrecio].aPrecio() ?: 0.0
    val categoria = fila[mapeo.colCategoria].texto() ?: "Sin categoría"
    val disponible = if (mapeo.colDisponible != null) {
        fila[mapeo.colDisponible].aBooleano()
    } else {
        true
    }
    return Producto(nombre = nombre, categoria = categoria, precio = precio, disponible = disponible)
}

private fun Any?.texto(): String? = when (this) {
    null -> null
    is String -> this
    else -> toString()
}

private fun Any?.aPrecio(): Double? = when (this) {
    null -> null
    is Number -> toDouble()
    is String -> toDoubleOrNull()
    else -> null
}

private fun Any?.aBooleano(): Boolean = when (this) {
    null -> true
    is Number -> toInt() != 0
    is String -> this == "1" || this.equals("true", ignoreCase = true) || this == "S"
    else -> true
}

data class FusionProductos(
    val insertar: List<Producto>,
    val actualizar: List<Producto>,
    val ignorados: Int
) {
    val insertados: Int get() = insertar.size
    val actualizados: Int get() = actualizar.size
}

/**
 * Fusiona los productos importados con los existentes: empareja por nombre
 * normalizado para actualizar precio/categoría/disponibilidad conservando el id.
 */
fun fusionarProductos(existentes: List<Producto>, importados: List<Producto>): FusionProductos {
    val porNombre = existentes.associateBy { normalizarNombre(it.nombre) }
    val insertar = mutableListOf<Producto>()
    val actualizar = mutableListOf<Producto>()
    var ignorados = 0

    for (nuevo in importados) {
        val clave = normalizarNombre(nuevo.nombre)
        if (clave.isBlank()) {
            ignorados++
            continue
        }
        val existente = porNombre[clave]
        if (existente == null) {
            insertar.add(nuevo.copy(id = 0))
        } else {
            actualizar.add(
                existente.copy(
                    nombre = nuevo.nombre,
                    categoria = nuevo.categoria,
                    precio = nuevo.precio,
                    disponible = nuevo.disponible
                )
            )
        }
    }
    return FusionProductos(insertar, actualizar, ignorados)
}
