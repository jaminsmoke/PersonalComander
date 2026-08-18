package com.jaminsmoke.personalcomander.data.sesion

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** Ventana del panel de oficio en Resumen. */
enum class OficioVentana {
    DIA,
    SEMANA,
    MES,
}

data class OficioBounds(
    val desde: Instant,
    val hasta: Instant,
)

data class ResumenOficio(
    val desde: Instant,
    val hasta: Instant,
    val horasSegundos: Int,
    val rondasServidas: Int,
    val porEstablecimiento: List<ResumenOficioEstablecimiento> = emptyList(),
)

data class ResumenOficioEstablecimiento(
    val establecimientoId: String,
    val horasSegundos: Int,
    val rondasServidas: Int,
)

data class JornadaOficio(
    val id: String,
    val camareroId: String,
    val establecimientoId: String,
    val inicio: Instant,
    val fin: Instant?,
)

data class HorasDiaPunto(
    val fecha: LocalDate,
    val segundos: Int,
)

/**
 * Límites de la ventana en zona local, recortados a [ahora].
 * Día = medianoche de hoy; semana = lunes ISO; mes = día 1.
 */
fun OficioVentana.limites(ahora: ZonedDateTime): OficioBounds {
    val zona = ahora.zone
    val hoy = ahora.toLocalDate()
    val inicioLocal = when (this) {
        OficioVentana.DIA -> hoy.atStartOfDay(zona)
        OficioVentana.SEMANA -> hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zona)
        OficioVentana.MES -> hoy.withDayOfMonth(1).atStartOfDay(zona)
    }
    return OficioBounds(inicioLocal.toInstant(), ahora.toInstant())
}

/** Formato de [horasSegundos] para UI: `2 h 15 min`, `45 min`, `0 min`. */
fun formatoHorasOficio(horasSegundos: Int): String {
    val total = horasSegundos.coerceAtLeast(0)
    val horas = total / 3600
    val minutos = (total % 3600) / 60
    return when {
        horas > 0 && minutos > 0 -> "$horas h $minutos min"
        horas > 0 -> "$horas h"
        else -> "$minutos min"
    }
}

/**
 * Segundos de jornada recortados a [desde]..[hasta], agrupados por día local.
 * Deriva de intervalos reales; no inventa horas.
 */
fun horasPorDia(
    jornadas: List<JornadaOficio>,
    desde: Instant,
    hasta: Instant,
    zona: ZoneId,
): List<HorasDiaPunto> {
    if (!hasta.isAfter(desde)) return emptyList()
    val inicioCal = desde.atZone(zona).toLocalDate()
    val finCal = hasta.atZone(zona).toLocalDate()
    val segundos = linkedMapOf<LocalDate, Int>()
    var dia = inicioCal
    while (!dia.isAfter(finCal)) {
        segundos[dia] = 0
        dia = dia.plusDays(1)
    }
    for (jornada in jornadas) {
        val ini = maxInstant(jornada.inicio, desde)
        val fin = minInstant(jornada.fin ?: hasta, hasta)
        if (!fin.isAfter(ini)) continue
        var cursor = ini
        while (cursor.isBefore(fin)) {
            val fecha = cursor.atZone(zona).toLocalDate()
            val finDia = fecha.plusDays(1).atStartOfDay(zona).toInstant()
            val corte = minInstant(fin, finDia)
            val extra = Duration.between(cursor, corte).seconds.toInt().coerceAtLeast(0)
            segundos[fecha] = (segundos[fecha] ?: 0) + extra
            cursor = corte
        }
    }
    return segundos.map { HorasDiaPunto(it.key, it.value) }
}

private fun maxInstant(a: Instant, b: Instant): Instant = if (a.isAfter(b)) a else b

private fun minInstant(a: Instant, b: Instant): Instant = if (a.isBefore(b)) a else b
