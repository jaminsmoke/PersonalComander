package com.jaminsmoke.personalcomander.debug

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class PcmWav(
    val pcm: ByteArray,
    val sampleRate: Int,
    val channels: Int,
)

/** Extrae PCM 16-bit little-endian de un WAV (sin cabecera RIFF). */
internal fun readWavPcm16(file: File): PcmWav {
    val bytes = file.readBytes()
    require(bytes.size > 44) { "WAV demasiado corto: ${file.name}" }
    fun ascii(at: Int, n: Int) = String(bytes, at, n, Charsets.US_ASCII)
    require(ascii(0, 4) == "RIFF" && ascii(8, 4) == "WAVE") {
        "No es WAV PCM: ${file.name}"
    }
    var pos = 12
    var rate = 16000
    var channels = 1
    var bits = 16
    var data: ByteArray? = null
    while (pos + 8 <= bytes.size) {
        val id = ascii(pos, 4)
        val size = ByteBuffer.wrap(bytes, pos + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val start = pos + 8
        val end = (start + size).coerceAtMost(bytes.size)
        when (id) {
            "fmt " -> {
                val fmt = ByteBuffer.wrap(bytes, start, (end - start).coerceAtLeast(16))
                    .order(ByteOrder.LITTLE_ENDIAN)
                val format = fmt.short.toInt() and 0xFFFF
                channels = fmt.short.toInt() and 0xFFFF
                rate = fmt.int
                fmt.int
                fmt.short
                bits = fmt.short.toInt() and 0xFFFF
                require(format == 1 && bits == 16) {
                    "Solo PCM 16-bit (format=$format bits=$bits)"
                }
            }
            "data" -> data = bytes.copyOfRange(start, end)
        }
        pos = start + size
        if (size % 2 == 1) pos++
    }
    return PcmWav(
        pcm = requireNotNull(data) { "WAV sin chunk data: ${file.name}" },
        sampleRate = rate,
        channels = channels,
    )
}

internal fun withTrailingSilence(pcm: ByteArray, sampleRate: Int, channels: Int, ms: Int): ByteArray {
    val extra = sampleRate * channels * 2 * ms / 1000
    return pcm + ByteArray(extra)
}
