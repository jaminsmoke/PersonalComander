package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertEquals
import org.junit.Test

class EstadoLaboralTest {

    private fun m(nombre: String, id: String = "id") =
        MembresiaEstablecimiento(id, nombre, "cuenta", "staff")

    @Test
    fun vacio_es_libre() {
        assertEquals(EstadoLaboral.Libre, estadoLaboral(emptyList()))
    }

    @Test
    fun nombres_en_blanco_es_libre() {
        assertEquals(EstadoLaboral.Libre, estadoLaboral(listOf(m("  "), m(""))))
    }

    @Test
    fun uno() {
        assertEquals(
            EstadoLaboral.Trabajador(listOf("Casa Pepe")),
            estadoLaboral(listOf(m("Casa Pepe"))),
        )
    }

    @Test
    fun varios_conservan_orden_y_recortan() {
        assertEquals(
            EstadoLaboral.Trabajador(listOf("Casa Pepe", "Bar Lola")),
            estadoLaboral(listOf(m(" Casa Pepe "), m("Bar Lola"), m(""))),
        )
    }
}
