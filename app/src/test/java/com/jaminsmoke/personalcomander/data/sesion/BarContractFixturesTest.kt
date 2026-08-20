package com.jaminsmoke.personalcomander.data.sesion

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Congela los parsers de Commander contra las **fixtures doradas** del contrato
 * LAN de Bar (`docs/contrato/fixtures/`, publicadas en main de PersonalBar).
 * Si Bar cambia un payload y no actualiza el corpus, o si Commander deja de
 * decodificarlo, este test falla.
 *
 * Localización de fixtures (patrón de [BarContratoTest]):
 * - sibling local del repo: `../PersonalBar/docs/contrato/fixtures` (cwd raíz)
 *   o `../../PersonalBar/docs/contrato/fixtures` (cwd `app/`)
 * - checkout de CI: `.family/bar/docs/contrato/fixtures`
 */
class BarContractFixturesTest {

    private fun fixture(nombre: String): String {
        val dir = fixtureDir()
        val fichero = File(dir, nombre)
        if (!fichero.isFile) {
            error("Fixture no encontrada: $fichero (cwd=${File(".").canonicalPath})")
        }
        return fichero.readText()
    }

    private fun fixtureDir(): File {
        val candidatos = listOf(
            File("../PersonalBar/docs/contrato/fixtures"),
            File("../../PersonalBar/docs/contrato/fixtures"),
            File(".family/bar/docs/contrato/fixtures"),
            File("docs/contrato/fixtures"),
        )
        return candidatos.firstOrNull { it.isDirectory }
            ?: error(
                "No se encuentra docs/contrato/fixtures de Bar. " +
                    "Clona PersonalBar como hermano (../PersonalBar) o usa el checkout de CI."
            )
    }

    @Test
    fun healthFixtureSeParsea() {
        val health = BarLanCliente.parseHealth(fixture("health.json"))
        assertNotNull(health)
        assertEquals(true, health!!.ok)
        assertEquals("bar", health.role)
        assertEquals("La Terraza", health.establecimiento)
        assertEquals("0.1", health.version)
        assertEquals("11111111-1111-4111-8111-111111111111", health.establecimientoId)
    }

    @Test
    fun sesionFixtureSeParsea() {
        val sesion = BarLanCliente.parseSesion(fixture("sesion.json"))
        assertNotNull(sesion)
        assertEquals(true, sesion!!.admitido)
        assertEquals("11111111-1111-4111-8111-111111111111", sesion.camareroId)
        assertEquals("luciaTest", sesion.nombre)
    }

    @Test
    fun sesionIniciarFixtureSeParsea() {
        val activa = BarLanCliente.parseSesionActiva(fixture("sesion-iniciar.json"))
        assertEquals(true, activa)
    }

    @Test
    fun ticketsFixtureSeParsea() {
        val tickets = RecogerLogica.parseTickets(fixture("tickets.json"))
        assertEquals(2, tickets.size)
        val barra = tickets.first { it.destino == "BARRA" }
        assertEquals("r1-barra", barra.id)
        assertEquals("r1", barra.rondaId)
        assertEquals(1, barra.numeroCola)
        assertEquals("cana", barra.lineas.single().productoId)
        assertEquals(2, barra.lineas.single().cantidad)
        val cocina = tickets.first { it.destino == "COCINA" }
        assertEquals("r1-cocina", cocina.id)
    }

    @Test
    fun estadoFixtureSeParsea() {
        val estado = RecogerLogica.parseEstado(fixture("estado.json"))
        assertNotNull(estado)
        assertEquals(1, estado!!.bebida.size)
        assertEquals("PREPARADO", estado.bebida.single().estado)
        assertEquals("anaTest", estado.bebida.single().preparadoPor)
        assertEquals(1, estado.salas.size)
        assertEquals("Terraza", estado.salas.single().nombre)
        assertEquals(3, estado.mesas.single().indiceZona)
    }

    @Test
    fun cartaFixtureSeParseaConSchemaYModificadores() {
        val carta = CartaSync.parse(fixture("carta.json"))
        assertNotNull(carta)
        assertEquals(2, carta!!.schema)
        val producto = carta.productos.single()
        assertEquals("Caña", producto.nombre)
        assertEquals("Zero", producto.subfamilia)
        assertEquals(listOf("22222222-2222-4222-8222-222222222222"), producto.grupos)
        val grupo = carta.gruposModificador.single()
        assertEquals("Punto", grupo.nombre)
        assertTrue(grupo.obligatorio)
        assertEquals("Al punto", grupo.opciones.single().nombre)
        assertEquals("al punto", grupo.opciones.single().alias)
    }

    @Test
    fun salaEventFixtureSeParseaConHidratacion() {
        val evento = RecogerLogica.parseSalaEvent(fixture("sala-event-preparado.json"))
        assertNotNull(evento)
        assertEquals(RecogerLogica.TIPO_PREPARADO, evento!!.tipo)
        assertEquals("r1-barra", evento.ticketId)
        assertEquals("T3", evento.mesaId)
        // destino/numeroCola/rondaId vienen anidados en `ticket` y se hidratan a la raíz.
        assertEquals("BARRA", evento.destino)
        assertEquals(1, evento.numeroCola)
        assertEquals("r1", evento.rondaId)
        assertEquals("anaTest", evento.preparadoPor)
        assertEquals(PlantillaAviso.COMPLETO, RecogerLogica.plantillaAviso(evento))
    }

    @Test
    fun rondaFixtureCubiertaPorElSerializadorDeCommander() {
        // Lo que Commander envía en POST /v1/rondas debe cubrir los campos del contrato:
        // las claves de la fixture son un subconjunto de las que emite RondaLanMapper.
        val enviado = JsonParser.parseString(
            RondaLanMapper.toJson(
                RondaLan(
                    id = "p42-t1730000000000",
                    mesaId = "T3",
                    numero = 1,
                    camarero = "luciaTest",
                    creadoEn = 1_730_000_000_000,
                    lineas = listOf(
                        LineaRondaLan(
                            productoId = "cana",
                            nombreProducto = "Caña",
                            cantidad = 2,
                            modificadores = emptyList(),
                        ),
                        LineaRondaLan(
                            productoId = "croquetas",
                            nombreProducto = "Croquetas",
                            cantidad = 1,
                            nota = "sin cebolla",
                            modificadores = listOf(
                                ModificadorRondaLan(grupo = "Punto", opcion = "Al punto", delta = 0.0),
                            ),
                        ),
                    ),
                )
            )
        ).asJsonObject

        val fixture = JsonParser.parseString(fixture("ronda.json")).asJsonObject
        assertTrue("Faltan claves del contrato en el serializador: $fixture", enviado.keySet().containsAll(fixture.keySet()))
        assertEquals("T3", fixture.get("mesaId").asString)
        val lineasFixture = fixture.getAsJsonArray("lineas")
        val lineasEnviadas = enviado.getAsJsonArray("lineas")
        assertEquals(lineasFixture.size(), lineasEnviadas.size())
        val conNota = lineasFixture.first { it.asJsonObject.has("nota") }.asJsonObject
        val enviadaConNota = lineasEnviadas.first { it.asJsonObject.has("nota") }.asJsonObject
        assertTrue(
            "Faltan claves de línea en el serializador: $enviadaConNota",
            enviadaConNota.keySet().containsAll(conNota.keySet()),
        )
        assertEquals("sin cebolla", enviadaConNota.get("nota").asString)
    }
}
