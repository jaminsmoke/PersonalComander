package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Test

class RedLocalTest {

    @Test
    fun loopback_127_0_0_1_aceptado() {
        RedLocal.requerirHostLocal("127.0.0.1")
    }

    @Test
    fun loopback_localhost_aceptado() {
        RedLocal.requerirHostLocal("localhost")
    }

    @Test
    fun siteLocal_10_x_aceptado() {
        RedLocal.requerirHostLocal("10.0.0.1")
        RedLocal.requerirHostLocal("10.255.255.254")
    }

    @Test
    fun siteLocal_172_16_x_aceptado() {
        RedLocal.requerirHostLocal("172.16.0.1")
        RedLocal.requerirHostLocal("172.31.255.254")
    }

    @Test
    fun siteLocal_192_168_x_aceptado() {
        RedLocal.requerirHostLocal("192.168.0.1")
        RedLocal.requerirHostLocal("192.168.1.37")
        RedLocal.requerirHostLocal("192.168.255.254")
    }

    @Test
    fun siteLocal_10_0_2_2_emulador_aceptado() {
        RedLocal.requerirHostLocal("10.0.2.2")
    }

    @Test(expected = SecurityException::class)
    fun publica_8_8_8_8_rechazada() {
        RedLocal.requerirHostLocal("8.8.8.8")
    }

    @Test(expected = SecurityException::class)
    fun publica_1_1_1_1_rechazada() {
        RedLocal.requerirHostLocal("1.1.1.1")
    }

    @Test(expected = SecurityException::class)
    fun publica_google_com_rechazada() {
        // google.com resuelve a IP pública → isSiteLocalAddress = false
        RedLocal.requerirHostLocal("google.com")
    }
}