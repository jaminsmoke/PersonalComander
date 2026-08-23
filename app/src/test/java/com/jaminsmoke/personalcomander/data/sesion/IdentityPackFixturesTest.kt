package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonParser
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Valida los parsers de Commander contra el **pack de operaciones** de Identity
 * (`docs/contracts/camareros.ops.json`, publicado en main de PersonalHostel-Server).
 *
 * El pack es la fuente del contrato camareros: cada operación que Commander
 * consume debe decodificarla con el parser real de la app. Si Identity cambia
 * un payload (campos, tipos, nulabilidad) y el parser deja de decodificarlo,
 * este test falla en CI.
 *
 * Best-effort: el pack aún no cubre todas las operaciones (item espejo en el
 * Project 10 de Identity). Solo falla por una op que ESTÁ con `response_200`;
 * la cobertura completa (Commander ⊆ pack) se valida cuando el pack se complete
 * (asumeTrue → test «skipped» hoy, duro cuando llegue).
 *
 * Localización (mismo patrón que [BarContractFixturesTest]):
 * - sibling local: `../PersonalHostelIdentity-Server/docs/contracts` (cwd raíz)
 *   o `../../PersonalHostelIdentity-Server/docs/contracts` (cwd `app/`)
 * - checkout de CI: `.family/identity/docs/contracts`
 */
class IdentityPackFixturesTest {

    private fun pack(): Map<String, Any> {
        val fichero = File(packDir(), "camareros.ops.json")
        if (!fichero.isFile) {
            error("Pack no encontrado: $fichero (cwd=${File(".").canonicalPath})")
        }
        return com.google.gson.Gson().fromJson(
            JsonParser.parseString(fichero.readText()).asJsonObject,
            Map::class.java,
        ) as Map<String, Any>
    }

    private fun packDir(): File {
        val candidatos = listOf(
            File("../PersonalHostelIdentity-Server/docs/contracts"),
            File("../../PersonalHostelIdentity-Server/docs/contracts"),
            File("../.family/identity/docs/contracts"),
            File(".family/identity/docs/contracts"),
        )
        return candidatos.firstOrNull { it.isDirectory }
            ?: error(
                "No se encuentran docs/contracts de Identity. Clona PersonalHostel-Server " +
                    "como hermano (../PersonalHostelIdentity-Server) o usa el checkout de CI.",
            )
    }

    @Test
    fun loginResponse_lo_parsea_IdentityJson() {
        val op = operacion("POST /v1/auth/login")
        val sesion = IdentityJson.parseLoginOrNull(responseDe(op))
        assertNotNull("IdentityJson no decodifica el login del pack", sesion)
    }

    @Test
    fun refreshResponse_lo_parsea_IdentityJson() {
        val op = operacion("POST /v1/auth/refresh")
        val renovada = IdentityJson.parseRefreshOrNull(responseDe(op))
        assertNotNull("IdentityJson no decodifica el refresh del pack", renovada)
    }

    @Test
    fun me_perfil_lo_parsea_identityJson() {
        val op = operacion("GET /v1/camareros/me")
        val perfil = IdentityJson.parsePerfil(JsonParser.parseString(responseDe(op)))
        assertNotNull("IdentityJson no decodifica el perfil del pack", perfil.id)
    }

    @Test
    fun qr_lo_parsea_identityJson() {
        val op = operacion("GET /v1/camareros/me/qr")
        val qr = IdentityJson.parseQr(responseDe(op))
        assertNotNull("IdentityJson no decodifica el QR del pack", qr.qr)
    }

    @Test
    fun error_lo_parsea_identityJson() {
        val op = operacion("POST /v1/auth/login")
        val err = (op as Map<*, *>)["error"] ?: return
        val gson = com.google.gson.Gson()
        val parseado = IdentityJson.parseError(gson.toJson(err))
        assertTrue("IdentityJson no codifica el error", parseado.detail.isNotBlank())
    }

    @Test
    fun cobertura_total_del_pack_cuando_se_complete() {
        // El pack no cubre aún todas las operaciones; hasta que el ítem espejo de
        // Identity las añada, este test queda «skipped» (no rojo). Cuando esté
        // completo valida de verdad que no sobra nada del lado Commander.
        val declaradas = IdentityCliente.Rutas.todas().toSet()
        val enPack = (pack() as Map<String, Any>).keys
            .mapNotNull { op -> op.split(" ", limit = 2).getOrNull(1) }
            .toSet()
        val faltan = declaradas - enPack
        assumeTrue(
            "El pack de Identity aún no cubre: $faltan — se difiere hasta ítem espejo",
            faltan.isEmpty(),
        )
        // Si cubre todo, ninguna ruta del pack debe ser de negocio (:8082) ni interna.
        val noPermitidas = enPack.filter { it.startsWith("/internal") || it.startsWith("/v1/auth/negocio") }
        assertTrue("El pack trae rutas fuera de oficio: $noPermitidas", noPermitidas.isEmpty())
    }

    private fun operacion(clave: String): Map<*, *> {
        val op = (pack() as Map<String, Any>)[clave]
            ?: error("No está la operación $clave en el pack de Identity")
        return op as Map<*, *>
    }

    private fun responseDe(op: Map<*, *>): String {
        val resp = op["response_200"]
            ?: error("El pack no trae response_200 para ${op["operation"]}")
        return JsonParser.parseString((resp as Map<*, *>).toString()).toString()
    }
}