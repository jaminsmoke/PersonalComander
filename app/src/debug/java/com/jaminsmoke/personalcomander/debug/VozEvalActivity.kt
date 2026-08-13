package com.jaminsmoke.personalcomander.debug

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jaminsmoke.personalcomander.ui.VozRecognizer
import java.io.File

/**
 * Solo debug: inyecta PCM en [VozRecognizer] via EXTRA_AUDIO_SOURCE (API 33+).
 * Job en getExternalFilesDir()/voz-eval/job.json
 *
 * adb shell am start -n com.jaminsmoke.personalcomander/.debug.VozEvalActivity
 */
class VozEvalActivity : ComponentActivity() {

    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: VozRecognizer? = null
    private var audioPfd: ParcelFileDescriptor? = null
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logView = TextView(this).apply {
            textSize = 14f
            setPadding(32, 48, 32, 32)
        }
        val scroll = ScrollView(this).apply { addView(logView) }
        val run = Button(this).apply { text = "Run job" }
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(run)
            addView(scroll, android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))
        }
        setContentView(root)
        dir().mkdirs()
        File(dir(), "heartbeat.txt").writeText("onCreate ${System.currentTimeMillis()}\n${dir().absolutePath}\n")
        Log.i(TAG, "voz-eval dir=${dir().absolutePath}")
        run.setOnClickListener { runJob() }
        if (intent?.getBooleanExtra("auto", false) == true) {
            handler.postDelayed({ runJob() }, 500)
        }
    }

    private fun dir(): File = File(getExternalFilesDir(null), "voz-eval").apply { mkdirs() }

    private fun log(msg: String) {
        logView.append(msg + "\n")
    }

    private fun runJob() {
        val jobFile = File(dir(), "job.json")
        if (!jobFile.isFile) {
            log("No hay job.json en ${dir().absolutePath}")
            return
        }
        val job = gson.fromJson(jobFile.readText(), JsonObject::class.java)
        val id = job.get("id")?.asString ?: "unknown"
        val wav = File(job.get("wav")?.asString ?: "")
        if (!wav.isFile) {
            log("WAV no encontrado: $wav")
            setResultCode("missing_wav", id, "")
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            log("API ${Build.VERSION.SDK_INT}: EXTRA_AUDIO_SOURCE requiere 33+")
            setResultCode("api_too_low", id, "")
            return
        }
        log("Inject $id ${wav.name}")
        recognizer?.destruir()
        closePfd()
        val rec = VozRecognizer(applicationContext)
        recognizer = rec
        var lastPartial = ""
        rec.onRms = { rms -> log("rms=$rms") }
        rec.onParcial = { texto ->
            lastPartial = texto
            log("parcial: $texto")
        }
        rec.onResultado = { texto ->
            log("STT: $texto rms=${rec.rmsPico} cercana=${rec.vozCercana}")
            appendHypothesis(id, texto, job, rec)
            rec.destruir()
            finishOk()
        }
        rec.onError = { err ->
            val texto = lastPartial
            log("STT error $err partial='$texto' rms=${rec.rmsPico} cercana=${rec.vozCercana}")
            appendHypothesis(id, texto, job, rec, error = err)
            rec.destruir()
            finishOk()
        }
        try {
            val parsed = readWavPcm16(wav)
            val pcm = withTrailingSilence(parsed.pcm, parsed.sampleRate, parsed.channels, 800)
            val pcmFile = File(cacheDir, "voz-eval-inject.pcm")
            pcmFile.writeBytes(pcm)
            audioPfd = ParcelFileDescriptor.open(pcmFile, ParcelFileDescriptor.MODE_READ_ONLY)
            log("PCM ${pcm.size} B rate=${parsed.sampleRate} ch=${parsed.channels}")
            rec.empezar(
                idioma = "es-ES",
                audioSource = audioPfd,
                audioSampleRateHz = parsed.sampleRate,
                audioChannelCount = parsed.channels,
            )
        } catch (e: Exception) {
            log("Inject error: ${e.message}")
            setResultCode("inject_error", id, "")
            finishOk()
        }
    }

    private fun appendHypothesis(
        id: String,
        texto: String,
        job: JsonObject,
        rec: VozRecognizer,
        error: Int? = null,
    ) {
        val out = File(dir(), "hypotheses.jsonl")
        val obj = JsonObject().apply {
            addProperty("id", id)
            addProperty("hypothesis", texto)
            addProperty("voice", job.get("voice")?.asString)
            addProperty("snr", job.get("snr")?.asString)
            addProperty("distance", job.get("distance")?.asString)
            addProperty("rmsMax", rec.rmsPico)
            addProperty("vozCercana", rec.vozCercana)
            addProperty("source", "speech_recognizer_inject")
            if (error != null) addProperty("sttError", error)
        }
        out.appendText(gson.toJson(obj) + "\n")
        out.setReadable(true, false)
        val last = File(dir(), "last.json")
        last.writeText(gson.toJson(obj))
        last.setReadable(true, false)
        last.setWritable(true, false)
    }

    private fun setResultCode(status: String, id: String, texto: String) {
        File(dir(), "last.json").writeText(
            """{"id":"$id","status":"$status","hypothesis":"$texto"}"""
        )
    }

    private fun closePfd() {
        runCatching { audioPfd?.close() }
        audioPfd = null
    }

    private fun finishOk() {
        closePfd()
        if (intent?.getBooleanExtra("auto", false) == true) {
            handler.postDelayed({ finish() }, 300)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        closePfd()
        recognizer?.destruir()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "VozEval"
    }
}
