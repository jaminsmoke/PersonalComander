package com.jaminsmoke.personalcomander.data.sesion

import com.jaminsmoke.personalcomander.data.ServidorDescubierto

/** Cómo se pinta un Bar visto en la LAN. El host no se enseña. */
enum class LanLocalAspecto {
    /** En la red, Bar no te tiene de camarero. Sin gesto. */
    APAGADO,
    /** En la red y admitido; sin jornada. */
    AMARILLO,
    /** Jornada en curso. */
    VERDE,
    /** Fallo de red, health o nodo caído. */
    ROJO,
}

data class LanLocalUi(
    val host: String,
    val puerto: Int,
    val nombre: String,
    val aspecto: LanLocalAspecto,
)

fun aspectoLan(errorConexion: Boolean, admitido: Boolean, jornada: Boolean): LanLocalAspecto = when {
    errorConexion -> LanLocalAspecto.ROJO
    jornada -> LanLocalAspecto.VERDE
    admitido -> LanLocalAspecto.AMARILLO
    else -> LanLocalAspecto.APAGADO
}

/**
 * Extra / beacon / scan: si el health falla, no se inventa un local.
 * El rojo es solo para un Bar que ya habíamos confirmado y ahora no responde.
 */
fun aspectoSondeo(
    conocido: Boolean,
    errorConexion: Boolean,
    admitido: Boolean,
    jornada: Boolean,
): LanLocalAspecto? {
    if (errorConexion) return if (conocido) LanLocalAspecto.ROJO else null
    return aspectoLan(false, admitido, jornada)
}

/** Nombre de sala para UI. Vacío si el health no trae establecimiento; nunca una IP. */
fun nombreLanVisible(healthNombre: String?): String =
    healthNombre?.trim().orEmpty()

fun mismosNodo(host: String, puerto: Int, modo: ModoSesion.Establecimiento): Boolean =
    modo.barHost == host && modo.barPuerto == puerto

/**
 * Candidatos a sondear: el scan más [extras] (emulador `10.0.2.2` y hosts de beacon).
 * El extra no se pinta; solo se usa como host interno.
 */
fun candidatosLan(
    descubiertos: List<ServidorDescubierto>,
    extras: List<ServidorDescubierto> = listOf(ServidorDescubierto(EMULADOR_BAR_HOST, BarLanCliente.PUERTO)),
): List<ServidorDescubierto> {
    val visto = HashSet<String>()
    val out = ArrayList<ServidorDescubierto>()
    for (s in descubiertos + extras) {
        val clave = "${s.ip}:${s.puerto}"
        if (visto.add(clave)) out.add(s)
    }
    return out
}

const val EMULADOR_BAR_HOST = "10.0.2.2"
