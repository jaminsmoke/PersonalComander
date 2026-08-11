package com.jaminsmoke.personalcomander.data

import com.google.gson.Gson

data class BackupProducto(
    val nombre: String,
    val categoria: String,
    val precio: Double,
    val disponible: Boolean
)

data class BackupFormato(
    val app: String = "personalcomander",
    val formato: Int = 1,
    val productos: List<BackupProducto>
)

object BackupJson {

    private val gson = Gson()

    fun serializar(productos: List<Producto>): String {
        val backup = BackupFormato(
            productos = productos.map {
                BackupProducto(
                    nombre = it.nombre,
                    categoria = it.categoria,
                    precio = it.precio,
                    disponible = it.disponible
                )
            }
        )
        return gson.toJson(backup)
    }

    fun deserializar(texto: String): List<Producto>? {
        val backup = runCatching {
            gson.fromJson(texto, BackupFormato::class.java)
        }.getOrNull() ?: return null
        if (backup.productos.isNullOrEmpty()) return emptyList()

        return backup.productos.map {
            Producto(
                nombre = it.nombre,
                categoria = it.categoria,
                precio = it.precio,
                disponible = it.disponible
            )
        }
    }
}
