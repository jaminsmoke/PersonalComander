package com.jaminsmoke.personalcomander.ui

import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.Producto
import com.jaminsmoke.personalcomander.data.normalizarNombre

/** Delegada a [normalizarNombre] en data — unifica la normalización de texto en un solo sitio. */
fun normalizar(texto: String): String = normalizarNombre(texto)

/** Distancia de Levenshtein (coste mínimo de ediciones) entre dos textos. */
fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    val dp = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        var prev = dp[0]
        dp[0] = i
        for (j in 1..b.length) {
            val temp = dp[j]
            dp[j] = minOf(
                dp[j] + 1,
                dp[j - 1] + 1,
                prev + if (a[i - 1] == b[j - 1]) 0 else 1
            )
            prev = temp
        }
    }
    return dp[b.length]
}

/* ── Números en texto (1–99) ── */
private val numerosTexto: Map<String, Int> = buildMap {
    // Unidades simples (0-9)
    put("cero", 0)
    for ((k, v) in listOf(
        "un" to 1, "una" to 1, "uno" to 1, "unas" to 1, "unos" to 1,
        "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9
    )) put(k, v)

    // 10-15 (irregulares + compuestos)
    for ((k, v) in listOf(
        "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13,
        "catorce" to 14, "quince" to 15
    )) put(k, v)

    // 16-19
    for ((k, v) in listOf(
        "dieciseis" to 16, "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19
    )) put(k, v)

    // 20-29 (veintiuno → 21, etc.)
    for ((k, v) in listOf(
        "veinte" to 20,
        "veintiuno" to 21, "veintiun" to 21, "veintidos" to 22, "veintitres" to 23,
        "veinticuatro" to 24, "veinticinco" to 25, "veintiseis" to 26,
        "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29
    )) put(k, v)

    // Decenas sueltas 30-90
    for ((k, v) in listOf(
        "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50,
        "sesenta" to 60, "setenta" to 70, "ochenta" to 80, "noventa" to 90
    )) put(k, v)

    // Cien
    put("cien", 100); put("ciento", 100)
}

/** Decenas que pueden combinarse con "y" + unidad (treinta y cinco = 35). */
private val decenasCompuestas = setOf(
    "veinte", "treinta", "cuarenta", "cincuenta",
    "sesenta", "setenta", "ochenta", "noventa"
)

/** Extrae un número compuesto estilo "treinta y cinco" que abarca 3 tokens. */
internal fun numeroCompuesto(tokens: List<String>, i: Int): Int? {
    if (i + 2 >= tokens.size) return null
    val decena = numerosTexto[tokens[i]] ?: return null
    if (tokens[i] !in decenasCompuestas) return null
    if (tokens[i + 1] != "y" && tokens[i + 1] != "e") return null
    val unidad = numerosTexto[tokens[i + 2]] ?: return null
    if (unidad < 1 || unidad > 9) return null
    return decena + unidad
}

internal val palabrasRelleno = setOf(
    "y", "e", "el", "la", "los", "las", "un", "una", "uno", "de", "del",
    "a", "al", "por", "para", "quiero", "quisiera", "me", "pongo",
    "pon", "ponme", "pongame", "trae", "traeme", "necesito", "tambien",
    "mas", "otra", "otro", "luego", "despues", "yadme", "deme",
    // Añadidas para hostelería y ruido ambiental
    "vale", "gracias", "porfa", "porfavor", "ademas", "ahora",
    "dame", "poner", "anadir", "anade", "anadime", "apunta", "apuntame",
    "vamos", "a", "ver", "pues", "entonces", "va", "venga", "bueno",
    "porfi", "todo", "nada", "ok", "valevale",
    "mira", "oye", "eh", "umm", "esto", "ahi"
)

// ── Tipos de datos ──

data class LineaVoz(val producto: Producto, val cantidad: Int)

data class ResultadoVoz(
    val lineas: List<LineaVoz>,
    val noEntendido: List<String>
)

sealed class AccionVoz {
    data class Anadir(val texto: String) : AccionVoz()
    data class Quitar(val texto: String) : AccionVoz()
}

data class LineaQuitar(val nombreProducto: String, val cantidad: Int)

data class ResultadoQuitar(
    val lineas: List<LineaQuitar>,
    val noEntendido: List<String>
)

// ── Keywords ──

/** Palabras clave para quitar productos (única acción que requiere keyword explícita). */
private val keywordsQuitar = setOf(
    "quita", "borra", "elimina", "saca", "quitar", "borrar", "eliminar",
    "retira", "retirar", "tacha", "anula", "anular"
)

// ── Funciones públicas del parser ──

/**
 * Extrae la acción del texto.
 * - Si empieza con keyword de quitar → Quitar
 * - En cualquier otro caso → Anadir (sin requerir keyword)
 * - Nunca devuelve null: siempre se intenta añadir por defecto
 */
fun extraerAccion(texto: String): AccionVoz {
    val norm = normalizar(texto).trim()
    for (kw in keywordsQuitar) {
        if (norm.startsWith("$kw ")) return AccionVoz.Quitar(norm.removePrefix("$kw ").trim())
        if (norm == kw) return AccionVoz.Quitar("")
    }
    // Sin keyword de quitar → siempre intentar añadir
    return AccionVoz.Anadir(norm)
}

/**
 * Convierte una comanda hablada en líneas de productos.
 * Ej.: "dos cafés con leche y una tarta de queso" ->
 *      [(Café con leche, 2), (Tarta de queso, 1)]
 */
fun parsearComanda(texto: String, productos: List<Producto>): ResultadoVoz {
    val tokens = normalizar(texto).split(" ")
    if (tokens.all { it.isEmpty() }) return ResultadoVoz(emptyList(), emptyList())

    val productosTokens = productos.map { p -> normalizar(p.nombre).split(" ") to p }

    val lineas = mutableListOf<LineaVoz>()
    val noEntendido = mutableListOf<String>()
    var i = 0
    var qty = 1

    while (i < tokens.size) {
        val tok = tokens[i]

        val compuesto = numeroCompuesto(tokens, i)
        if (compuesto != null) { qty = compuesto; i += 3; continue }

        val numero = tok.toIntOrNull() ?: numerosTexto[tok]
        if (numero != null) { qty = numero; i++; continue }

        val matchExacto = buscarExactoGen(tokens, i, productosTokens)
        if (matchExacto != null) {
            lineas.add(LineaVoz(matchExacto.second, qty))
            qty = 1; i += matchExacto.first; continue
        }

        val matchDifuso = buscarDifusoGen(tokens, i, productosTokens)
        if (matchDifuso != null) {
            lineas.add(LineaVoz(matchDifuso.second, qty))
            qty = 1; i += matchDifuso.first; continue
        }

        if (tok in palabrasRelleno) { i++; continue }
        noEntendido.add(tok)
        i++
    }
    return ResultadoVoz(lineas, noEntendido)
}

/**
 * Convierte un comando de quitar hablado en líneas a eliminar.
 * Empareja contra los nombres de productos ya existentes en la comanda.
 * Ej.: "quita un café con leche" -> [("Café con leche", 1)]
 */
fun parsearQuitar(texto: String, lineas: List<LineaPedido>): ResultadoQuitar {
    val tokens = normalizar(texto).split(" ")
    if (tokens.all { it.isEmpty() }) return ResultadoQuitar(emptyList(), emptyList())

    val lineaTokens = lineas.map { l -> normalizar(l.nombreProducto).split(" ") to l.nombreProducto }

    val quitadas = mutableListOf<LineaQuitar>()
    val noEntendido = mutableListOf<String>()
    var i = 0
    var qty = 1

    while (i < tokens.size) {
        val tok = tokens[i]

        val compuesto = numeroCompuesto(tokens, i)
        if (compuesto != null) { qty = compuesto; i += 3; continue }

        val numero = tok.toIntOrNull() ?: numerosTexto[tok]
        if (numero != null) { qty = numero; i++; continue }

        val matchExacto = buscarExactoGen(tokens, i, lineaTokens)
        if (matchExacto != null) {
            quitadas.add(LineaQuitar(matchExacto.second, qty))
            qty = 1; i += matchExacto.first; continue
        }

        val matchDifuso = buscarDifusoGen(tokens, i, lineaTokens)
        if (matchDifuso != null) {
            quitadas.add(LineaQuitar(matchDifuso.second, qty))
            qty = 1; i += matchDifuso.first; continue
        }

        if (tok in palabrasRelleno) { i++; continue }
        noEntendido.add(tok)
        i++
    }
    return ResultadoQuitar(quitadas, noEntendido)
}

/**
 * Devuelve un score de coincidencia para la búsqueda (menor = mejor).
 * null = sin coincidencia.
 */
fun coincidenciaBusqueda(query: String, producto: Producto): Int? {
    val q = normalizar(query)
    if (q.isEmpty()) return 0
    val nombre = normalizar(producto.nombre)
    val categoria = normalizar(producto.categoria)

    if (nombre.startsWith(q) || categoria.startsWith(q)) return 0
    if (nombre.contains(q) || categoria.contains(q) || q.contains(nombre)) return 1

    val qTokens = q.split(" ")
    val pTokens = nombre.split(" ")
    var dist = 0
    for (t in qTokens) {
        val min = pTokens.minOfOrNull { levenshtein(t, it) } ?: return null
        if (min > 3) return null
        dist += min
    }
    return 2 + dist
}

// ── Funciones genéricas de búsqueda ──

/** Empareja desde i el item contiguo más largo (coincidencia exacta de tokens). */
internal fun <T> buscarExactoGen(
    tokens: List<String>, i: Int,
    items: List<Pair<List<String>, T>>
): Pair<Int, T>? {
    var mejor: Pair<Int, T>? = null
    for ((tokensItem, item) in items) {
        if (i + tokensItem.size > tokens.size) continue
        if (tokens.subList(i, i + tokensItem.size) == tokensItem) {
            if (mejor == null || tokensItem.size > mejor.first) {
                mejor = tokensItem.size to item
            }
        }
    }
    return mejor
}

/**
 * Empareja desde i permitiendo errores de voz (plurales, palabras pegadas, erratas).
 * Tolerancia: estricta para 1 token, más permisiva para nombres compuestos.
 */
internal fun <T> buscarDifusoGen(
    tokens: List<String>, i: Int,
    items: List<Pair<List<String>, T>>
): Pair<Int, T>? {
    var mejor: Pair<Int, T>? = null
    var mejorDist = Int.MAX_VALUE
    var mejorLen = 0
    for ((tokensItem, item) in items) {
        val maxLen = minOf(tokensItem.size, tokens.size - i)
        val objetivoFull = tokensItem.joinToString(" ")
        for (len in 1..maxLen) {
            val sub = tokens.subList(i, i + len).joinToString(" ")
            val subSingular = if (sub.length > 1 && sub.endsWith("s")) sub.dropLast(1) else sub
            val objetivo = tokensItem.take(len).joinToString(" ")
            val d = minOf(
                levenshtein(sub, objetivo),
                levenshtein(sub, objetivoFull),
                levenshtein(subSingular, objetivoFull)
            )
            val tolerancia = if (len == 1) 1 else len + 1
            if (d <= tolerancia && (d < mejorDist || (d == mejorDist && len > mejorLen))) {
                mejorDist = d; mejorLen = len; mejor = len to item
            }
        }
    }
    return mejor
}
