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

/** Umbral de RMS (dB) por debajo del cual se considera ruido lejano, no voz del camarero. */
const val RMS_UMBRAL_CERCANIA = 6.0f

/** Envuelve SpeechRecognizer optimizado para ambientes con ruido de bar/restaurante. */
class VozRecognizer(private val appContext: Context) {

    companion object {
        /** Timeout inicial: si en este tiempo no hay resultados ni RMS alto, se cancela. */
        private const val BASE_TIMEOUT_MS = 15_000L
        /** Timeout extendido cuando se ha detectado voz (RMS alto): da margen para frases largas. */
        private const val VOICE_TIMEOUT_MS = 30_000L
        /** Máximo absoluto de escucha: corta pase lo que pase. */
        private const val ABSOLUTE_MAX_MS = 45_000L
    }

    private val speech: SpeechRecognizer? = SpeechRecognizer.createSpeechRecognizer(appContext)
    private var activo = false
    private val handler = Handler(Looper.getMainLooper())
    private var rmsMax = 0f
    private var inicioMs = 0L
    private var vozDetectada = false

    /** true si el nivel máximo de voz captado sugiere un camarero cerca del dispositivo. */
    val vozCercana: Boolean get() = rmsMax >= RMS_UMBRAL_CERCANIA

    var onResultado: ((String) -> Unit)? = null
    var onParcial: ((String) -> Unit)? = null
    var onError: ((Int) -> Unit)? = null
    var onRms: ((Float) -> Unit)? = null

    private val timeoutRunnable = object : Runnable {
        override fun run() {
            if (!activo) return
            val elapsed = System.currentTimeMillis() - inicioMs

            // Si detectamos voz, dar más tiempo (VOICE_TIMEOUT)
            if (vozDetectada && elapsed < VOICE_TIMEOUT_MS) {
                handler.postDelayed(this, 2_000)
                return
            }

            // Si es el timeout base y hay voz → extender
            if (!vozDetectada && elapsed >= BASE_TIMEOUT_MS && elapsed < ABSOLUTE_MAX_MS) {
                handler.postDelayed(this, 2_000) // seguir chequeando
                return
            }

            // Timeout absoluto o sin voz → cancelar
            activo = false
            handler.removeCallbacks(this)
            // Si hubo voz, dejar que Android entregue resultados (stopListening)
            if (vozDetectada) {
                runCatching { speech?.stopListening() }
            } else {
                runCatching { speech?.cancel() }
                onError?.invoke(SpeechRecognizer.ERROR_NO_MATCH)
            }
        }
    }

    init {
        speech?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                if (rmsdB > rmsMax) rmsMax = rmsdB
                if (rmsdB >= RMS_UMBRAL_CERCANIA) vozDetectada = true
                onRms?.invoke(rmsdB)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                // Android detectó silencio natural → dejar que termine
                if (activo) {
                    handler.removeCallbacks(timeoutRunnable)
                }
            }

            override fun onError(error: Int) {
                handler.removeCallbacks(timeoutRunnable)
                activo = false
                // Sin auto-reintento: reportar el error y parar
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
        rmsMax = 0f
        vozDetectada = false
        inicioMs = System.currentTimeMillis()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, idioma)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, idioma)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Más permisivo con el silencio en ambientes ruidosos
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        try {
            speech.startListening(intent)
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed(timeoutRunnable, BASE_TIMEOUT_MS)
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
