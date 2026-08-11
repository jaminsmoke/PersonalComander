package com.jaminsmoke.personalcomander.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.jaminsmoke.personalcomander.R
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

private val numerosTexto = mapOf(
    "un" to 1, "una" to 1, "uno" to 1, "unas" to 1, "unos" to 1,
    "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
    "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10,
    "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14, "quince" to 15
)

private val palabrasRelleno = setOf(
    "y", "e", "el", "la", "los", "las", "un", "una", "uno", "de", "del",
    "a", "al", "por", "para", "quiero", "quisiera", "me", "pongo",
    "pon", "ponme", "pongame", "trae", "traeme", "necesito", "tambien",
    "mas", "otra", "otro", "luego", "despues", "yadme", "deme"
)

data class LineaVoz(val producto: Producto, val cantidad: Int)

data class ResultadoVoz(
    val lineas: List<LineaVoz>,
    val noEntendido: List<String>
)

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

        val numero = tok.toIntOrNull() ?: numerosTexto[tok]
        if (numero != null) {
            qty = numero
            i++
            continue
        }

        val matchExacto = buscarExacto(tokens, i, productosTokens)
        if (matchExacto != null) {
            lineas.add(LineaVoz(matchExacto.second, qty))
            qty = 1
            i += matchExacto.first
            continue
        }

        val matchDifuso = buscarDifuso(tokens, i, productosTokens)
        if (matchDifuso != null) {
            lineas.add(LineaVoz(matchDifuso.second, qty))
            qty = 1
            i += matchDifuso.first
            continue
        }

        if (tok in palabrasRelleno) {
            i++
            continue
        }
        noEntendido.add(tok)
        i++
    }
    return ResultadoVoz(lineas, noEntendido)
}

/** Empareja desde i el nombre de producto contiguo más largo (coincidencia exacta de tokens). */
private fun buscarExacto(
    tokens: List<String>,
    i: Int,
    productosTokens: List<Pair<List<String>, Producto>>
): Pair<Int, Producto>? {
    var mejor: Pair<Int, Producto>? = null
    for ((tokensProd, p) in productosTokens) {
        if (i + tokensProd.size > tokens.size) continue
        if (tokens.subList(i, i + tokensProd.size) == tokensProd) {
            if (mejor == null || tokensProd.size > mejor.first) {
                mejor = tokensProd.size to p
            }
        }
    }
    return mejor
}

/**
 * Empareja desde i permitiendo errores de voz (plurales, palabras pegadas, erratas).
 * Tolerancia aumentada para ambientes con ruido: len + 1 (más permisivo).
 * Ante empate de distancia prefiere consumir más tokens (la comanda más larga).
 */
private fun buscarDifuso(
    tokens: List<String>,
    i: Int,
    productosTokens: List<Pair<List<String>, Producto>>
): Pair<Int, Producto>? {
    var mejor: Pair<Int, Producto>? = null
    var mejorDist = Int.MAX_VALUE
    var mejorLen = 0
    for ((tokensProd, p) in productosTokens) {
        val maxLen = minOf(tokensProd.size, tokens.size - i)
        val objetivoFull = tokensProd.joinToString(" ")
        for (len in 1..maxLen) {
            val sub = tokens.subList(i, i + len).joinToString(" ")
            val subSingular = if (sub.length > 1 && sub.endsWith("s")) {
                sub.dropLast(1)
            } else {
                sub
            }
            val objetivo = tokensProd.take(len).joinToString(" ")
            val d = minOf(
                levenshtein(sub, objetivo),
                levenshtein(sub, objetivoFull),
                levenshtein(subSingular, objetivoFull)
            )
            // Tolerancia aumentada: len + 1 para ambientes ruidosos
            val tolerancia = len + 1
            if (d <= tolerancia && (d < mejorDist || (d == mejorDist && len > mejorLen))) {
                mejorDist = d
                mejorLen = len
                mejor = len to p
            }
        }
    }
    return mejor
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
        // Tolerancia aumentada para búsqueda manual en entorno ruidoso
        val min = pTokens.minOfOrNull { levenshtein(t, it) } ?: return null
        if (min > 3) return null
        dist += min
    }
    return 2 + dist
}

/** Envuelve SpeechRecognizer optimizado para ambientes con ruido de bar/restaurante. */
class VozRecognizer(private val appContext: Context) {
    private val speech: SpeechRecognizer? = SpeechRecognizer.createSpeechRecognizer(appContext)
    private var activo = false
    private var reintentos = 0
    private val maxReintentos = 1
    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        if (activo) {
            activo = false
            runCatching { speech?.cancel() }
            onError?.invoke(SpeechRecognizer.ERROR_NO_MATCH)
        }
    }

    var onResultado: ((String) -> Unit)? = null
    var onParcial: ((String) -> Unit)? = null
    var onError: ((Int) -> Unit)? = null

    init {
        speech?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() { reintentos = 0 }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                handler.removeCallbacks(timeoutRunnable)
                activo = false
                // Auto-retry en ambientes ruidosos: si no se entendió, reintentar una vez
                if (error == SpeechRecognizer.ERROR_NO_MATCH && reintentos < maxReintentos) {
                    reintentos++
                    handler.postDelayed({ empezar() }, 300)
                    return
                }
                onError?.invoke(error)
            }

            override fun onResults(results: Bundle?) {
                handler.removeCallbacks(timeoutRunnable)
                activo = false
                val texto = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!texto.isNullOrBlank()) {
                    onResultado?.invoke(texto)
                } else {
                    onError?.invoke(SpeechRecognizer.ERROR_NO_MATCH)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val parcial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!parcial.isNullOrBlank()) {
                    onParcial?.invoke(parcial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun empezar(idioma: String = "es-ES") {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError?.invoke(SpeechRecognizer.ERROR_CLIENT)
            return
        }
        if (activo || speech == null) {
            if (speech == null) onError?.invoke(SpeechRecognizer.ERROR_CLIENT)
            return
        }
        activo = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, idioma)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, idioma)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Optimizaciones para ambientes ruidosos
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Silencio más corto = captura más rápida en entornos con ruido de fondo
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }
        try {
            speech.startListening(intent)
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed(timeoutRunnable, 15000)
        } catch (e: Exception) {
            handler.removeCallbacks(timeoutRunnable)
            activo = false
            onError?.invoke(SpeechRecognizer.ERROR_CLIENT)
        }
    }

    fun detener() {
        handler.removeCallbacks(timeoutRunnable)
        if (activo) speech?.stopListening()
    }

    fun destruir() {
        handler.removeCallbacks(timeoutRunnable)
        speech?.destroy()
    }
}

fun mensajeErrorVoz(context: android.content.Context?, error: Int): String {
    val c = context ?: return "Voice error ($error)"
    return when (error) {
        SpeechRecognizer.ERROR_AUDIO -> c.getString(R.string.voice_error_audio)
        SpeechRecognizer.ERROR_NETWORK -> c.getString(R.string.voice_error_network)
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> c.getString(R.string.voice_error_network_timeout)
        SpeechRecognizer.ERROR_NO_MATCH -> c.getString(R.string.voice_error_no_match)
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> c.getString(R.string.voice_error_permissions)
        SpeechRecognizer.ERROR_CLIENT -> c.getString(R.string.voice_error_unavailable)
        else -> c.getString(R.string.voice_error_generic, error)
    }
}
