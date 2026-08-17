package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityUrlTest {

    @Test
    fun vacio_usa_vps() {
        assertEquals(SesionStore.DEFAULT_IDENTITY_URL, SesionStore.urlIdentityEfectiva(null))
        assertEquals(SesionStore.DEFAULT_IDENTITY_URL, SesionStore.urlIdentityEfectiva("  "))
        assertEquals("https://camareros.siberia.solutions", SesionStore.DEFAULT_IDENTITY_URL)
    }

    @Test
    fun docker_local_se_depreca_hacia_vps() {
        assertTrue(SesionStore.esIdentityDockerLocal("http://10.0.2.2:8080"))
        assertTrue(SesionStore.esIdentityDockerLocal("http://10.0.2.2:8080/"))
        assertTrue(SesionStore.esIdentityDockerLocal("http://127.0.0.1:8080"))
        assertTrue(SesionStore.esIdentityDockerLocal("http://localhost:8080"))
        assertEquals(
            SesionStore.DEFAULT_IDENTITY_URL,
            SesionStore.urlIdentityEfectiva("http://10.0.2.2:8080"),
        )
    }

    @Test
    fun bar_lan_y_custom_no_se_tocan() {
        assertFalse(SesionStore.esIdentityDockerLocal("http://10.0.2.2:8787"))
        assertEquals(
            "https://camareros.siberia.solutions",
            SesionStore.urlIdentityEfectiva("https://camareros.siberia.solutions/"),
        )
        assertEquals(
            "https://identity.example",
            SesionStore.urlIdentityEfectiva("https://identity.example"),
        )
    }
}
