package com.jaminsmoke.personalcomander.data.sesion

import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaForma
import com.jaminsmoke.personalcomander.data.Sala

/** Sala del contrato Bar `GET /v1/estado`. [id] es el slug de red (`sala-terraza`). */
data class SalaLan(
    val id: String = "",
    val nombre: String = "",
    val orden: Int = 0,
)

/**
 * Mesa del contrato Bar `GET /v1/estado`. [id] es el slug de red (`mesa-1`);
 * [salaId] apunta al [SalaLan.id] del nodo, no al Long local.
 */
data class MesaLan(
    val id: String = "",
    val salaId: String = "",
    val indiceZona: Int = 0,
    val numero: Int = 0,
    val alias: String? = null,
    val forma: String = "CUADRADA",
    val capacidad: Int = 4,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val girada: Boolean = false,
    val bloqueada: Boolean = false,
)

data class PlanSalas(
    val insertar: List<Sala>,
    val actualizar: List<Sala>,
)

data class PlanMesas(
    val insertar: List<Mesa>,
    val actualizar: List<Mesa>,
)

/**
 * Espejo del layout de Bar: plan de upsert por [Sala.codigoBar] / [Mesa.codigoBar].
 * No borra salas ni mesas locales sin código. Conserva estado de comanda al actualizar.
 */
object MapaSync {

    fun formaDe(remoto: String): MesaForma =
        MesaForma.entries.find { it.name.equals(remoto, ignoreCase = true) } ?: MesaForma.CUADRADA

    fun planSalas(locales: List<Sala>, remotas: List<SalaLan>): PlanSalas {
        val porCodigo = locales.mapNotNull { s -> s.codigoBar?.let { it to s } }.toMap()
        val insertar = mutableListOf<Sala>()
        val actualizar = mutableListOf<Sala>()
        for (remoto in remotas.distinctBy { it.id }) {
            if (remoto.id.isBlank() || remoto.nombre.isBlank()) continue
            val local = porCodigo[remoto.id]
            if (local == null) {
                insertar.add(
                    Sala(nombre = remoto.nombre, orden = remoto.orden, codigoBar = remoto.id),
                )
            } else {
                actualizar.add(local.copy(nombre = remoto.nombre, orden = remoto.orden))
            }
        }
        return PlanSalas(insertar, actualizar)
    }

    fun planMesas(
        locales: List<Mesa>,
        remotas: List<MesaLan>,
        salasPorCodigoBar: Map<String, Long>,
    ): PlanMesas {
        val porCodigo = locales.mapNotNull { m -> m.codigoBar?.let { it to m } }.toMap()
        val insertar = mutableListOf<Mesa>()
        val actualizar = mutableListOf<Mesa>()
        for (remoto in remotas.distinctBy { it.id }) {
            if (remoto.id.isBlank()) continue
            val salaIdLocal = salasPorCodigoBar[remoto.salaId] ?: continue
            val local = porCodigo[remoto.id]
            if (local == null) {
                insertar.add(
                    Mesa(
                        numero = remoto.numero,
                        alias = remoto.alias,
                        forma = formaDe(remoto.forma),
                        salaId = salaIdLocal,
                        capacidad = remoto.capacidad,
                        posX = remoto.posX,
                        posY = remoto.posY,
                        girada = remoto.girada,
                        indiceZona = remoto.indiceZona,
                        bloqueada = remoto.bloqueada,
                        codigoBar = remoto.id,
                    ),
                )
            } else {
                actualizar.add(
                    local.copy(
                        numero = remoto.numero,
                        alias = remoto.alias,
                        forma = formaDe(remoto.forma),
                        salaId = salaIdLocal,
                        capacidad = remoto.capacidad,
                        posX = remoto.posX,
                        posY = remoto.posY,
                        girada = remoto.girada,
                        indiceZona = remoto.indiceZona,
                        bloqueada = remoto.bloqueada,
                    ),
                )
            }
        }
        return PlanMesas(insertar, actualizar)
    }
}
