package com.jaminsmoke.personalcomander.data.sesion

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

/**
 * Escucha el beacon UDP de Bar. El host interno es la IP origen del datagrama;
 * no se enseña. En dos AVD el datagrama no llega: el probe 10.0.2.2 cubre el lab.
 */
object PresenciaLanOyente {
    suspend fun escuchar(
        context: Context,
        onAnuncio: (host: String, anuncio: PresenciaLan.Anuncio) -> Unit,
    ) {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wm.createMulticastLock("com.jaminsmoke.personalcomander:presencia")
        lock.setReferenceCounted(false)
        try {
            lock.acquire()
            DatagramSocket(null).use { socket ->
                socket.reuseAddress = true
                socket.broadcast = true
                socket.soTimeout = 500
                socket.bind(InetSocketAddress(PresenciaLan.PUERTO))
                val buf = ByteArray(1024)
                while (currentCoroutineContext().isActive) {
                    try {
                        val packet = DatagramPacket(buf, buf.size)
                        socket.receive(packet)
                        val texto = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                        val anuncio = PresenciaLan.decode(texto) ?: continue
                        val host = packet.address?.hostAddress?.trim().orEmpty()
                        if (host.isEmpty() || host.contains(':')) continue
                        onAnuncio(host, anuncio)
                    } catch (_: java.net.SocketTimeoutException) {
                        // para poder cancelar
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Oyente de presencia: $e")
        } finally {
            if (lock.isHeld) lock.release()
        }
    }

    private const val TAG = "PresenciaLanOyente"
}
