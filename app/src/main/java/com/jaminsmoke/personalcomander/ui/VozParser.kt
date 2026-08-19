package com.jaminsmoke.personalcomander.ui

import com.jaminsmoke.personalcomander.data.CartaModificadores
import com.jaminsmoke.personalcomander.data.GrupoConOpciones
import com.jaminsmoke.personalcomander.data.GrupoModificador
import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.ModificadorElegido
import com.jaminsmoke.personalcomander.data.OpcionModificador
import com.jaminsmoke.personalcomander.data.Producto
import com.jaminsmoke.personalcomander.data.modificadores
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
    "mira", "oye", "eh", "umm", "esto", "ahi",
    // Sala / babble: no son productos
    "cuenta", "calor", "ronda", "aqui", "casa", "favor",
    "personas", "gente", "mesa",
)

internal fun esCantidad(tok: String): Boolean =
    tok.toIntOrNull() != null || numerosTexto.containsKey(tok)

/**
 * "para cuatro" = cubiertos, no "Pizza Cuatro Quesos".
 * Si después del número viene un producto ("para cuatro pizzas"), solo se salta "para"
 * y el número queda como cantidad.
 * @return nuevo índice, o null si no hay idioma de sala.
 */
internal fun indiceTrasParaComensales(
    tokens: List<String>,
    i: Int,
    hayProductoDesde: (Int) -> Boolean,
): Int? {
    if (tokens[i] != "para" || i + 1 >= tokens.size || !esCantidad(tokens[i + 1])) return null
    val after = i + 2
    if (after >= tokens.size || !hayProductoDesde(after)) return after
    return i + 1
}

// ── Tipos de datos ──

data class LineaVoz(
    val producto: Producto,
    val cantidad: Int,
    val modificadores: List<ModificadorElegido> = emptyList(),
    val nota: String? = null,
    val faltaObligatorio: Boolean = false,
)

data class CartaVoz(
    val productos: List<Producto>,
    val gruposPorProducto: Map<Long, List<GrupoConOpciones>> = emptyMap(),
)

data class ResultadoVoz(
    val lineas: List<LineaVoz>,
    val noEntendido: List<String>
)

sealed class AccionVoz {
    data class Anadir(val texto: String) : AccionVoz()
    data class Quitar(val texto: String) : AccionVoz()
}

data class LineaQuitar(
    val nombreProducto: String,
    val cantidad: Int,
    val modificadoresJson: String = "[]",
)

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

fun parsearComanda(texto: String, productos: List<Producto>): ResultadoVoz =
    parsearComanda(texto, CartaVoz(productos))

/**
 * Convierte una comanda hablada en líneas de productos.
 * Ej.: "dos cafés con leche y una tarta de queso" ->
 *      [(Café con leche, 2), (Tarta de queso, 1)]
 * Tras el SKU consume opciones de modificador («al punto») y, si cabe, nota.
 */
fun parsearComanda(texto: String, carta: CartaVoz): ResultadoVoz {
    val tokens = normalizar(texto).split(" ").filter { it.isNotEmpty() }
    if (tokens.all { it.isEmpty() }) return ResultadoVoz(emptyList(), emptyList())

    val productosTokens = carta.productos.map { p -> normalizar(p.nombre).split(" ") to p }
    val colaMod = primerosTokensOpcion(carta)

    val lineas = mutableListOf<LineaVoz>()
    val noEntendido = mutableListOf<String>()
    var i = 0
    var qty = 1
    var ultimoProducto: Producto? = null

    while (i < tokens.size) {
        val tok = tokens[i]

        indiceTrasParaComensales(tokens, i) { j ->
            buscarExactoGen(tokens, j, productosTokens) != null ||
                buscarDifusoGen(tokens, j, productosTokens, colaMod) != null
        }?.let { i = it; continue }

        val compuesto = numeroCompuesto(tokens, i)
        if (compuesto != null) { qty = compuesto; i += 3; continue }

        val numero = tok.toIntOrNull() ?: numerosTexto[tok]
        if (numero != null) { qty = numero; i++; continue }

        val matchExacto = buscarExactoGen(tokens, i, productosTokens)
        val matchDifuso = matchExacto ?: buscarDifusoGen(tokens, i, productosTokens, colaMod)
        if (matchDifuso != null) {
            val producto = matchDifuso.second
            i += matchDifuso.first
            ultimoProducto = producto
            i = anadirLineaVoz(lineas, noEntendido, carta, tokens, i, producto, qty, productosTokens, colaMod)
            qty = 1
            continue
        }

        val previo = ultimoProducto
        if (previo != null) {
            val grupos = carta.gruposPorProducto[previo.id].orEmpty()
            val cola = consumirColaModificadores(
                tokens, i, grupos, previo.permiteNota, productosTokens, colaMod,
            )
            if (cola.elegidos.isNotEmpty() || cola.nota != null) {
                noEntendido.addAll(cola.noEntendido)
                lineas.add(
                    LineaVoz(
                        producto = previo,
                        cantidad = qty,
                        modificadores = cola.elegidos,
                        nota = cola.nota,
                        faltaObligatorio = CartaModificadores.faltanObligatorios(grupos, cola.elegidos),
                    ),
                )
                i = cola.indice
                qty = 1
                continue
            }
        }

        if (tok in palabrasRelleno) { i++; continue }
        noEntendido.add(tok)
        i++
    }
    return ResultadoVoz(lineas, noEntendido)
}

private fun anadirLineaVoz(
    lineas: MutableList<LineaVoz>,
    noEntendido: MutableList<String>,
    carta: CartaVoz,
    tokens: List<String>,
    start: Int,
    producto: Producto,
    qty: Int,
    productosTokens: List<Pair<List<String>, Producto>>,
    colaMod: Set<String>,
): Int {
    val grupos = carta.gruposPorProducto[producto.id].orEmpty()
    val cola = consumirColaModificadores(
        tokens, start, grupos, producto.permiteNota, productosTokens, colaMod,
    )
    noEntendido.addAll(cola.noEntendido)
    lineas.add(
        LineaVoz(
            producto = producto,
            cantidad = qty,
            modificadores = cola.elegidos,
            nota = cola.nota,
            faltaObligatorio = CartaModificadores.faltanObligatorios(grupos, cola.elegidos),
        ),
    )
    return cola.indice
}

private fun primerosTokensOpcion(carta: CartaVoz): Set<String> =
    carta.gruposPorProducto.values.flatten()
        .flatMap { gc -> gc.opciones }
        .flatMap { CartaModificadores.tokensOpcion(it) }
        .mapNotNull { it.firstOrNull() }
        .toSet()

private data class ColaModificadores(
    val indice: Int,
    val elegidos: List<ModificadorElegido>,
    val nota: String?,
    val noEntendido: List<String>,
)

private fun hayProductoOCantidadDesde(
    tokens: List<String>,
    i: Int,
    productosTokens: List<Pair<List<String>, Producto>>,
    colaMod: Set<String> = emptySet(),
): Boolean {
    if (i >= tokens.size) return false
    if (esCantidad(tokens[i]) || numeroCompuesto(tokens, i) != null) return true
    if (productosTokens.isEmpty()) return false
    return buscarExactoGen(tokens, i, productosTokens) != null ||
        buscarDifusoGen(tokens, i, productosTokens, colaMod) != null
}

internal fun candidatosOpcion(
    grupos: List<GrupoConOpciones>,
    usados: Set<Long>,
): List<Pair<List<String>, Pair<GrupoConOpciones, OpcionModificador>>> {
    val out = mutableListOf<Pair<List<String>, Pair<GrupoConOpciones, OpcionModificador>>>()
    for (gc in grupos) {
        if (!gc.grupo.multiple && gc.grupo.id in usados) continue
        for (op in gc.opciones) {
            for (toks in CartaModificadores.tokensOpcion(op)) {
                out.add(toks to (gc to op))
            }
        }
    }
    return out
}

private fun consumirColaModificadores(
    tokens: List<String>,
    start: Int,
    grupos: List<GrupoConOpciones>,
    permiteNota: Boolean,
    productosTokens: List<Pair<List<String>, Producto>>,
    colaMod: Set<String> = emptySet(),
): ColaModificadores {
    var i = start
    val elegidos = mutableListOf<ModificadorElegido>()
    val usados = mutableSetOf<Long>()
    val noEntendido = mutableListOf<String>()
    while (i < tokens.size) {
        if (hayProductoOCantidadDesde(tokens, i, productosTokens, colaMod)) break
        if (tokens[i] in palabrasRelleno) { i++; continue }
        val cands = candidatosOpcion(grupos, usados)
        if (cands.isEmpty()) break
        val match = buscarExactoGen(tokens, i, cands) ?: buscarDifusoGen(tokens, i, cands)
        if (match == null) break
        val (gc, op) = match.second
        elegidos.add(
            ModificadorElegido(
                grupoId = gc.grupo.id,
                grupoNombre = gc.grupo.nombre,
                opcionId = op.id,
                opcionNombre = op.nombre,
                deltaPrecio = op.deltaPrecio,
            ),
        )
        if (!gc.grupo.multiple) usados.add(gc.grupo.id)
        i += match.first
    }
    val notaTokens = mutableListOf<String>()
    while (i < tokens.size && !hayProductoOCantidadDesde(tokens, i, productosTokens, colaMod)) {
        val t = tokens[i]
        if (t in palabrasRelleno) { i++; continue }
        if (permiteNota) notaTokens.add(t) else noEntendido.add(t)
        i++
    }
    val nota = notaTokens.joinToString(" ").trim().takeIf { it.isNotEmpty() }
    return ColaModificadores(i, elegidos, nota, noEntendido)
}

/**
 * Convierte un comando de quitar hablado en líneas a eliminar.
 * Empareja contra los nombres de productos ya existentes en la comanda.
 * Ej.: "quita un café con leche" -> [("Café con leche", 1)]
 */
fun parsearQuitar(texto: String, lineas: List<LineaPedido>): ResultadoQuitar {
    val tokens = normalizar(texto).split(" ").filter { it.isNotEmpty() }
    if (tokens.all { it.isEmpty() }) return ResultadoQuitar(emptyList(), emptyList())

    val lineaTokens = lineas.map { l -> normalizar(l.nombreProducto).split(" ") to l }

    val quitadas = mutableListOf<LineaQuitar>()
    val noEntendido = mutableListOf<String>()
    var i = 0
    var qty = 1

    while (i < tokens.size) {
        val tok = tokens[i]

        indiceTrasParaComensales(tokens, i) { j ->
            buscarExactoGen(tokens, j, lineaTokens) != null ||
                buscarDifusoGen(tokens, j, lineaTokens) != null
        }?.let { i = it; continue }

        val compuesto = numeroCompuesto(tokens, i)
        if (compuesto != null) { qty = compuesto; i += 3; continue }

        val numero = tok.toIntOrNull() ?: numerosTexto[tok]
        if (numero != null) { qty = numero; i++; continue }

        val matchExacto = buscarExactoGen(tokens, i, lineaTokens)
        val matchDifuso = matchExacto ?: buscarDifusoGen(tokens, i, lineaTokens)
        if (matchDifuso != null) {
            val linea = matchDifuso.second
            i += matchDifuso.first
            val gruposLinea = linea.modificadores().map { e ->
                GrupoConOpciones(
                    GrupoModificador(id = e.grupoId, nombre = e.grupoNombre, multiple = true),
                    listOf(
                        OpcionModificador(
                            id = e.opcionId,
                            grupoId = e.grupoId,
                            nombre = e.opcionNombre,
                            deltaPrecio = e.deltaPrecio,
                        ),
                    ),
                )
            }
            val cola = consumirColaModificadores(tokens, i, gruposLinea, false, emptyList())
            i = cola.indice
            val json = if (cola.elegidos.isEmpty()) {
                linea.modificadoresJson
            } else {
                CartaModificadores.canonicalJson(cola.elegidos)
            }
            quitadas.add(LineaQuitar(linea.nombreProducto, qty, json))
            qty = 1
            continue
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
    val subfamilia = normalizar(producto.subfamilia.orEmpty())

    if (nombre.startsWith(q) || categoria.startsWith(q) || (subfamilia.isNotEmpty() && subfamilia.startsWith(q))) return 0
    if (nombre.contains(q) || categoria.contains(q) || q.contains(nombre) ||
        (subfamilia.isNotEmpty() && (subfamilia.contains(q) || q.contains(subfamilia)))
    ) return 1

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
 * Rechaza matches parciales espurios: si el producto tiene más tokens y quedan tokens
 * en el input, el siguiente token debe continuar plausiblemente el nombre del producto.
 * Esto evita que "café capuccino" matchee "Café solo".
 */
internal fun <T> buscarDifusoGen(
    tokens: List<String>, i: Int,
    items: List<Pair<List<String>, T>>,
    colaExtra: Set<String> = emptySet(),
): Pair<Int, T>? {
    var mejor: Pair<Int, T>? = null
    var mejorDist = Int.MAX_VALUE
    var mejorLen = 0
    for ((tokensItem, item) in items) {
        // Consumir todos los tokens del producto (o los que queden si son menos)
        var len = minOf(tokensItem.size, tokens.size - i)
        // "hamburguesas para cuatro": no comer "para"/cantidad como si fueran el resto del nombre
        val headIn = tokens[i].let { t -> if (t.length > 1 && t.endsWith("s")) t.dropLast(1) else t }
        val headOk = levenshtein(headIn, tokensItem.first()) <= 1 || levenshtein(tokens[i], tokensItem.first()) <= 1
        while (len > 1 && headOk) {
            val last = tokens[i + len - 1]
            val prev = tokens[i + len - 2]
            val colaSala = last in palabrasRelleno || last in colaExtra ||
                (esCantidad(last) && prev == "para")
            val lastProd = tokensItem.getOrNull(len - 1)
            val lastSing = if (last.length > 1 && last.endsWith("s")) last.dropLast(1) else last
            val lastNoEsProducto = lastProd != null &&
                levenshtein(last, lastProd) > 1 &&
                levenshtein(lastSing, lastProd) > 1
            if (!colaSala && !lastNoEsProducto) break
            len--
        }

        // Si el match es parcial y quedan más tokens, verificar que continúen el producto
        if (len < tokensItem.size && i + len < tokens.size) {
            val nextInput = tokens[i + len]
            val nextProduct = tokensItem[len]
            val nextEsSala = nextInput in palabrasRelleno || nextInput in colaExtra ||
                (nextInput == "para" && i + len + 1 < tokens.size && esCantidad(tokens[i + len + 1]))
            if (!nextEsSala && levenshtein(nextInput, nextProduct) > 2) {
                continue // Match parcial espurio: el siguiente token no pertenece a este producto
            }
        }

        val sub = tokens.subList(i, i + len).joinToString(" ")
        val subSingular = if (sub.length > 1 && sub.endsWith("s")) sub.dropLast(1) else sub
        val objetivoFull = tokensItem.joinToString(" ")
        val objetivo = tokensItem.take(len).joinToString(" ")
        val d = minOf(
            levenshtein(sub, objetivo),
            levenshtein(sub, objetivoFull),
            levenshtein(subSingular, objetivoFull)
        )
        val tolerancia = if (len == 1) 1 else len + 1
        if (d > tolerancia) continue
        if (!cubreNombreProducto(tokens.subList(i, i + len), tokensItem)) continue
        if (d < mejorDist || (d == mejorDist && len > mejorLen)) {
            mejorDist = d; mejorLen = len; mejor = len to item
        }
    }
    return mejor
}

/**
 * Un producto de 2+ palabras de contenido no puede matchear solo con un número o
 * relleno ("para cuatro" ↛ Pizza Cuatro Quesos). Excepciones: el token es la
 * cabeza del nombre ("pizza", "croquetas") o un pegado tipo "cocacolas".
 */
internal fun cubreNombreProducto(input: List<String>, producto: List<String>): Boolean {
    val prodContent = producto.filter { it !in palabrasRelleno }
    val inputContent = input.filter { it !in palabrasRelleno }
    if (prodContent.size < 2) return true
    if (inputContent.size >= 2) return true
    val tok = inputContent.singleOrNull() ?: return false
    val tokSing = if (tok.length > 1 && tok.endsWith("s")) tok.dropLast(1) else tok
    val head = prodContent.first()
    if (levenshtein(tok, head) <= 1 || levenshtein(tokSing, head) <= 1) return true
    val concat = producto.joinToString("")
    val concatContent = prodContent.joinToString("")
    return levenshtein(tok, concat) <= 1 ||
        levenshtein(tokSing, concat) <= 1 ||
        levenshtein(tok, concatContent) <= 1 ||
        levenshtein(tokSing, concatContent) <= 1
}
