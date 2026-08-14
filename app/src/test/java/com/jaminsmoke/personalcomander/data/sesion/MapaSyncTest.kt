package com.jaminsmoke.personalcomander.data.sesion

import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaEstado
import com.jaminsmoke.personalcomander.data.MesaForma
import com.jaminsmoke.personalcomander.data.Sala
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapaSyncTest {

    @Test
    fun formaDe_desconocida_es_cuadrada() {
        assertEquals(MesaForma.REDONDA, MapaSync.formaDe("REDONDA"))
        assertEquals(MesaForma.RECTANGULAR_XL, MapaSync.formaDe("rectangular_xl"))
        assertEquals(MesaForma.CUADRADA, MapaSync.formaDe("ovalo"))
        assertEquals(MesaForma.CUADRADA, MapaSync.formaDe(""))
    }

    @Test
    fun parseEstado_incluye_salas_y_mesas() {
        val estado = RecogerLogica.parseEstado(
            """
            {
              "bebida":[{"id":"a","estado":"PREPARADO"}],
              "comida":[],
              "servidos":[],
              "salas":[
                {"id":"sala-terraza","nombre":"Terraza","orden":2},
                {"id":"","nombre":"Vacía"},
                {"id":"sala-x","nombre":""}
              ],
              "mesas":[
                {"id":"mesa-1","salaId":"sala-terraza","indiceZona":1,"numero":3,"forma":"REDONDA","capacidad":2,"posX":10.5,"posY":20,"girada":true,"bloqueada":false,"reservaActivaId":"ignorar"},
                {"id":"","salaId":"sala-terraza"}
              ]
            }
            """.trimIndent(),
        )!!
        assertEquals(1, RecogerLogica.ticketsDeColas(estado).size)
        assertEquals(listOf("sala-terraza"), estado.salas.map { it.id })
        assertEquals(1, estado.mesas.size)
        assertEquals("mesa-1", estado.mesas[0].id)
        assertEquals("REDONDA", estado.mesas[0].forma)
        assertEquals(10.5f, estado.mesas[0].posX, 0.01f)
    }

    @Test
    fun parseEstado_sin_mapa_deja_listas_vacias() {
        val estado = RecogerLogica.parseEstado(
            """{"bebida":[],"comida":[],"servidos":[]}""",
        )!!
        assertTrue(estado.salas.isEmpty())
        assertTrue(estado.mesas.isEmpty())
    }

    @Test
    fun planSalas_inserta_nuevas_y_no_borra_locales() {
        val locales = listOf(Sala(id = 1, nombre = "Seed local", orden = 0))
        val remotas = listOf(
            SalaLan(id = "sala-barra", nombre = "Barra", orden = 0),
            SalaLan(id = "sala-terraza", nombre = "Terraza", orden = 2),
        )
        val plan = MapaSync.planSalas(locales, remotas)
        assertEquals(2, plan.insertar.size)
        assertTrue(plan.actualizar.isEmpty())
        assertEquals("sala-barra", plan.insertar[0].codigoBar)
        assertEquals("Barra", plan.insertar[0].nombre)
        assertEquals("Seed local", locales[0].nombre)
    }

    @Test
    fun planSalas_actualiza_por_codigoBar() {
        val locales = listOf(
            Sala(id = 9, nombre = "Vieja", orden = 0, codigoBar = "sala-barra"),
            Sala(id = 1, nombre = "Seed local", orden = 1),
        )
        val remotas = listOf(SalaLan(id = "sala-barra", nombre = "Barra", orden = 3))
        val plan = MapaSync.planSalas(locales, remotas)
        assertTrue(plan.insertar.isEmpty())
        assertEquals(1, plan.actualizar.size)
        assertEquals(9L, plan.actualizar[0].id)
        assertEquals("Barra", plan.actualizar[0].nombre)
        assertEquals(3, plan.actualizar[0].orden)
        assertEquals("sala-barra", plan.actualizar[0].codigoBar)
    }

    @Test
    fun planMesas_inserta_con_salaId_local() {
        val salas = mapOf("sala-terraza" to 7L)
        val remotas = listOf(
            MesaLan(
                id = "mesa-1",
                salaId = "sala-terraza",
                indiceZona = 1,
                numero = 3,
                forma = "REDONDA",
                capacidad = 2,
                posX = 40f,
                posY = 80f,
            ),
        )
        val plan = MapaSync.planMesas(emptyList(), remotas, salas)
        assertEquals(1, plan.insertar.size)
        assertTrue(plan.actualizar.isEmpty())
        val mesa = plan.insertar[0]
        assertEquals(7L, mesa.salaId)
        assertEquals("mesa-1", mesa.codigoBar)
        assertEquals(MesaForma.REDONDA, mesa.forma)
        assertEquals(MesaEstado.LIBRE, mesa.estado)
        assertEquals(null, mesa.comandaActivaId)
    }

    @Test
    fun planMesas_salta_si_sala_remota_no_esta_en_el_mapa() {
        val remotas = listOf(MesaLan(id = "mesa-1", salaId = "sala-fantasma"))
        val plan = MapaSync.planMesas(emptyList(), remotas, emptyMap())
        assertTrue(plan.insertar.isEmpty())
        assertTrue(plan.actualizar.isEmpty())
    }

    @Test
    fun planMesas_actualiza_layout_conservando_comanda() {
        val local = Mesa(
            id = 42,
            numero = 1,
            alias = "Vieja",
            forma = MesaForma.CUADRADA,
            salaId = 1,
            capacidad = 4,
            estado = MesaEstado.OCUPADA,
            comandaActivaId = 99,
            reservaActivaId = 5,
            codigoBar = "mesa-1",
        )
        val remotas = listOf(
            MesaLan(
                id = "mesa-1",
                salaId = "sala-terraza",
                indiceZona = 4,
                numero = 8,
                alias = "Ventana",
                forma = "RECTANGULAR",
                capacidad = 8,
                posX = 100f,
                posY = 200f,
                girada = true,
                bloqueada = true,
            ),
        )
        val plan = MapaSync.planMesas(listOf(local), remotas, mapOf("sala-terraza" to 3L))
        assertTrue(plan.insertar.isEmpty())
        assertEquals(1, plan.actualizar.size)
        val mesa = plan.actualizar[0]
        assertEquals(42L, mesa.id)
        assertEquals(MesaEstado.OCUPADA, mesa.estado)
        assertEquals(99L, mesa.comandaActivaId)
        assertEquals(5L, mesa.reservaActivaId)
        assertEquals("mesa-1", mesa.codigoBar)
        assertEquals(3L, mesa.salaId)
        assertEquals("Ventana", mesa.alias)
        assertEquals(MesaForma.RECTANGULAR, mesa.forma)
        assertEquals(8, mesa.numero)
        assertEquals(4, mesa.indiceZona)
        assertEquals(100f, mesa.posX)
        assertTrue(mesa.girada)
        assertTrue(mesa.bloqueada)
    }
}
