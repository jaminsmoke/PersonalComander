package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BarContratoTest {

    @Test
    fun rutas_cliente_coinciden_con_snapshot() {
        assertEquals(leerSnapshot(), BarLanCliente.Rutas.todas().toSet())
    }

    @Test
    fun fuente_bar_cliente_no_introduce_rutas_fuera_del_snapshot() {
        val extra = rutasEnFuente() - leerSnapshot()
        assertTrue("Rutas no listadas en docs/bar-contract-paths.txt: $extra", extra.isEmpty())
    }

    private fun leerSnapshot(): Set<String> {
        return snapshotFile().readLines()
            .map { it.trim() }
            .filter { it.startsWith("/") }
            .toSet()
    }

    private fun rutasEnFuente(): Set<String> {
        return Regex("\"(/(?:health|v1)[^\"]*)\"")
            .findAll(leerFuente())
            .map { it.groupValues[1] }
            .toSet()
    }

    private fun leerFuente(): String = fuenteBarLanCliente().readText()

    private fun snapshotFile(): File {
        val candidatos = listOf(
            File("docs/bar-contract-paths.txt"),
            File("../docs/bar-contract-paths.txt"),
        )
        return candidatos.firstOrNull { it.isFile }
            ?: error("No se encuentra docs/bar-contract-paths.txt (cwd=${File(".").canonicalPath})")
    }

    private fun fuenteBarLanCliente(): File {
        val rel = "java/com/jaminsmoke/personalcomander/data/sesion/BarLanCliente.kt"
        val candidatos = listOf(
            File("src/main/$rel"),
            File("app/src/main/$rel"),
        )
        return candidatos.firstOrNull { it.isFile }
            ?: error("No se encuentra BarLanCliente.kt (cwd=${File(".").canonicalPath})")
    }
}
