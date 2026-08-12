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
    private val speech: SpeechRecognizer? = SpeechRecognizer.createSpeechRecognizer(appContext)
    private var activo = false
    private var reintentos = 0
    private val maxReintentos = 1
    private val handler = Handler(Looper.getMainLooper())
    private var rmsMax = 0f
    private val timeoutRunnable = Runnable {
        if (activo) {
            activo = false
            runCatching { speech?.cancel() }
            onError?.invoke(SpeechRecognizer.ERROR_NO_MATCH)
        }
    }

    /** true si el nivel máximo de voz captado sugiere un camarero cerca del dispositivo. */
    val vozCercana: Boolean get() = rmsMax >= RMS_UMBRAL_CERCANIA

    var onResultado: ((String) -> Unit)? = null
    var onParcial: ((String) -> Unit)? = null
    var onError: ((Int) -> Unit)? = null
    var onRms: ((Float) -> Unit)? = null

    init {
        speech?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() { reintentos = 0 }
            override fun onRmsChanged(rmsdB: Float) {
                if (rmsdB > rmsMax) rmsMax = rmsdB
                onRms?.invoke(rmsdB)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                handler.removeCallbacks(timeoutRunnable)
                activo = false
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
        rmsMax = 0f
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, idioma)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, idioma)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
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
