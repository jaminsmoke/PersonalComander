package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Contrato de identidad del cliente de Identity: las rutas que Commander
 * consume se derivan de [IdentityCliente.Rutas] (ya no hay snapshot).
 * Este test fija las reglas de oficio: solo `/v1` públicas del servicio
 * camareros; nunca internas, nunca el servicio negocio (`:8082`).
 */
class IdentityContratoTest {

    @Test
    fun todas_las_rutas_son_v1_y_no_internas() {
        val rutas = IdentityCliente.Rutas.todas()
        assertTrue("IdentityCliente debe declarar rutas", rutas.isNotEmpty())
        for (r in rutas) {
            assertTrue("Ruta fuera de /v1: $r", r.startsWith("/v1/"))
            assertFalse("No debe llamar rutas internas: $r", r.startsWith("/internal"))
            assertFalse("No debe llamar rutas externas del server: $r", r.startsWith("http"))
        }
    }

    @Test
    fun fuentes_no_presentan_rutas_fuera_de_rutas() {
        // Cualquier literal "/v1/..." en la fuente debe estar declarado en Rutas.
        val declaradas = IdentityCliente.Rutas.todas().toSet()
        val literales = rutasRutasEnFuente()
        val extra = literales - declaradas
        assertTrue(
            "Rutas en IdentityCliente.kt no declaradas en Rutas.*: $extra",
            extra.isEmpty(),
        )
    }

    @Test
    fun no_llama_servicio_negocio() {
        val src = leerFuenteIdentityCliente()
        val citado8082 = Regex("\"[^\"]*8082[^\"]*\"").containsMatchIn(src)
        assertFalse("IdentityCliente no debe citar :8082 en literales", citado8082)
        assertFalse(src.contains("/v1/auth/negocio"))
        assertFalse(src.contains("/v1/establecimientos/"))
    }

    private fun rutasRutasEnFuente(): Set<String> {
        return Regex("\"(/v1/[^\"]+)\"")
            .findAll(leerFuenteIdentityCliente())
            .map { it.groupValues[1] }
            .toSet()
    }

    private fun leerFuenteIdentityCliente(): String {
        val rel = "java/com/jaminsmoke/personalcomander/data/sesion/IdentityCliente.kt"
        val candidatos = listOf(
            File("src/main/$rel"),
            File("app/src/main/$rel"),
        )
        return candidatos.firstOrNull { it.isFile }
            ?.readText()
            ?: error("No se encuentra IdentityCliente.kt (cwd=${File(".").canonicalPath})")
    }
}