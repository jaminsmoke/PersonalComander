package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IdentityContratoTest {

    @Test
    fun rutas_cliente_coinciden_con_snapshot() {
        assertEquals(leerSnapshot(), IdentityCliente.Rutas.todas().toSet())
    }

    @Test
    fun fuente_identity_cliente_no_introduce_rutas_fuera_del_snapshot() {
        val extra = rutasEnFuente() - leerSnapshot()
        assertTrue("Rutas no listadas en docs/identity-contract-paths.txt: $extra", extra.isEmpty())
    }

    @Test
    fun no_llama_servicio_negocio() {
        val src = leerFuenteIdentityCliente()
        val citado8082 = Regex("\"[^\"]*8082[^\"]*\"").containsMatchIn(src)
        assertFalse("IdentityCliente no debe citar :8082 en literales", citado8082)
        assertFalse(src.contains("/v1/auth/negocio"))
        assertFalse(src.contains("/v1/establecimientos/"))
    }

    private fun leerSnapshot(): Set<String> {
        val lineas = snapshotFile().readLines()
            .map { it.trim() }
            .filter { it.startsWith("/") }
        return lineas.toSet()
    }

    private fun rutasEnFuente(): Set<String> {
        return Regex("\"(/v1/[^\"]+)\"")
            .findAll(leerFuenteIdentityCliente())
            .map { it.groupValues[1] }
            .toSet()
    }

    private fun leerFuenteIdentityCliente(): String = fuenteIdentityCliente().readText()

    private fun snapshotFile(): File {
        val candidatos = listOf(
            File("docs/identity-contract-paths.txt"),
            File("../docs/identity-contract-paths.txt"),
        )
        return candidatos.firstOrNull { it.isFile }
            ?: error("No se encuentra docs/identity-contract-paths.txt (cwd=${File(".").canonicalPath})")
    }

    private fun fuenteIdentityCliente(): File {
        val rel = "java/com/jaminsmoke/personalcomander/data/sesion/IdentityCliente.kt"
        val candidatos = listOf(
            File("src/main/$rel"),
            File("app/src/main/$rel"),
        )
        return candidatos.firstOrNull { it.isFile }
            ?: error("No se encuentra IdentityCliente.kt (cwd=${File(".").canonicalPath})")
    }
}
