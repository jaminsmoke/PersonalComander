package com.jaminsmoke.personalcomander.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MesaZonaTest {

    private fun mesa(zona: String, indice: Int, alias: String? = null) = Mesa(
        numero = 1, alias = alias, zona = zona, indiceZona = indice
    )

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
        assertEquals("B1", mesa("Barra", 1).idZona)
        assertEquals("B3", mesa("Barra", 3).idZona)
        assertEquals("T2", mesa("Terraza", 2).idZona)
        assertEquals("I5", mesa("Interior", 5).idZona)
    }

    // ── nombreVisible ──

    @Test
    fun nombreVisible_sin_alias_muestra_id_zona() {
        assertEquals("B1", mesa("Barra", 1).nombreVisible)
    }

    @Test
    fun nombreVisible_con_alias_muestra_el_alias() {
        assertEquals("Mesa de la esquina", mesa("Barra", 1, alias = "Mesa de la esquina").nombreVisible)
    }

    // ── Seed ──

    @Test
    fun seed_asigna_indices_por_zona() {
        val mesas = Seed.mesas()
        val terraza = mesas.filter { it.zona == "Terraza" }
        val barra = mesas.filter { it.zona == "Barra" }
        val interior = mesas.filter { it.zona == "Interior" }

        assertEquals(listOf(1, 2, 3, 4), terraza.map { it.indiceZona })
        assertEquals(listOf(1, 2), barra.map { it.indiceZona })
        assertEquals((1..10).toList(), interior.map { it.indiceZona })
        assertEquals(listOf("T1", "T2", "T3", "T4"), terraza.map { it.idZona })
        assertEquals(listOf("B1", "B2"), barra.map { it.idZona })
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
