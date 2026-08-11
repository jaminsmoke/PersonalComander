package com.jaminsmoke.personalcomander.data

/**
 * Asigna un emoji representativo a cada categoría de producto.
 * Usa normalización para ser tolerante a variaciones (mayúsculas, tildes).
 * Si no encuentra match, devuelve 📋 por defecto.
 */
object CategoriaIcono {

    private val mapa = mapOf(
        "entrantes" to "🥗", "entrante" to "🥗", "aperitivos" to "🥗",
        "ensaladas" to "🥬", "ensalada" to "🥬",
        "pizzas" to "🍕", "pizza" to "🍕",
        "burgers" to "🍔", "hamburguesas" to "🍔", "burger" to "🍔",
        "carnes" to "🥩", "carne" to "🥩", "parrilla" to "🥩",
        "pescados" to "🐟", "pescado" to "🐟", "mariscos" to "🦐",
        "bebidas" to "🍺", "bebida" to "🍺", "refrescos" to "🥤",
        "cafes" to "☕", "cafetería" to "☕", "cafeteria" to "☕",
        "postres" to "🍰", "postre" to "🍰",
        "arroces" to "🍚", "pasta" to "🍝", "pastas" to "🍝",
        "sandwich" to "🥪", "bocadillos" to "🥪", "bocadillo" to "🥪",
        "vinos" to "🍷", "vino" to "🍷",
        "copas" to "🍸", "cocteles" to "🍹"
    )

    fun de(categoria: String): String {
        val clave = categoria.lowercase().trim()
            .replace('á', 'a').replace('é', 'e').replace('í', 'i')
            .replace('ó', 'o').replace('ú', 'u')
        return mapa[clave] ?: "📋"
    }
}
