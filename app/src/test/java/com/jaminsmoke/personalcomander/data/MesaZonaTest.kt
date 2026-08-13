package com.jaminsmoke.personalcomander.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MesaZonaTest {

    private fun mesa(nombreSala: String, indice: Int, alias: String? = null) =
        Mesa(numero = 1, alias = alias, salaId = 1, indiceZona = indice) to nombreSala

    // ── zonaPrefijo ──

    @Test
    fun zonaPrefijo_barra_es_B() {
        assertEquals("B", zonaPrefijo("Barra"))
        assertEquals("B", zonaPrefijo("Bar"))
    }

    @Test
    fun zonaPrefijo_terraza_es_T() {
        assertEquals("T", zonaPrefijo("Terraza"))
    }

    @Test
    fun zonaPrefijo_interior_es_I() {
        assertEquals("I", zonaPrefijo("Interior"))
        assertEquals("I", zonaPrefijo("Salón"))
        assertEquals("I", zonaPrefijo("Salon"))
    }

    @Test
    fun zonaPrefijo_vip_es_V() {
        assertEquals("V", zonaPrefijo("VIP"))
        assertEquals("V", zonaPrefijo("Reservado"))
    }

    @Test
    fun zonaPrefijo_vacia_es_M() {
        assertEquals("M", zonaPrefijo(""))
    }

    @Test
    fun zonaPrefijo_desconocida_usa_primera_letra() {
        assertEquals("J", zonaPrefijo("Jardín"))
        assertEquals("P", zonaPrefijo("Patio"))
    }

    // ── idZona ──

    @Test
    fun idZona_combina_prefijo_e_indice() {
        assertEquals("B1", mesa("Barra", 1).let { (m, n) -> m.idZona(n) })
        assertEquals("B3", mesa("Barra", 3).let { (m, n) -> m.idZona(n) })
        assertEquals("T2", mesa("Terraza", 2).let { (m, n) -> m.idZona(n) })
        assertEquals("I5", mesa("Interior", 5).let { (m, n) -> m.idZona(n) })
    }

    // ── nombreVisible ──

    @Test
    fun nombreVisible_sin_alias_muestra_id_zona() {
        assertEquals("B1", mesa("Barra", 1).let { (m, n) -> m.nombreVisible(n) })
    }

    @Test
    fun nombreVisible_con_alias_muestra_el_alias() {
        assertEquals(
            "Mesa de la esquina",
            mesa("Barra", 1, alias = "Mesa de la esquina").let { (m, n) -> m.nombreVisible(n) },
        )
    }

    // ── Seed ──

    @Test
    fun seed_asigna_indices_por_sala() {
        val salas = mapOf("Terraza" to 1L, "Interior" to 2L, "Barra" to 3L)
        val mesas = Seed.mesas(salas)
        val terraza = mesas.filter { it.salaId == 1L }
        val barra = mesas.filter { it.salaId == 3L }
        val interior = mesas.filter { it.salaId == 2L }

        assertEquals(listOf(1, 2, 3, 4), terraza.map { it.indiceZona })
        assertEquals(listOf(1, 2), barra.map { it.indiceZona })
        assertEquals((1..10).toList(), interior.map { it.indiceZona })
        assertEquals(listOf("T1", "T2", "T3", "T4"), terraza.map { it.idZona("Terraza") })
        assertEquals(listOf("B1", "B2"), barra.map { it.idZona("Barra") })
    }

    @Test
    fun seed_posiciones_alineadas_al_grid() {
        val mesas = Seed.mesas()
        for (m in mesas) {
            assertEquals(0f, m.posX % 40f, 0.001f)
            assertEquals(0f, m.posY % 40f, 0.001f)
        }
    }
}
