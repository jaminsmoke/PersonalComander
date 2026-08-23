package com.jaminsmoke.personalcomander.data.sesion

import java.net.InetAddress

/**
 * Valida que un host pertenece a la red local (RFC 1918) o es loopback.
 * El guarda de código complementa al Network Security Config, que no puede
 * expresar rangos CIDR — solo dominios exactos.
 *
 * Sin dependencias de Android; funciona en JVM para tests unitarios.
 */
object RedLocal {

    /**
     * Lanza [SecurityException] si [host] no es una IP privada (site-local)
     * ni loopback. No hace DNS si [host] es una IP literal.
     */
    fun requerirHostLocal(host: String) {
        val addr = InetAddress.getByName(host)
        if (addr.isSiteLocalAddress || addr.isLoopbackAddress) return
        throw SecurityException(
            "Conexión cleartext rechazada: $host no es un host local (RFC 1918 / loopback)"
        )
    }
}